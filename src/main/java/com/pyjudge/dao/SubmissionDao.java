package com.pyjudge.dao;

import com.pyjudge.db.Database;
import com.pyjudge.model.ContentStatus;
import com.pyjudge.model.Difficulty;
import com.pyjudge.model.ExerciseStats;
import com.pyjudge.model.Page;
import com.pyjudge.model.Problem;
import com.pyjudge.model.Submission;
import com.pyjudge.model.Verdict;
import com.pyjudge.util.Times;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class SubmissionDao extends BaseDao {
    private static final String FROM =
            " FROM submissions s"
                    + " JOIN users u ON u.id = s.user_id"
                    + " JOIN problems p ON p.id = s.problem_id"
                    + " LEFT JOIN users g ON g.id = s.graded_by";
    private static final String SELECT =
            "SELECT s.*, " + userColumns("u", "student") + ", " + userColumns("g", "grader")
                    + ", p.title AS problem_title, p.slug AS problem_slug, p.difficulty AS problem_difficulty,"
                    + " p.created_by AS problem_created_by, p.status AS problem_status" + FROM;
    private static final String PENDING = Verdict.PENDING.name();
    private static final String ACCEPTED = Verdict.AC.name();

    /** One judged submission, as needed by the instructor report. */
    public record ReportRow(int userId, int problemId, Verdict verdict, Integer grade, LocalDateTime submittedAt) {
    }

    /** One submission, as needed by the admin activity chart. */
    public record ActivityRow(LocalDateTime submittedAt, Verdict verdict, int userId) {
    }

    /** One (user, exercise) pair with an accepted submission. */
    public record Solve(int userId, int problemId, Difficulty difficulty) {
    }

    public SubmissionDao(Database db) {
        super(db);
    }

    private static Submission map(ResultSet rs) throws SQLException {
        Submission s = new Submission();
        s.setId(rs.getInt("id"));
        s.setUserId(rs.getInt("user_id"));
        s.setProblemId(rs.getInt("problem_id"));
        s.setCode(rs.getString("code"));
        s.setVerdict(Verdict.fromName(rs.getString("verdict")));
        s.setPassed(rs.getInt("passed"));
        s.setTotal(rs.getInt("total"));
        s.setRuntimeMs(intOrNull(rs, "runtime_ms"));
        s.setMemoryKb(intOrNull(rs, "memory_kb"));
        s.setError(rs.getString("error"));
        s.setSubmittedAt(timestamp(rs, "submitted_at"));
        s.setGrade(intOrNull(rs, "grade"));
        s.setFeedback(rs.getString("feedback"));
        s.setGradedBy(intOrNull(rs, "graded_by"));
        s.setGradedAt(timestamp(rs, "graded_at"));
        s.setUser(joinedUser(rs, "student"));
        s.setGrader(joinedUser(rs, "grader"));

        Problem p = new Problem();
        p.setId(s.getProblemId());
        p.setTitle(rs.getString("problem_title"));
        p.setSlug(rs.getString("problem_slug"));
        p.setDifficulty(Difficulty.fromLabel(rs.getString("problem_difficulty")));
        p.setCreatedBy(intOrNull(rs, "problem_created_by"));
        p.setStatus(ContentStatus.fromKey(rs.getString("problem_status")));
        s.setProblem(p);
        return s;
    }

    // ------------------------------------------------------------ single submissions

    public Optional<Submission> findById(int id) {
        return db.queryOne(SELECT + " WHERE s.id = ?", SubmissionDao::map, id);
    }

    public Submission insert(Submission s) {
        if (s.getSubmittedAt() == null) {
            s.setSubmittedAt(Times.now());
        }
        int id = db.insert("INSERT INTO submissions (user_id, problem_id, code, verdict, passed, total, runtime_ms,"
                        + " error, submitted_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                s.getUserId(), s.getProblemId(), s.getCode(), s.getVerdict().name(), s.getPassed(), s.getTotal(),
                s.getRuntimeMs(), s.getError(), s.getSubmittedAt());
        s.setId(id);
        return s;
    }

    /** Stores the judge's result for a submission. */
    public void saveResult(int id, Verdict verdict, int passed, int total, Integer runtimeMs, String error) {
        db.update("UPDATE submissions SET verdict = ?, passed = ?, total = ?, runtime_ms = ?, error = ? WHERE id = ?",
                verdict.name(), passed, total, runtimeMs, error, id);
    }

    /** Stores an instructor's grade and feedback. */
    public void saveGrade(int id, int grade, String feedback, int graderId) {
        db.update("UPDATE submissions SET grade = ?, feedback = ?, graded_by = ?, graded_at = ? WHERE id = ?",
                grade, feedback, graderId, Times.now(), id);
    }

    public Optional<Submission> lastFor(int userId, int problemId) {
        return db.queryOne(SELECT + " WHERE s.user_id = ? AND s.problem_id = ? ORDER BY s.id DESC LIMIT 1",
                SubmissionDao::map, userId, problemId);
    }

    // ------------------------------------------------------------ lists

    public Page<Submission> pageByUser(int userId, int page, int perPage) {
        long total = db.count("SELECT COUNT(*) FROM submissions WHERE user_id = ?", userId);
        List<Submission> items = db.query(SELECT + " WHERE s.user_id = ? ORDER BY s.id DESC LIMIT ? OFFSET ?",
                SubmissionDao::map, userId, perPage, offset(page, perPage));
        return new Page<>(items, page, perPage, total);
    }

    public List<Submission> recent(int limit) {
        return db.query(SELECT + " ORDER BY s.id DESC LIMIT ?", SubmissionDao::map, limit);
    }

    public List<Submission> recentByUser(int userId, int limit) {
        return db.query(SELECT + " WHERE s.user_id = ? ORDER BY s.id DESC LIMIT ?", SubmissionDao::map, userId, limit);
    }

    /** A student's most recently graded submissions. */
    public List<Submission> recentFeedback(int userId, int limit) {
        return db.query(SELECT + " WHERE s.user_id = ? AND s.grade IS NOT NULL ORDER BY s.graded_at DESC, s.id DESC LIMIT ?",
                SubmissionDao::map, userId, limit);
    }

    private static int offset(int page, int perPage) {
        return Math.max(0, page - 1) * perPage;
    }

    // ------------------------------------------------------------ instructor views

    /** Judged submissions to exercises written by {@code authorId} (every exercise when it is null). */
    private static Where judgedFor(Integer authorId) {
        Where where = new Where().and("s.verdict <> ?", PENDING);
        if (authorId != null) {
            where.and("p.created_by = ?", authorId);
        }
        return where;
    }

    /** @param show {@code ungraded}, {@code graded} or anything else for all */
    public Page<Submission> pageForAuthor(Integer authorId, String show, Integer problemId, int page, int perPage) {
        Where where = judgedFor(authorId);
        if ("ungraded".equals(show)) {
            where.and("s.grade IS NULL");
        } else if ("graded".equals(show)) {
            where.and("s.grade IS NOT NULL");
        }
        if (problemId != null) {
            where.and("s.problem_id = ?", problemId);
        }
        long total = db.count("SELECT COUNT(*)" + FROM + where.sql(), where.params());
        List<Submission> items = db.query(SELECT + where.sql() + " ORDER BY s.id DESC LIMIT ? OFFSET ?",
                SubmissionDao::map, where.params(perPage, offset(page, perPage)));
        return new Page<>(items, page, perPage, total);
    }

    public long countJudgedFor(Integer authorId) {
        Where where = judgedFor(authorId);
        return db.count("SELECT COUNT(*)" + FROM + where.sql(), where.params());
    }

    public long countUngradedFor(Integer authorId) {
        Where where = judgedFor(authorId).and("s.grade IS NULL");
        return db.count("SELECT COUNT(*)" + FROM + where.sql(), where.params());
    }

    public long countStudentsFor(Integer authorId) {
        Where where = judgedFor(authorId);
        return db.count("SELECT COUNT(DISTINCT s.user_id)" + FROM + where.sql(), where.params());
    }

    public List<Submission> ungradedFor(Integer authorId, int limit) {
        Where where = judgedFor(authorId).and("s.grade IS NULL");
        return db.query(SELECT + where.sql() + " ORDER BY s.id DESC LIMIT ?", SubmissionDao::map, where.params(limit));
    }

    // ------------------------------------------------------------ statistics

    public long countAll() {
        return db.count("SELECT COUNT(*) FROM submissions");
    }

    public long countJudged() {
        return db.count("SELECT COUNT(*) FROM submissions WHERE verdict <> ?", PENDING);
    }

    public long countAccepted() {
        return db.count("SELECT COUNT(*) FROM submissions WHERE verdict = ?", ACCEPTED);
    }

    public long countByUser(int userId) {
        return db.count("SELECT COUNT(*) FROM submissions WHERE user_id = ?", userId);
    }

    /** Submissions of one student still waiting for the judge. */
    public long countPending(int userId) {
        return db.count("SELECT COUNT(*) FROM submissions WHERE user_id = ? AND verdict = ?", userId, PENDING);
    }

    public Map<Integer, Integer> countsByUser() {
        return countsByInt("SELECT user_id, COUNT(*) FROM submissions GROUP BY user_id");
    }

    public Map<String, Integer> verdictCounts(int userId) {
        return countsByString("SELECT verdict, COUNT(*) FROM submissions WHERE user_id = ? GROUP BY verdict", userId);
    }

    public Set<Integer> solvedProblemIds(int userId) {
        return intSet("SELECT DISTINCT problem_id FROM submissions WHERE user_id = ? AND verdict = ?", userId, ACCEPTED);
    }

    public Set<Integer> attemptedProblemIds(int userId) {
        return intSet("SELECT DISTINCT problem_id FROM submissions WHERE user_id = ?", userId);
    }

    /** Exercises of a challenge that the user solved inside its time window. */
    public Set<Integer> solvedInWindow(int userId, Collection<Integer> problemIds, LocalDateTime from, LocalDateTime to) {
        if (problemIds.isEmpty()) {
            return Set.of();
        }
        Where where = new Where()
                .and("user_id = ?", userId)
                .and("verdict = ?", ACCEPTED)
                .andIn("problem_id", problemIds)
                .and("submitted_at >= ?", from)
                .and("submitted_at <= ?", to);
        return intSet("SELECT DISTINCT problem_id FROM submissions" + where.sql(), where.params());
    }

    /** When the user first solved each exercise (one timestamp per solved exercise). */
    public List<LocalDateTime> firstAcceptedTimes(int userId) {
        return db.query("SELECT MIN(submitted_at) FROM submissions WHERE user_id = ? AND verdict = ? GROUP BY problem_id",
                rs -> rs.getObject(1, LocalDateTime.class), userId, ACCEPTED);
    }

    public List<LocalDateTime> submissionTimes(int userId) {
        return db.query("SELECT submitted_at FROM submissions WHERE user_id = ?",
                rs -> rs.getObject(1, LocalDateTime.class), userId);
    }

    /** Rounded average of the grades a student has received, or null when nothing is graded. */
    public Integer averageGrade(int userId) {
        return db.queryOne("SELECT AVG(grade) FROM submissions WHERE user_id = ? AND grade IS NOT NULL",
                rs -> {
                    double avg = rs.getDouble(1);
                    return rs.wasNull() ? null : (int) Math.round(avg);
                }, userId).orElse(null);
    }

    /**
     * {problemId: stats} over judged submissions. Uses conditional aggregation so each exercise
     * needs one row: total, accepted, distinct students and distinct solvers.
     */
    public Map<Integer, ExerciseStats> acceptanceStats(Collection<Integer> problemIds) {
        Where where = new Where().and("verdict <> ?", PENDING);
        if (problemIds != null) {
            where.andIn("problem_id", problemIds);
        }
        Map<Integer, ExerciseStats> out = new LinkedHashMap<>();
        db.query("SELECT problem_id, COUNT(*),"
                        + " SUM(CASE WHEN verdict = 'AC' THEN 1 ELSE 0 END),"
                        + " COUNT(DISTINCT user_id),"
                        + " COUNT(DISTINCT CASE WHEN verdict = 'AC' THEN user_id END)"
                        + " FROM submissions" + where.sql() + " GROUP BY problem_id",
                rs -> out.put(rs.getInt(1), new ExerciseStats(rs.getInt(2), rs.getInt(3), rs.getInt(4), rs.getInt(5))),
                where.params());
        return out;
    }

    /** {problemId: rounded average grade} for exercises that have graded submissions. */
    public Map<Integer, Integer> averageGradeByProblem(Collection<Integer> problemIds) {
        if (problemIds.isEmpty()) {
            return Map.of();
        }
        Where where = new Where().and("grade IS NOT NULL").andIn("problem_id", problemIds);
        Map<Integer, Integer> out = new LinkedHashMap<>();
        db.query("SELECT problem_id, AVG(grade) FROM submissions" + where.sql() + " GROUP BY problem_id",
                rs -> out.put(rs.getInt(1), (int) Math.round(rs.getDouble(2))), where.params());
        return out;
    }

    public List<ReportRow> reportRows(Collection<Integer> problemIds) {
        if (problemIds.isEmpty()) {
            return List.of();
        }
        Where where = new Where().and("verdict <> ?", PENDING).andIn("problem_id", problemIds);
        return db.query("SELECT user_id, problem_id, verdict, grade, submitted_at FROM submissions" + where.sql(),
                rs -> new ReportRow(rs.getInt(1), rs.getInt(2), Verdict.fromName(rs.getString(3)),
                        intOrNull(rs, "grade"), rs.getObject(5, LocalDateTime.class)),
                where.params());
    }

    public List<ActivityRow> activitySince(LocalDateTime since) {
        return db.query("SELECT submitted_at, verdict, user_id FROM submissions WHERE submitted_at >= ?",
                rs -> new ActivityRow(rs.getObject(1, LocalDateTime.class), Verdict.fromName(rs.getString(2)), rs.getInt(3)),
                since);
    }

    /**
     * Distinct (user, exercise) solves of approved exercises, the raw material of the leaderboards.
     * All three filters are optional.
     */
    public List<Solve> solves(Collection<Integer> problemIds, LocalDateTime since, LocalDateTime until) {
        Where where = new Where().and("s.verdict = ?", ACCEPTED).and("p.status = ?", ContentStatus.APPROVED.key());
        if (problemIds != null) {
            where.andIn("s.problem_id", problemIds);
        }
        if (since != null) {
            where.and("s.submitted_at >= ?", since);
        }
        if (until != null) {
            where.and("s.submitted_at <= ?", until);
        }
        return db.query("SELECT s.user_id, s.problem_id, p.difficulty FROM submissions s"
                        + " JOIN problems p ON p.id = s.problem_id" + where.sql()
                        + " GROUP BY s.user_id, s.problem_id, p.difficulty",
                rs -> new Solve(rs.getInt(1), rs.getInt(2), Difficulty.fromLabel(rs.getString(3))),
                where.params());
    }
}
