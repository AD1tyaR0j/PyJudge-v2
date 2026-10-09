package com.pyjudge.dao;

import com.pyjudge.db.Database;
import com.pyjudge.model.ContentStatus;
import com.pyjudge.model.Lesson;
import com.pyjudge.model.Topic;
import com.pyjudge.util.Times;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class LessonDao extends BaseDao {
    private static final String COLUMNS =
            "l.id, l.topic_id, l.slug, l.title, l.minutes, l.practice_tags, l.sort_order, l.source, l.status,"
                    + " l.review_note, l.reviewed_by, l.reviewed_at, l.created_by, l.created_at, l.updated_at,"
                    + " t.slug AS topic_slug, t.title AS topic_title, t.icon AS topic_icon, t.level AS topic_level,"
                    + " t.sort_order AS topic_sort_order, " + userColumns("a", "author")
                    + ", (SELECT COUNT(*) FROM quiz_questions q WHERE q.lesson_id = l.id) AS question_count";
    private static final String FROM = " FROM lessons l JOIN topics t ON t.id = l.topic_id LEFT JOIN users a ON a.id = l.created_by";
    /** Full rows, including the Markdown body. */
    private static final String SELECT = "SELECT l.content, " + COLUMNS + FROM;
    /** Rows without the body, for lists and progress calculations. */
    private static final String SELECT_SUMMARY = "SELECT '' AS content, " + COLUMNS + FROM;
    private static final String COURSE_ORDER = " ORDER BY t.sort_order, t.id, l.sort_order, l.id";

    public LessonDao(Database db) {
        super(db);
    }

    private static Lesson map(ResultSet rs) throws SQLException {
        Lesson l = new Lesson();
        l.setId(rs.getInt("id"));
        l.setTopicId(rs.getInt("topic_id"));
        l.setSlug(rs.getString("slug"));
        l.setTitle(rs.getString("title"));
        l.setContent(rs.getString("content"));
        l.setMinutes(rs.getInt("minutes"));
        l.setPracticeTags(rs.getString("practice_tags"));
        l.setPosition(rs.getInt("sort_order"));
        l.setSource(rs.getString("source"));
        l.setStatus(ContentStatus.fromKey(rs.getString("status")));
        l.setReviewNote(rs.getString("review_note"));
        l.setReviewedBy(intOrNull(rs, "reviewed_by"));
        l.setReviewedAt(timestamp(rs, "reviewed_at"));
        l.setCreatedBy(intOrNull(rs, "created_by"));
        l.setCreatedAt(timestamp(rs, "created_at"));
        l.setUpdatedAt(timestamp(rs, "updated_at"));
        l.setAuthor(joinedUser(rs, "author"));
        l.setQuestionCount(rs.getInt("question_count"));

        Topic t = new Topic();
        t.setId(l.getTopicId());
        t.setSlug(rs.getString("topic_slug"));
        t.setTitle(rs.getString("topic_title"));
        t.setIcon(rs.getString("topic_icon"));
        t.setLevel(rs.getString("topic_level"));
        t.setPosition(rs.getInt("topic_sort_order"));
        l.setTopic(t);
        return l;
    }

    public Optional<Lesson> findById(int id) {
        return db.queryOne(SELECT + " WHERE l.id = ?", LessonDao::map, id);
    }

    public Optional<Lesson> find(int topicId, String slug) {
        return db.queryOne(SELECT + " WHERE l.topic_id = ? AND l.slug = ?", LessonDao::map, topicId, slug);
    }

    public boolean slugTaken(int topicId, String slug) {
        return db.count("SELECT COUNT(*) FROM lessons WHERE topic_id = ? AND slug = ?", topicId, slug) > 0;
    }

    public int countInTopic(int topicId) {
        return (int) db.count("SELECT COUNT(*) FROM lessons WHERE topic_id = ?", topicId);
    }

    public long count(ContentStatus status) {
        return db.count("SELECT COUNT(*) FROM lessons WHERE status = ?", status.key());
    }

    /** Every published lesson in course order (without bodies). */
    public List<Lesson> listApproved() {
        return db.query(SELECT_SUMMARY + " WHERE l.status = ?" + COURSE_ORDER, LessonDao::map, ContentStatus.APPROVED.key());
    }

    public List<Lesson> listPending() {
        return db.query(SELECT_SUMMARY + " WHERE l.status = ? ORDER BY l.updated_at, l.id", LessonDao::map,
                ContentStatus.PENDING.key());
    }

    /** Lessons written by {@code authorId}; the whole course when it is null (the admin view). */
    public List<Lesson> listByAuthor(Integer authorId) {
        return authorId == null
                ? db.query(SELECT_SUMMARY + COURSE_ORDER, LessonDao::map)
                : db.query(SELECT_SUMMARY + " WHERE l.created_by = ?" + COURSE_ORDER, LessonDao::map, authorId);
    }

    public Lesson insert(Lesson l) {
        l.setCreatedAt(Times.now());
        l.setUpdatedAt(l.getCreatedAt());
        l.setId(db.insert("INSERT INTO lessons (topic_id, slug, title, content, minutes, practice_tags, sort_order,"
                        + " source, status, review_note, reviewed_by, reviewed_at, created_by, created_at, updated_at)"
                        + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                l.getTopicId(), l.getSlug(), l.getTitle(), l.getContent(), l.getMinutes(), l.getPracticeTags(),
                l.getPosition(), l.getSource(), l.getStatus().key(), l.getReviewNote(), l.getReviewedBy(),
                l.getReviewedAt(), l.getCreatedBy(), l.getCreatedAt(), l.getUpdatedAt()));
        return l;
    }

    public void update(Lesson l) {
        l.setUpdatedAt(Times.now());
        db.update("UPDATE lessons SET topic_id = ?, title = ?, content = ?, minutes = ?, practice_tags = ?,"
                        + " sort_order = ?, status = ?, review_note = ?, reviewed_by = ?, reviewed_at = ?, updated_at = ?"
                        + " WHERE id = ?",
                l.getTopicId(), l.getTitle(), l.getContent(), l.getMinutes(), l.getPracticeTags(), l.getPosition(),
                l.getStatus().key(), l.getReviewNote(), l.getReviewedBy(), l.getReviewedAt(), l.getUpdatedAt(), l.getId());
    }

    /** Deletes the lesson with its quiz and all student progress on it (ON DELETE CASCADE). */
    public void delete(int id) {
        db.update("DELETE FROM lessons WHERE id = ?", id);
    }
}
