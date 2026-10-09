package com.pyjudge.dao;

import com.pyjudge.db.Database;
import com.pyjudge.model.Challenge;
import com.pyjudge.model.Problem;
import com.pyjudge.util.Times;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ChallengeDao extends BaseDao {
    private static final String SELECT =
            "SELECT c.*, " + userColumns("a", "author") + " FROM challenges c LEFT JOIN users a ON a.id = c.created_by";

    public ChallengeDao(Database db) {
        super(db);
    }

    private static Challenge map(ResultSet rs) throws SQLException {
        Challenge c = new Challenge();
        c.setId(rs.getInt("id"));
        c.setTitle(rs.getString("title"));
        c.setDescription(rs.getString("description"));
        c.setStartAt(timestamp(rs, "start_at"));
        c.setEndAt(timestamp(rs, "end_at"));
        c.setCreatedBy(intOrNull(rs, "created_by"));
        c.setCreatedAt(timestamp(rs, "created_at"));
        c.setAuthor(joinedUser(rs, "author"));
        return c;
    }

    private Challenge withProblems(Challenge c) {
        c.setProblems(db.query(ProblemDao.SELECT + " JOIN challenge_problems cp ON cp.problem_id = p.id"
                + " WHERE cp.challenge_id = ? ORDER BY p.id", ProblemDao::map, c.getId()));
        return c;
    }

    public Optional<Challenge> findById(int id) {
        return db.queryOne(SELECT + " WHERE c.id = ?", ChallengeDao::map, id).map(this::withProblems);
    }

    /** @param newestFirst order by start time descending; otherwise by end time ascending */
    public List<Challenge> listAll(boolean newestFirst) {
        List<Challenge> all = db.query(SELECT + " ORDER BY " + (newestFirst ? "c.start_at DESC" : "c.end_at"),
                ChallengeDao::map);
        all.forEach(this::withProblems);
        return all;
    }

    public long count() {
        return db.count("SELECT COUNT(*) FROM challenges");
    }

    /** Inserts the challenge and its exercise links in one transaction. */
    public Challenge insert(Challenge c) {
        return db.inTransaction(() -> {
            c.setCreatedAt(Times.now());
            c.setId(db.insert("INSERT INTO challenges (title, description, start_at, end_at, created_by, created_at)"
                            + " VALUES (?, ?, ?, ?, ?, ?)",
                    c.getTitle(), c.getDescription(), c.getStartAt(), c.getEndAt(), c.getCreatedBy(), c.getCreatedAt()));
            linkProblems(c);
            return c;
        });
    }

    public void update(Challenge c) {
        db.inTransaction(() -> {
            db.update("UPDATE challenges SET title = ?, description = ?, start_at = ?, end_at = ? WHERE id = ?",
                    c.getTitle(), c.getDescription(), c.getStartAt(), c.getEndAt(), c.getId());
            db.update("DELETE FROM challenge_problems WHERE challenge_id = ?", c.getId());
            linkProblems(c);
        });
    }

    private void linkProblems(Challenge c) {
        List<Object[]> rows = new ArrayList<>();
        for (Problem p : c.getProblems()) {
            rows.add(new Object[] {c.getId(), p.getId()});
        }
        db.batch("INSERT INTO challenge_problems (challenge_id, problem_id) VALUES (?, ?)", rows);
    }

    public void delete(int id) {
        db.update("DELETE FROM challenges WHERE id = ?", id);
    }
}
