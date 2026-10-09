package com.pyjudge.dao;

import com.pyjudge.db.Database;
import com.pyjudge.model.LessonProgress;
import com.pyjudge.util.Times;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ProgressDao extends BaseDao {
    private static final String SELECT =
            "SELECT id, user_id, lesson_id, completed_at, quiz_score, quiz_total, attempts, updated_at FROM lesson_progress";

    public ProgressDao(Database db) {
        super(db);
    }

    private static LessonProgress map(ResultSet rs) throws SQLException {
        LessonProgress p = new LessonProgress();
        p.setId(rs.getInt("id"));
        p.setUserId(rs.getInt("user_id"));
        p.setLessonId(rs.getInt("lesson_id"));
        p.setCompletedAt(timestamp(rs, "completed_at"));
        p.setQuizScore(intOrNull(rs, "quiz_score"));
        p.setQuizTotal(intOrNull(rs, "quiz_total"));
        p.setAttempts(rs.getInt("attempts"));
        p.setUpdatedAt(timestamp(rs, "updated_at"));
        return p;
    }

    public Optional<LessonProgress> find(int userId, int lessonId) {
        return db.queryOne(SELECT + " WHERE user_id = ? AND lesson_id = ?", ProgressDao::map, userId, lessonId);
    }

    /** {lessonId: progress} for one user. */
    public Map<Integer, LessonProgress> byUser(int userId) {
        Map<Integer, LessonProgress> out = new LinkedHashMap<>();
        db.query(SELECT + " WHERE user_id = ?", ProgressDao::map, userId).forEach(p -> out.put(p.getLessonId(), p));
        return out;
    }

    /** Inserts the row, or updates it when the user already has progress on the lesson. */
    public void save(LessonProgress p) {
        p.setUpdatedAt(Times.now());
        if (p.getId() == 0) {
            p.setId(db.insert("INSERT INTO lesson_progress (user_id, lesson_id, completed_at, quiz_score, quiz_total,"
                            + " attempts, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    p.getUserId(), p.getLessonId(), p.getCompletedAt(), p.getQuizScore(), p.getQuizTotal(),
                    p.getAttempts(), p.getUpdatedAt()));
        } else {
            db.update("UPDATE lesson_progress SET completed_at = ?, quiz_score = ?, quiz_total = ?, attempts = ?,"
                            + " updated_at = ? WHERE id = ?",
                    p.getCompletedAt(), p.getQuizScore(), p.getQuizTotal(), p.getAttempts(), p.getUpdatedAt(), p.getId());
        }
    }

    public long countCompleted() {
        return db.count("SELECT COUNT(*) FROM lesson_progress WHERE completed_at IS NOT NULL");
    }

    public List<LocalDateTime> completionTimes(int userId) {
        return db.query("SELECT completed_at FROM lesson_progress WHERE user_id = ? AND completed_at IS NOT NULL",
                rs -> rs.getObject(1, LocalDateTime.class), userId);
    }

    /** {lessonId: students who completed it}. */
    public Map<Integer, Integer> completionsByLesson(Collection<Integer> lessonIds) {
        if (lessonIds.isEmpty()) {
            return Map.of();
        }
        Where where = new Where().and("completed_at IS NOT NULL").andIn("lesson_id", lessonIds);
        return countsByInt("SELECT lesson_id, COUNT(*) FROM lesson_progress" + where.sql() + " GROUP BY lesson_id",
                where.params());
    }

    /** {lessonId: average quiz score as a percentage} over every student's best attempt. */
    public Map<Integer, Integer> averageQuizByLesson(Collection<Integer> lessonIds) {
        if (lessonIds.isEmpty()) {
            return Map.of();
        }
        Where where = new Where().and("quiz_total IS NOT NULL").andIn("lesson_id", lessonIds);
        Map<Integer, Integer> out = new LinkedHashMap<>();
        db.query("SELECT lesson_id, SUM(quiz_score), SUM(quiz_total) FROM lesson_progress" + where.sql()
                        + " GROUP BY lesson_id",
                rs -> {
                    long total = rs.getLong(3);
                    return total > 0 ? out.put(rs.getInt(1), (int) Math.round(100.0 * rs.getLong(2) / total)) : null;
                }, where.params());
        return out;
    }
}
