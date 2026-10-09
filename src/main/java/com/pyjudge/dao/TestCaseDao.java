package com.pyjudge.dao;

import com.pyjudge.db.Database;
import com.pyjudge.model.TestCase;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class TestCaseDao extends BaseDao {
    private static final String SELECT = "SELECT id, problem_id, input, expected_output, is_sample FROM test_cases";

    public TestCaseDao(Database db) {
        super(db);
    }

    private static TestCase map(ResultSet rs) throws SQLException {
        TestCase tc = new TestCase();
        tc.setId(rs.getInt("id"));
        tc.setProblemId(rs.getInt("problem_id"));
        tc.setInput(rs.getString("input"));
        tc.setExpectedOutput(rs.getString("expected_output"));
        tc.setSample(rs.getBoolean("is_sample"));
        return tc;
    }

    public Optional<TestCase> findById(int id) {
        return db.queryOne(SELECT + " WHERE id = ?", TestCaseDao::map, id);
    }

    /** Every case in judging order. Hidden cases can be megabytes, so only the judge should call this. */
    public List<TestCase> listByProblem(int problemId) {
        return db.query(SELECT + " WHERE problem_id = ? ORDER BY id", TestCaseDao::map, problemId);
    }

    /** The cases shown on the exercise page. */
    public List<TestCase> listSamples(int problemId) {
        return db.query(SELECT + " WHERE problem_id = ? AND is_sample = TRUE ORDER BY id", TestCaseDao::map, problemId);
    }

    /** Every case with input and output cut to {@code maxChars}, for the exercise editor. */
    public List<TestCase> listPreviews(int problemId, int maxChars) {
        return db.query("SELECT id, problem_id, SUBSTRING(input, 1, ?) AS input,"
                        + " SUBSTRING(expected_output, 1, ?) AS expected_output, is_sample"
                        + " FROM test_cases WHERE problem_id = ? ORDER BY id",
                TestCaseDao::map, maxChars, maxChars, problemId);
    }

    public int count(int problemId) {
        return (int) db.count("SELECT COUNT(*) FROM test_cases WHERE problem_id = ?", problemId);
    }

    public int countSamples(int problemId) {
        return (int) db.count("SELECT COUNT(*) FROM test_cases WHERE problem_id = ? AND is_sample = TRUE", problemId);
    }

    /** {problemId: number of test cases} for the given exercises (all exercises when {@code ids} is null). */
    public Map<Integer, Integer> countsByProblem(Collection<Integer> ids) {
        Where where = new Where();
        if (ids != null) {
            where.andIn("problem_id", ids);
        }
        return countsByInt("SELECT problem_id, COUNT(*) FROM test_cases" + where.sql() + " GROUP BY problem_id",
                where.params());
    }

    public TestCase insert(TestCase tc) {
        int id = db.insert("INSERT INTO test_cases (problem_id, input, expected_output, is_sample) VALUES (?, ?, ?, ?)",
                tc.getProblemId(), tc.getInput(), tc.getExpectedOutput(), tc.isSample());
        tc.setId(id);
        return tc;
    }

    /** Adds all cases of one exercise in a single JDBC batch. */
    public void insertAll(int problemId, List<TestCase> cases) {
        List<Object[]> rows = new ArrayList<>();
        for (TestCase tc : cases) {
            rows.add(new Object[] {problemId, tc.getInput(), tc.getExpectedOutput(), tc.isSample()});
        }
        db.batch("INSERT INTO test_cases (problem_id, input, expected_output, is_sample) VALUES (?, ?, ?, ?)", rows);
    }

    public void delete(int id) {
        db.update("DELETE FROM test_cases WHERE id = ?", id);
    }
}
