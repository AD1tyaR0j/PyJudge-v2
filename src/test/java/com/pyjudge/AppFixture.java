package com.pyjudge;

import com.pyjudge.config.AppConfig;
import com.pyjudge.model.ContentStatus;
import com.pyjudge.model.Difficulty;
import com.pyjudge.model.Problem;
import com.pyjudge.model.Role;
import com.pyjudge.model.Submission;
import com.pyjudge.model.TestCase;
import com.pyjudge.model.User;
import com.pyjudge.model.Verdict;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Builds the whole application around a private database for one test.
 * By default that is an in-memory H2 database in MySQL mode; set PYJUDGE_TEST_DB_URL
 * (plus _USER/_PASSWORD) to run the same tests against a real MySQL or MariaDB schema,
 * which is emptied before each test.
 */
public final class AppFixture {
    public static final String PASSWORD = "pass12345";
    private static final AtomicInteger COUNTER = new AtomicInteger();
    private static final List<String> TABLES = List.of("lesson_progress", "quiz_questions", "lessons", "topics",
            "challenge_problems", "challenges", "messages", "submissions", "test_cases", "problems", "settings", "users");

    private AppFixture() {
    }

    public static AppContext start() {
        Map<String, String> settings = new HashMap<>();
        String external = System.getenv("PYJUDGE_TEST_DB_URL");
        if (external != null && !external.isBlank()) {
            settings.put("db.url", external);
            settings.put("db.user", System.getenv().getOrDefault("PYJUDGE_TEST_DB_USER", "root"));
            settings.put("db.password", System.getenv().getOrDefault("PYJUDGE_TEST_DB_PASSWORD", ""));
        } else {
            settings.put("db.url", "jdbc:h2:mem:pyjudge_test_" + COUNTER.incrementAndGet()
                    + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
            settings.put("db.user", "sa");
            settings.put("db.password", "");
        }
        settings.put("db.pool.size", "4");
        settings.put("seed.onStartup", "false");
        settings.put("judge.sync", "true");
        AppContext app = AppContext.start(AppConfig.of(settings));
        if (external != null && !external.isBlank()) {
            wipe(app);
        }
        return app;
    }

    private static void wipe(AppContext app) {
        app.db().execute(connection -> {
            try (Statement st = connection.createStatement()) {
                for (String table : TABLES) {
                    st.executeUpdate("DELETE FROM " + table);
                }
            }
            return null;
        });
    }

    public static User user(AppContext app, String username, Role role) {
        return app.accounts().create(username, username.substring(0, 1).toUpperCase() + username.substring(1),
                username + "@example.com", PASSWORD, role);
    }

    /** An approved "print a + b" exercise with one sample and one hidden test. */
    public static Problem sumProblem(AppContext app, User author) {
        Problem p = new Problem();
        p.setTitle("Sum " + COUNTER.incrementAndGet());
        p.setSlug(app.problems().uniqueSlug(p.getTitle(), null));
        p.setDescription("Add two numbers.");
        p.setDifficulty(Difficulty.EASY);
        p.setStatus(ContentStatus.APPROVED);
        p.setCreatedBy(author == null ? null : author.getId());
        app.problems().insert(p);
        app.testCases().insertAll(p.getId(), List.of(new TestCase("1 2\n", "3\n", true), new TestCase("5 7\n", "12\n", false)));
        return p;
    }

    public static final String SUM_SOLUTION = "a, b = map(int, input().split())\nprint(a + b)\n";

    /** Stores a submission and judges it immediately (the test app judges synchronously). */
    public static Submission submit(AppContext app, User user, Problem problem, String code) {
        Submission s = new Submission();
        s.setUserId(user.getId());
        s.setProblemId(problem.getId());
        s.setCode(code);
        s.setVerdict(Verdict.PENDING);
        s.setTotal(app.testCases().count(problem.getId()));
        app.submissions().insert(s);
        app.judge().dispatch(s.getId());
        return app.submissions().findById(s.getId()).orElseThrow();
    }
}
