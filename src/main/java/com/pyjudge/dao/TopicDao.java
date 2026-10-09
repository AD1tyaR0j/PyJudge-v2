package com.pyjudge.dao;

import com.pyjudge.db.Database;
import com.pyjudge.model.Topic;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class TopicDao extends BaseDao {
    private static final String SELECT = "SELECT id, slug, title, icon, level, summary, description, sort_order FROM topics";

    public TopicDao(Database db) {
        super(db);
    }

    private static Topic map(ResultSet rs) throws SQLException {
        Topic t = new Topic();
        t.setId(rs.getInt("id"));
        t.setSlug(rs.getString("slug"));
        t.setTitle(rs.getString("title"));
        t.setIcon(rs.getString("icon"));
        t.setLevel(rs.getString("level"));
        t.setSummary(rs.getString("summary"));
        t.setDescription(rs.getString("description"));
        t.setPosition(rs.getInt("sort_order"));
        return t;
    }

    public List<Topic> listAll() {
        return db.query(SELECT + " ORDER BY sort_order, id", TopicDao::map);
    }

    public Optional<Topic> findById(int id) {
        return db.queryOne(SELECT + " WHERE id = ?", TopicDao::map, id);
    }

    public Optional<Topic> findBySlug(String slug) {
        return db.queryOne(SELECT + " WHERE slug = ?", TopicDao::map, slug);
    }

    public Topic insert(Topic t) {
        t.setId(db.insert("INSERT INTO topics (slug, title, icon, level, summary, description, sort_order)"
                        + " VALUES (?, ?, ?, ?, ?, ?, ?)",
                t.getSlug(), t.getTitle(), t.getIcon(), t.getLevel(), t.getSummary(), t.getDescription(), t.getPosition()));
        return t;
    }

    public void update(Topic t) {
        db.update("UPDATE topics SET title = ?, icon = ?, level = ?, summary = ?, description = ?, sort_order = ? WHERE id = ?",
                t.getTitle(), t.getIcon(), t.getLevel(), t.getSummary(), t.getDescription(), t.getPosition(), t.getId());
    }
}
