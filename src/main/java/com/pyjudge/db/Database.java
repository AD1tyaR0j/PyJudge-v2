package com.pyjudge.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * JDBC access for the whole application: a small connection pool, transactions, and
 * helpers that run a {@link PreparedStatement} and map its {@link ResultSet}.
 *
 * <p>Every statement is parameterised, so user input never becomes part of the SQL text.
 * Inside {@link #inTransaction(Supplier)} all helpers share one connection (bound to the
 * current thread), which lets several DAO calls commit or roll back together.
 */
public final class Database implements AutoCloseable {
    private static final int BORROW_TIMEOUT_SECONDS = 10;

    private final String url;
    private final String user;
    private final String password;
    private final int maxSize;
    private final BlockingQueue<Connection> idle;
    private final AtomicInteger created = new AtomicInteger();
    private final ThreadLocal<Connection> bound = new ThreadLocal<>();
    private volatile boolean closed;

    public Database(String url, String user, String password, int poolSize) {
        this.url = url;
        this.user = user;
        this.password = password;
        this.maxSize = Math.max(1, poolSize);
        this.idle = new ArrayBlockingQueue<>(this.maxSize);
    }

    public String getUrl() {
        return url;
    }

    // ------------------------------------------------------------ connection pool

    private Connection borrow() throws SQLException {
        if (closed) {
            throw new DaoException("The database connection pool is closed.");
        }
        while (true) {
            Connection connection = idle.poll();
            if (connection == null) {
                if (created.incrementAndGet() <= maxSize) {
                    try {
                        return DriverManager.getConnection(url, user, password);
                    } catch (SQLException e) {
                        created.decrementAndGet();
                        throw e;
                    }
                }
                created.decrementAndGet();
                try {
                    connection = idle.poll(BORROW_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new DaoException("Interrupted while waiting for a database connection.", e);
                }
                if (connection == null) {
                    throw new DaoException("All " + maxSize + " database connections are busy. Try again shortly.");
                }
            }
            if (isUsable(connection)) {
                return connection;
            }
            discard(connection); // stale (e.g. the server closed it): drop it and try the next one
        }
    }

    private static boolean isUsable(Connection connection) {
        try {
            return connection.isValid(2);
        } catch (SQLException e) {
            return false;
        }
    }

    private void release(Connection connection) {
        try {
            if (!connection.getAutoCommit()) {
                connection.rollback();
                connection.setAutoCommit(true);
            }
            if (closed || !idle.offer(connection)) {
                discard(connection);
            }
        } catch (SQLException e) {
            discard(connection);
        }
    }

    private void discard(Connection connection) {
        created.decrementAndGet();
        try {
            connection.close();
        } catch (SQLException ignored) {
            // already unusable
        }
    }

    @Override
    public void close() {
        closed = true;
        List<Connection> open = new ArrayList<>();
        idle.drainTo(open);
        open.forEach(this::discard);
    }

    // ------------------------------------------------------------ transactions

    /** A unit of JDBC work that may throw {@link SQLException}. */
    @FunctionalInterface
    public interface SqlWork<T> {
        T run(Connection connection) throws SQLException;
    }

    /** Runs {@code work} on the transaction's connection if there is one, otherwise on a pooled connection. */
    public <T> T execute(SqlWork<T> work) {
        Connection current = bound.get();
        if (current != null) {
            try {
                return work.run(current);
            } catch (SQLException e) {
                throw new DaoException(e.getMessage(), e);
            }
        }
        Connection connection = null;
        try {
            connection = borrow();
            return work.run(connection);
        } catch (SQLException e) {
            throw new DaoException(e.getMessage(), e);
        } finally {
            if (connection != null) {
                release(connection);
            }
        }
    }

    /**
     * Runs {@code work} atomically: everything it does through this class is committed together,
     * or rolled back if it throws. Nested calls join the outer transaction.
     */
    public <T> T inTransaction(Supplier<T> work) {
        if (bound.get() != null) {
            return work.get();
        }
        Connection connection = null;
        try {
            connection = borrow();
            connection.setAutoCommit(false);
            bound.set(connection);
            T result = work.get();
            connection.commit();
            return result;
        } catch (SQLException e) {
            rollbackQuietly(connection);
            throw new DaoException(e.getMessage(), e);
        } catch (RuntimeException e) {
            rollbackQuietly(connection);
            throw e;
        } finally {
            bound.remove();
            if (connection != null) {
                release(connection);
            }
        }
    }

    public void inTransaction(Runnable work) {
        inTransaction(() -> {
            work.run();
            return null;
        });
    }

    private static void rollbackQuietly(Connection connection) {
        if (connection != null) {
            try {
                connection.rollback();
            } catch (SQLException ignored) {
                // the original failure is the one worth reporting
            }
        }
    }

    // ------------------------------------------------------------ query helpers

    public <T> List<T> query(String sql, RowMapper<T> mapper, Object... params) {
        return execute(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                bind(ps, params);
                try (ResultSet rs = ps.executeQuery()) {
                    List<T> rows = new ArrayList<>();
                    while (rs.next()) {
                        rows.add(mapper.map(rs));
                    }
                    return rows;
                }
            }
        });
    }

    public <T> Optional<T> queryOne(String sql, RowMapper<T> mapper, Object... params) {
        List<T> rows = query(sql, mapper, params);
        return rows.isEmpty() ? Optional.empty() : Optional.ofNullable(rows.get(0));
    }

    /** First column of the first row as a number, or 0 when there is no row or the value is NULL. */
    public long count(String sql, Object... params) {
        return queryOne(sql, rs -> rs.getLong(1), params).orElse(0L);
    }

    /** Runs an INSERT, UPDATE or DELETE and returns the number of affected rows. */
    public int update(String sql, Object... params) {
        return execute(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                bind(ps, params);
                return ps.executeUpdate();
            }
        });
    }

    /** Runs an INSERT and returns the generated AUTO_INCREMENT key. */
    public int insert(String sql, Object... params) {
        return execute(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                bind(ps, params);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) {
                        throw new SQLException("The INSERT did not return a generated key.");
                    }
                    return keys.getInt(1);
                }
            }
        });
    }

    /** Runs the same statement once per parameter row, as a single JDBC batch. */
    public void batch(String sql, List<Object[]> rows) {
        if (rows.isEmpty()) {
            return;
        }
        execute(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                for (Object[] row : rows) {
                    bind(ps, row);
                    ps.addBatch();
                }
                ps.executeBatch();
                return null;
            }
        });
    }

    private static void bind(PreparedStatement ps, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            Object value = params[i];
            if (value == null) {
                ps.setNull(i + 1, Types.NULL);
            } else if (value instanceof Enum<?>) {
                // Enums are stored by their database key (see Role, Difficulty...), never by accident
                throw new IllegalArgumentException("Bind the enum's database key, not the enum: " + value);
            } else {
                ps.setObject(i + 1, value);
            }
        }
    }

    // ------------------------------------------------------------ SQL building helpers

    /** {@code "?, ?, ?"} for an IN (...) list of {@code n} values. */
    public static String placeholders(int n) {
        return String.join(", ", Collections.nCopies(n, "?"));
    }
}
