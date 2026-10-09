package com.pyjudge.dao;

import com.pyjudge.db.Database;
import com.pyjudge.model.ContentStatus;
import com.pyjudge.model.Difficulty;
import com.pyjudge.model.Problem;
import com.pyjudge.util.Slugs;
import com.pyjudge.util.Times;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class ProblemDao extends BaseDao {
    static final String SELECT =
            "SELECT p.*, " + userColumns("a", "author") + ", " + userColumns("r", "reviewer")
                    + " FROM problems p"
                    + " LEFT JOIN users a ON a.id = p.created_by"
                    + " LEFT JOIN users r ON r.id = p.reviewed_by";

    public ProblemDao(Database db) {
        super(db);
    }

    static Problem map(ResultSet rs) throws SQLException {
        Problem p = new Problem();
        p.setId(rs.getInt("id"));
        p.setTitle(rs.getString("title"));
        p.setSlug(rs.getString("slug"));
        p.setDescription(rs.getString("description"));
        p.setDifficulty(Difficulty.fromLabel(rs.getString("difficulty")));
        p.setTimeLimit(rs.getInt("time_limit"));
        p.setMemoryLimit(rs.getInt("memory_limit"));
        p.setTags(rs.getString("tags"));
        p.setStarterCode(rs.getString("starter_code"));
        p.setStatus(ContentStatus.fromKey(rs.getString("status")));
        p.setReviewNote(rs.getString("review_note"));
        p.setReviewedBy(intOrNull(rs, "reviewed_by"));
        p.setReviewedAt(timestamp(rs, "reviewed_at"));
        p.setCreatedBy(intOrNull(rs, "created_by"));
        p.setCreatedAt(timestamp(rs, "created_at"));
        p.setUpdatedAt(timestamp(rs, "updated_at"));
        p.setAuthor(joinedUser(rs, "author"));
        p.setReviewer(joinedUser(rs, "reviewer"));
        return p;
    }

    public Optional<Problem> findById(int id) {
        return db.queryOne(SELECT + " WHERE p.id = ?", ProblemDao::map, id);
    }

    public Optional<Problem> findBySlug(String slug) {
        return db.queryOne(SELECT + " WHERE p.slug = ?", ProblemDao::map, slug);
    }

    public boolean existsByTitle(String title) {
        return db.count("SELECT COUNT(*) FROM problems WHERE title = ?", title) > 0;
    }

    public List<Problem> findByIds(Collection<Integer> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        Where where = new Where().andIn("p.id", ids);
        return db.query(SELECT + where.sql() + " ORDER BY p.id", ProblemDao::map, where.params());
    }

    /** Every exercise students can see, oldest first. */
    public List<Problem> listApproved() {
        return list(ContentStatus.APPROVED);
    }

    /** The public exercise list with its difficulty, tag and title filters. */
    public List<Problem> searchApproved(Difficulty difficulty, String tag, String q) {
        Where where = new Where().and("p.status = ?", ContentStatus.APPROVED.key());
        if (difficulty != null) {
            where.and("p.difficulty = ?", difficulty.label());
        }
        if (tag != null && !tag.isBlank()) {
            where.and("LOWER(p.tags) LIKE ?", "%" + tag.toLowerCase(Locale.ROOT) + "%");
        }
        if (q != null && !q.isBlank()) {
            where.and("LOWER(p.title) LIKE ?", "%" + q.toLowerCase(Locale.ROOT) + "%");
        }
        return db.query(SELECT + where.sql() + " ORDER BY p.id", ProblemDao::map, where.params());
    }

    /** All exercises, or only those with {@code status} when it is not null. */
    public List<Problem> list(ContentStatus status) {
        Where where = new Where();
        if (status != null) {
            where.and("p.status = ?", status.key());
        }
        return db.query(SELECT + where.sql() + " ORDER BY p.id", ProblemDao::map, where.params());
    }

    /** Exercises written by {@code authorId}; every exercise when it is null (the admin view). */
    public List<Problem> listByAuthor(Integer authorId, ContentStatus status, String orderBy) {
        Where where = new Where();
        if (authorId != null) {
            where.and("p.created_by = ?", authorId);
        }
        if (status != null) {
            where.and("p.status = ?", status.key());
        }
        String order = switch (orderBy) {
            case "title" -> "p.title";
            case "newest" -> "p.id DESC";
            default -> "p.id";
        };
        return db.query(SELECT + where.sql() + " ORDER BY " + order, ProblemDao::map, where.params());
    }

    public Set<Integer> idsByAuthor(Integer authorId) {
        return authorId == null
                ? intSet("SELECT id FROM problems")
                : intSet("SELECT id FROM problems WHERE created_by = ?", authorId);
    }

    public List<Problem> listPending(int limit) {
        return db.query(SELECT + " WHERE p.status = ? ORDER BY p.updated_at, p.id LIMIT ?", ProblemDao::map,
                ContentStatus.PENDING.key(), limit);
    }

    public List<Problem> listApproved(Difficulty difficulty, int limit) {
        return db.query(SELECT + " WHERE p.status = ? AND p.difficulty = ? ORDER BY p.id LIMIT ?", ProblemDao::map,
                ContentStatus.APPROVED.key(), difficulty.label(), limit);
    }

    public Map<String, Integer> countByStatus() {
        return countsByString("SELECT status, COUNT(*) FROM problems GROUP BY status");
    }

    public long count(ContentStatus status) {
        return db.count("SELECT COUNT(*) FROM problems WHERE status = ?", status.key());
    }

    public long countAll() {
        return db.count("SELECT COUNT(*) FROM problems");
    }

    /** A slug for {@code title} that no other exercise uses: two-sum, two-sum-2, two-sum-3... */
    public String uniqueSlug(String title, Integer excludeId) {
        String base = Slugs.slugify(title);
        String slug = base;
        for (int n = 2; slugTaken(slug, excludeId); n++) {
            slug = base + "-" + n;
        }
        return slug;
    }

    private boolean slugTaken(String slug, Integer excludeId) {
        return db.queryOne("SELECT id FROM problems WHERE slug = ?", rs -> rs.getInt(1), slug)
                .filter(id -> !id.equals(excludeId))
                .isPresent();
    }

    public Problem insert(Problem p) {
        p.setCreatedAt(Times.now());
        p.setUpdatedAt(p.getCreatedAt());
        int id = db.insert(
                "INSERT INTO problems (title, slug, description, difficulty, time_limit, memory_limit, tags,"
                        + " starter_code, status, review_note, reviewed_by, reviewed_at, created_by, created_at, updated_at)"
                        + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                p.getTitle(), p.getSlug(), p.getDescription(), p.getDifficulty().label(), p.getTimeLimit(),
                p.getMemoryLimit(), p.getTags(), p.getStarterCode(), p.getStatus().key(), p.getReviewNote(),
                p.getReviewedBy(), p.getReviewedAt(), p.getCreatedBy(), p.getCreatedAt(), p.getUpdatedAt());
        p.setId(id);
        return p;
    }

    public void update(Problem p) {
        p.setUpdatedAt(Times.now());
        db.update("UPDATE problems SET title = ?, slug = ?, description = ?, difficulty = ?, time_limit = ?,"
                        + " memory_limit = ?, tags = ?, starter_code = ?, status = ?, review_note = ?, reviewed_by = ?,"
                        + " reviewed_at = ?, updated_at = ? WHERE id = ?",
                p.getTitle(), p.getSlug(), p.getDescription(), p.getDifficulty().label(), p.getTimeLimit(),
                p.getMemoryLimit(), p.getTags(), p.getStarterCode(), p.getStatus().key(), p.getReviewNote(),
                p.getReviewedBy(), p.getReviewedAt(), p.getUpdatedAt(), p.getId());
    }

    /** Deletes the exercise with its test cases and submissions (ON DELETE CASCADE). */
    public void delete(int id) {
        db.update("DELETE FROM problems WHERE id = ?", id);
    }
}
