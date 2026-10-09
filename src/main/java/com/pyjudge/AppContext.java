package com.pyjudge;

import com.pyjudge.config.AppConfig;
import com.pyjudge.curriculum.CurriculumSync;
import com.pyjudge.dao.ChallengeDao;
import com.pyjudge.dao.LessonDao;
import com.pyjudge.dao.MessageDao;
import com.pyjudge.dao.ProblemDao;
import com.pyjudge.dao.ProgressDao;
import com.pyjudge.dao.QuizDao;
import com.pyjudge.dao.SettingDao;
import com.pyjudge.dao.SubmissionDao;
import com.pyjudge.dao.TestCaseDao;
import com.pyjudge.dao.TopicDao;
import com.pyjudge.dao.UserDao;
import com.pyjudge.db.Database;
import com.pyjudge.db.SchemaInitializer;
import com.pyjudge.judge.DockerSandbox;
import com.pyjudge.judge.Judge;
import com.pyjudge.judge.JudgeService;
import com.pyjudge.judge.ProcessSandbox;
import com.pyjudge.judge.Sandbox;
import com.pyjudge.seed.Seeder;
import com.pyjudge.service.AccountService;
import com.pyjudge.service.AchievementService;
import com.pyjudge.service.ChallengeService;
import com.pyjudge.service.CourseService;
import com.pyjudge.service.MarkdownService;
import com.pyjudge.service.MessageService;
import com.pyjudge.service.ReviewService;
import com.pyjudge.service.SettingsService;
import com.pyjudge.service.StatsService;
import com.pyjudge.service.StudioService;

/**
 * The application's object graph, built once at start-up and shared by every servlet.
 * Wiring by hand keeps the dependencies explicit: each class receives what it needs through
 * its constructor, which is also how the tests assemble the app around an in-memory database.
 */
public final class AppContext implements AutoCloseable {
    /** ServletContext attribute under which the instance is stored. */
    public static final String ATTRIBUTE = "pyjudge.app";

    private final AppConfig config;
    private final Database db;

    private final UserDao users;
    private final ProblemDao problems;
    private final TestCaseDao testCases;
    private final SubmissionDao submissions;
    private final MessageDao messages;
    private final ChallengeDao challenges;
    private final TopicDao topics;
    private final LessonDao lessons;
    private final QuizDao quizzes;
    private final ProgressDao progress;

    private final SettingsService settings;
    private final AccountService accounts;
    private final MarkdownService markdown;
    private final StatsService stats;
    private final CourseService course;
    private final ChallengeService challengeService;
    private final MessageService messaging;
    private final StudioService studio;
    private final ReviewService reviews;
    private final AchievementService achievements;

    private final Sandbox sandbox;
    private final JudgeService judge;
    private final CurriculumSync curriculum;
    private final Seeder seeder;

    private AppContext(AppConfig config) {
        this.config = config;
        this.db = new Database(config.get("db.url"), config.get("db.user"), config.get("db.password"),
                config.getInt("db.pool.size", 8));
        SchemaInitializer.run(db);

        users = new UserDao(db);
        problems = new ProblemDao(db);
        testCases = new TestCaseDao(db);
        submissions = new SubmissionDao(db);
        messages = new MessageDao(db);
        challenges = new ChallengeDao(db);
        topics = new TopicDao(db);
        lessons = new LessonDao(db);
        quizzes = new QuizDao(db);
        progress = new ProgressDao(db);

        settings = new SettingsService(new SettingDao(db));
        accounts = new AccountService(users);
        markdown = new MarkdownService();
        stats = new StatsService(submissions, users, progress);
        course = new CourseService(topics, lessons, quizzes, progress, problems, markdown);
        challengeService = new ChallengeService(challenges, problems, submissions);
        messaging = new MessageService(messages, users, submissions, settings);
        studio = new StudioService(problems, testCases, lessons, topics, quizzes, settings);
        reviews = new ReviewService(db, problems, testCases, lessons, submissions, messages);
        achievements = new AchievementService(course, stats, challengeService, submissions, problems);

        // Polymorphism: the rest of the app only knows the Sandbox interface
        sandbox = "docker".equalsIgnoreCase(config.get("sandbox.backend"))
                ? new DockerSandbox(config.get("sandbox.docker.image", "python:3.11-slim"))
                : new ProcessSandbox(config.get("python.executable"));
        judge = new JudgeService(new Judge(sandbox), submissions, problems, testCases,
                config.getInt("judge.workers", 4), config.getBoolean("judge.sync", false));

        curriculum = new CurriculumSync(db, topics, lessons, quizzes);
        seeder = new Seeder(config, db, users, problems, testCases, challenges, accounts, curriculum);
    }

    /** Connects to the database, creates missing tables and (when configured) loads the first-run data. */
    public static AppContext start(AppConfig config) {
        AppContext app = new AppContext(config);
        if (config.getBoolean("seed.onStartup", true)) {
            app.seeder.run();
        }
        return app;
    }

    @Override
    public void close() {
        judge.close();
        db.close();
    }

    public AppConfig config() { return config; }
    public Database db() { return db; }
    public UserDao users() { return users; }
    public ProblemDao problems() { return problems; }
    public TestCaseDao testCases() { return testCases; }
    public SubmissionDao submissions() { return submissions; }
    public MessageDao messages() { return messages; }
    public ChallengeDao challenges() { return challenges; }
    public TopicDao topics() { return topics; }
    public LessonDao lessons() { return lessons; }
    public QuizDao quizzes() { return quizzes; }
    public ProgressDao progress() { return progress; }
    public SettingsService settings() { return settings; }
    public AccountService accounts() { return accounts; }
    public MarkdownService markdown() { return markdown; }
    public StatsService stats() { return stats; }
    public CourseService course() { return course; }
    public ChallengeService challengeService() { return challengeService; }
    public MessageService messaging() { return messaging; }
    public StudioService studio() { return studio; }
    public ReviewService reviews() { return reviews; }
    public AchievementService achievements() { return achievements; }
    public Sandbox sandbox() { return sandbox; }
    public JudgeService judge() { return judge; }
    public CurriculumSync curriculum() { return curriculum; }
    public Seeder seeder() { return seeder; }
}
