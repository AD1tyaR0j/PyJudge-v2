package com.pyjudge.dao;

import com.pyjudge.db.Database;
import com.pyjudge.model.Role;
import com.pyjudge.model.User;
import com.pyjudge.util.Times;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class UserDao extends BaseDao {
    private static final String SELECT =
            "SELECT id, username, name, email, bio, password_hash, role, is_blocked, created_at, last_seen_at FROM users";

    public UserDao(Database db) {
        super(db);
    }

    private static User map(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setUsername(rs.getString("username"));
        user.setName(rs.getString("name"));
        user.setEmail(rs.getString("email"));
        user.setBio(rs.getString("bio"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setRole(Role.fromKey(rs.getString("role")));
        user.setBlocked(rs.getBoolean("is_blocked"));
        user.setCreatedAt(timestamp(rs, "created_at"));
        user.setLastSeenAt(timestamp(rs, "last_seen_at"));
        return user;
    }

    public Optional<User> findById(int id) {
        return db.queryOne(SELECT + " WHERE id = ?", UserDao::map, id);
    }

    public Optional<User> findByUsername(String username) {
        return db.queryOne(SELECT + " WHERE username = ?", UserDao::map, username);
    }

    public Optional<User> findByEmail(String email) {
        return db.queryOne(SELECT + " WHERE email = ?", UserDao::map, email);
    }

    /** Login accepts either the username or the (lowercased) email address. */
    public Optional<User> findByLogin(String identifier) {
        return db.queryOne(SELECT + " WHERE username = ? OR email = ?", UserDao::map,
                identifier, identifier.toLowerCase(Locale.ROOT));
    }

    public Map<Integer, User> findByIds(Collection<Integer> ids) {
        Map<Integer, User> out = new LinkedHashMap<>();
        if (!ids.isEmpty()) {
            Where where = new Where().andIn("id", ids);
            db.query(SELECT + where.sql(), UserDao::map, where.params()).forEach(u -> out.put(u.getId(), u));
        }
        return out;
    }

    /** Admin user list: optional role filter and a search over name, username and email. */
    public List<User> search(Role role, String q) {
        Where where = new Where();
        if (role != null) {
            where.and("role = ?", role.key());
        }
        if (q != null && !q.isBlank()) {
            String like = "%" + q.toLowerCase(Locale.ROOT) + "%";
            where.and("(LOWER(username) LIKE ? OR LOWER(name) LIKE ? OR LOWER(email) LIKE ?)", like, like, like);
        }
        return db.query(SELECT + where.sql() + " ORDER BY id", UserDao::map, where.params());
    }

    /** Everyone except {@code userId} who is not blocked, staff first. */
    public List<User> listActiveExcept(int userId) {
        return db.query(SELECT + " WHERE id <> ? AND is_blocked = FALSE ORDER BY role, name", UserDao::map, userId);
    }

    public Map<String, Integer> countByRole() {
        return countsByString("SELECT role, COUNT(*) FROM users GROUP BY role");
    }

    public long count() {
        return db.count("SELECT COUNT(*) FROM users");
    }

    public long countByRole(Role role) {
        return db.count("SELECT COUNT(*) FROM users WHERE role = ?", role.key());
    }

    public List<LocalDateTime> createdSince(LocalDateTime since) {
        return db.query("SELECT created_at FROM users WHERE created_at >= ?", rs -> rs.getObject(1, LocalDateTime.class), since);
    }

    public User insert(User user) {
        if (user.getCreatedAt() == null) {
            user.setCreatedAt(Times.now());
        }
        int id = db.insert(
                "INSERT INTO users (username, name, email, bio, password_hash, role, is_blocked, created_at)"
                        + " VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                user.getUsername(), user.getName(), user.getEmail(), user.getBio(), user.getPasswordHash(),
                user.getRole().key(), user.isBlocked(), user.getCreatedAt());
        user.setId(id);
        return user;
    }

    /** Saves every editable column (the username never changes). */
    public void update(User user) {
        db.update("UPDATE users SET name = ?, email = ?, bio = ?, password_hash = ?, role = ?, is_blocked = ? WHERE id = ?",
                user.getName(), user.getEmail(), user.getBio(), user.getPasswordHash(), user.getRole().key(),
                user.isBlocked(), user.getId());
    }

    public void touchLastSeen(int id, LocalDateTime when) {
        db.update("UPDATE users SET last_seen_at = ? WHERE id = ?", when, id);
    }

    /**
     * Deletes the account. Foreign keys do the clean-up: the user's submissions, messages and lesson
     * progress are removed (ON DELETE CASCADE) and content they wrote or reviewed is kept without an
     * author (ON DELETE SET NULL).
     */
    public void delete(int id) {
        db.update("DELETE FROM users WHERE id = ?", id);
    }
}
