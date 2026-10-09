package com.pyjudge.dao;

import com.pyjudge.db.Database;
import com.pyjudge.model.Role;
import com.pyjudge.model.User;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Shared plumbing for the data-access objects: result-set helpers and a small WHERE-clause builder. */
abstract class BaseDao {
    protected final Database db;

    protected BaseDao(Database db) {
        this.db = db;
    }

    protected static LocalDateTime timestamp(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, LocalDateTime.class);
    }

    protected static Integer intOrNull(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    /** SELECT fragment for a joined user: {@code a.id AS author_id, a.username AS author_username, ...}. */
    protected static String userColumns(String alias, String prefix) {
        return alias + ".id AS " + prefix + "_id, " + alias + ".username AS " + prefix + "_username, "
                + alias + ".name AS " + prefix + "_name, " + alias + ".role AS " + prefix + "_role";
    }

    /** Reads the columns written by {@link #userColumns}; null when the LEFT JOIN found nobody. */
    protected static User joinedUser(ResultSet rs, String prefix) throws SQLException {
        int id = rs.getInt(prefix + "_id");
        if (rs.wasNull()) {
            return null;
        }
        User user = new User();
        user.setId(id);
        user.setUsername(rs.getString(prefix + "_username"));
        user.setName(rs.getString(prefix + "_name"));
        user.setRole(Role.fromKey(rs.getString(prefix + "_role")));
        return user;
    }

    /** Result of a {@code SELECT key, COUNT(*) ... GROUP BY key} query. */
    protected Map<Integer, Integer> countsByInt(String sql, Object... params) {
        Map<Integer, Integer> out = new LinkedHashMap<>();
        db.query(sql, rs -> out.put(rs.getInt(1), rs.getInt(2)), params);
        return out;
    }

    protected Map<String, Integer> countsByString(String sql, Object... params) {
        Map<String, Integer> out = new LinkedHashMap<>();
        db.query(sql, rs -> out.put(rs.getString(1), rs.getInt(2)), params);
        return out;
    }

    protected Set<Integer> intSet(String sql, Object... params) {
        return new LinkedHashSet<>(db.query(sql, rs -> rs.getInt(1), params));
    }

    /** Collects optional filters and their parameters, then renders {@code " WHERE a AND b"}. */
    protected static final class Where {
        private final List<String> clauses = new ArrayList<>();
        private final List<Object> params = new ArrayList<>();

        Where and(String clause, Object... values) {
            clauses.add(clause);
            Collections.addAll(params, values);
            return this;
        }

        /** {@code column IN (?, ?, ...)}; an empty collection matches nothing. */
        Where andIn(String column, Collection<?> values) {
            if (values.isEmpty()) {
                clauses.add("1 = 0");
            } else {
                clauses.add(column + " IN (" + Database.placeholders(values.size()) + ")");
                params.addAll(values);
            }
            return this;
        }

        String sql() {
            return clauses.isEmpty() ? "" : " WHERE " + String.join(" AND ", clauses);
        }

        Object[] params(Object... extra) {
            List<Object> all = new ArrayList<>(params);
            Collections.addAll(all, extra);
            return all.toArray();
        }
    }
}
