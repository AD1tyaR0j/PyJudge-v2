package com.pyjudge.seed;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.pyjudge.config.AppConfig;
import com.pyjudge.curriculum.CurriculumSync;
import com.pyjudge.dao.ChallengeDao;
import com.pyjudge.dao.ProblemDao;
import com.pyjudge.dao.TestCaseDao;
import com.pyjudge.dao.UserDao;
import com.pyjudge.db.Database;
import com.pyjudge.model.Challenge;
import com.pyjudge.model.ContentStatus;
import com.pyjudge.model.Difficulty;
import com.pyjudge.model.Problem;
import com.pyjudge.model.Role;
import com.pyjudge.model.TestCase;
import com.pyjudge.service.AccountService;
import com.pyjudge.util.Times;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

/**
 * First-run data: the starter exercise bank, the Python course, a sample challenge and the
 * bootstrap accounts. Every step skips what already exists, so it is safe to run on every start.
 */
public class Seeder {
    private static final Logger LOG = Logger.getLogger(Seeder.class.getName());
    private static final String BANK = "/seed/problems.json.gz";

    /** One test case in the exercise bank. */
    public static final class SeedCase {
        public String input;
        public String output;
    }

    /** One exercise in the exercise bank, with its expected outputs already worked out. */
    public static final class SeedProblem {
        public String title;
        public String difficulty;
        public String tags;
        public String description;
        public int timeLimit = 2;
        public String starter;
        /** A reference solution in Python; the tests run it through the judge. May be null. */
        public String solution;
        public List<SeedCase> samples = new ArrayList<>();
        public List<SeedCase> hidden = new ArrayList<>();

        public List<TestCase> testCases() {
            List<TestCase> cases = new ArrayList<>();
            samples.forEach(c -> cases.add(new TestCase(c.input, c.output, true)));
            hidden.forEach(c -> cases.add(new TestCase(c.input, c.output, false)));
            return cases;
        }
    }

    private final AppConfig config;
    private final Database db;
    private final UserDao users;
    private final ProblemDao problems;
    private final TestCaseDao testCases;
    private final ChallengeDao challenges;
    private final AccountService accounts;
    private final CurriculumSync curriculum;

    public Seeder(AppConfig config, Database db, UserDao users, ProblemDao problems, TestCaseDao testCases,
                  ChallengeDao challenges, AccountService accounts, CurriculumSync curriculum) {
        this.config = config;
        this.db = db;
        this.users = users;
        this.problems = problems;
        this.testCases = testCases;
        this.challenges = challenges;
        this.accounts = accounts;
        this.curriculum = curriculum;
    }

    /** Everything a fresh installation needs. */
    public void run() {
        bootstrapAccounts();
        int added = seedProblems();
        CurriculumSync.Result course = curriculum.sync();
        seedChallenge();
        LOG.info(() -> "Seed: " + added + " exercise(s) added; course: " + course.topicsCreated() + " new topic(s), "
                + course.lessonsCreated() + " new lesson(s), " + course.lessonsUpdated() + " refreshed.");
    }

    /**
     * Streams the bundled exercise bank one exercise at a time (it holds megabytes of test data,
     * so it is never loaded as a whole).
     */
    public static void forEachProblem(Consumer<SeedProblem> action) {
        Gson gson = new Gson();
        try (InputStream raw = Seeder.class.getResourceAsStream(BANK)) {
            if (raw == null) {
                throw new IllegalStateException("Missing resource " + BANK);
            }
            try (JsonReader reader = new JsonReader(new InputStreamReader(new GZIPInputStream(raw), StandardCharsets.UTF_8))) {
                reader.beginArray();
                while (reader.hasNext()) {
                    SeedProblem problem = gson.fromJson(reader, SeedProblem.class);
                    action.accept(problem);
                }
                reader.endArray();
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + BANK, e);
        }
    }

    /** Adds every bank exercise whose title is not in the database yet. Returns how many were added. */
    public int seedProblems() {
        int[] created = {0};
        forEachProblem(spec -> {
            if (problems.existsByTitle(spec.title)) {
                return;
            }
            // One transaction per exercise: it appears with all of its test cases or not at all
            db.inTransaction(() -> {
                Problem p = new Problem();
                p.setTitle(spec.title);
                p.setSlug(problems.uniqueSlug(spec.title, null));
                p.setDescription(spec.description);
                p.setDifficulty(Difficulty.fromLabel(spec.difficulty));
                p.setTags(spec.tags);
                p.setTimeLimit(spec.timeLimit);
                p.setStatus(ContentStatus.APPROVED);
                p.setStarterCode(spec.starter == null ? Problem.DEFAULT_STARTER : spec.starter);
                problems.insert(p);
                testCases.insertAll(p.getId(), spec.testCases());
            });
            created[0]++;
        });
        return created[0];
    }

    /** A month-long challenge over the first five Easy exercises, so the feature is visible on day one. */
    public void seedChallenge() {
        if (challenges.count() > 0) {
            return;
        }
        List<Problem> starters = problems.listApproved(Difficulty.EASY, 5);
        if (starters.isEmpty()) {
            return;
        }
        LocalDateTime now = Times.now().withMinute(0).withSecond(0).withNano(0);
        Challenge c = new Challenge();
        c.setTitle("Python Starter Sprint");
        c.setDescription("Solve all five warm-up exercises within the challenge window to complete it.");
        c.setStartAt(now.minusDays(1));
        c.setEndAt(now.plusDays(30));
        c.setProblems(starters);
        challenges.insert(c);
    }

    /**
     * Creates the first admin (there is no other way to get one from the browser) and, optionally,
     * a demo instructor and student. Does nothing once an admin exists.
     */
    public void bootstrapAccounts() {
        if (users.countByRole(Role.ADMIN) > 0) {
            return;
        }
        String username = config.get("bootstrap.admin.username");
        String password = config.get("bootstrap.admin.password");
        if (username.isEmpty() || password.isEmpty()) {
            return;
        }
        createIfFree(username, "Administrator", config.get("bootstrap.admin.email", username + "@pyjudge.local"),
                password, Role.ADMIN);
        if (config.getBoolean("bootstrap.demoAccounts", false)) {
            createIfFree("instructor", "Demo Instructor", "instructor@pyjudge.local", "instructor12345", Role.INSTRUCTOR);
            createIfFree("student", "Demo Student", "student@pyjudge.local", "student12345", Role.STUDENT);
        }
    }

    private void createIfFree(String username, String name, String email, String password, Role role) {
        List<String> errors = accounts.validate(username, name, email, password, null, null);
        if (errors.isEmpty()) {
            accounts.create(username, name, email, password, role);
            LOG.info(() -> "Created " + role.key() + " account '" + username + "'. Change its password after logging in.");
        } else {
            LOG.warning(() -> "Could not create the " + role.key() + " account '" + username + "': " + errors);
        }
    }
}
