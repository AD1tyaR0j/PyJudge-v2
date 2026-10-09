package com.pyjudge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pyjudge.config.AppConfig;
import com.pyjudge.curriculum.CurriculumException;
import com.pyjudge.curriculum.CurriculumLoader;
import com.pyjudge.curriculum.CurriculumParser;
import com.pyjudge.curriculum.CurriculumParser.LessonSpec;
import com.pyjudge.curriculum.CurriculumParser.Playground;
import com.pyjudge.curriculum.CurriculumParser.TopicSpec;
import com.pyjudge.curriculum.CurriculumSync;
import com.pyjudge.judge.ProcessSandbox;
import com.pyjudge.judge.RunResult;
import com.pyjudge.judge.RunStatus;
import com.pyjudge.judge.Sandbox;
import com.pyjudge.model.ContentStatus;
import com.pyjudge.model.Lesson;
import com.pyjudge.model.LessonProgress;
import com.pyjudge.model.QuizQuestion;
import com.pyjudge.model.Role;
import com.pyjudge.model.User;
import com.pyjudge.service.AchievementService;
import com.pyjudge.service.CourseOverview;
import com.pyjudge.service.CourseService.QuizOutcome;
import com.pyjudge.service.Form;
import com.pyjudge.service.MarkdownService;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

/** The built-in Python course: its files, its rendering, and student progress through it. */
class CourseTest {
    private static List<TopicSpec> course;
    private AppContext app;

    @BeforeAll
    static void loadCourse() {
        course = CurriculumLoader.load();
    }

    @AfterEach
    void tearDown() {
        if (app != null) {
            app.close();
        }
    }

    private AppContext startWithCourse() {
        app = AppFixture.start();
        app.curriculum().sync(course);
        return app;
    }

    // ------------------------------------------------------------ the course files

    @Test
    void courseShape() {
        assertEquals(11, course.size());
        assertEquals("getting-started", course.get(0).slug());
        int lessons = 0;
        for (TopicSpec topic : course) {
            assertFalse(topic.title().isBlank());
            assertFalse(topic.summary().isBlank(), topic.slug());
            assertTrue(topic.lessons().size() >= 2, topic.slug());
            for (LessonSpec lesson : topic.lessons()) {
                lessons++;
                assertTrue(lesson.content().length() > 300, lesson.slug());
                assertTrue(lesson.quiz().size() >= 2, lesson.slug() + " needs a quiz");
                assertFalse(lesson.practice().isBlank(), lesson.slug() + " needs practice tags");
            }
        }
        assertEquals(32, lessons);
    }

    /** Runs every playground in every lesson: clean examples must succeed, pyrun-error ones must fail. */
    @TestFactory
    Stream<DynamicTest> everyRunnableExampleWorks() {
        Sandbox sandbox = new ProcessSandbox("");
        List<DynamicTest> tests = new ArrayList<>();
        for (TopicSpec topic : course) {
            for (LessonSpec lesson : topic.lessons()) {
                List<Playground> blocks = CurriculumParser.playgrounds(lesson.content());
                for (int i = 0; i < blocks.size(); i++) {
                    Playground block = blocks.get(i);
                    tests.add(DynamicTest.dynamicTest(topic.slug() + "/" + lesson.slug() + " #" + (i + 1), () -> {
                        RunResult result = sandbox.run(block.code(), block.stdin(), 5, 128, AppConfig.MAX_OUTPUT_BYTES);
                        if (block.expectsError()) {
                            assertEquals(RunStatus.RE, result.status(), "expected an error:\n" + block.code());
                        } else {
                            assertEquals(RunStatus.OK, result.status(), block.code() + "\n" + result.stderr());
                        }
                    }));
                }
            }
        }
        assertTrue(tests.size() > 60, "the course should be full of runnable examples");
        return tests.stream();
    }

    @Test
    void quizParserRejectsBadQuestions() {
        assertThrows(CurriculumException.class, () -> CurriculumParser.parseQuiz("Q: One option?\n- [x] only\n", "t"));
        assertThrows(CurriculumException.class, () -> CurriculumParser.parseQuiz("Q: No answer?\n- [ ] a\n- [ ] b\n", "t"));
        assertThrows(CurriculumException.class, () -> CurriculumParser.parseQuiz("Q: Two?\n- [x] a\n- [x] b\n", "t"));
        var parsed = CurriculumParser.parseQuiz("Q: What prints?\n```python\nprint(1)\n```\n- [ ] 0\n- [x] 1\n> Because.\n> Really.\n", "t");
        assertEquals(1, parsed.size());
        assertEquals(1, parsed.get(0).answer());
        assertTrue(parsed.get(0).prompt().contains("print(1)"));
        assertEquals("Because. Really.", parsed.get(0).explanation());
        assertThrows(CurriculumException.class, () -> CurriculumParser.parseLesson("no front matter", "t", "x", 0));
    }

    @Test
    void renderTurnsPyrunIntoPlaygroundAndEscapes() {
        MarkdownService md = new MarkdownService();
        String html = md.renderLesson("# Title\n\nSome *text* with `code`.\n\n```pyrun\nprint('<b>hi</b>')\n### stdin\n5\n```\n\n"
                + "```python\nx = 1 < 2\n```\n\n| a | b |\n|---|---|\n| 1 | 2 |\n\n<script>alert(1)</script>\n");
        assertTrue(html.contains("<h1>Title</h1>"), html);
        assertTrue(html.contains("<em>text</em>"));
        assertTrue(html.contains("class=\"playground\" data-expect-error=\"0\""), html);
        assertTrue(html.contains("print(&#39;&lt;b&gt;hi&lt;/b&gt;&#39;)"), html);
        assertTrue(html.contains("<details class=\"pg-input\" open>"), html);
        assertTrue(html.contains("class=\"language-python\""), html);
        assertTrue(html.contains("<table>"), html);
        assertFalse(html.contains("<script>"), "raw HTML from authors is escaped");
        assertFalse(html.contains("PYJUDGEPLAYGROUND"));
        assertTrue(md.renderLesson("```pyrun-error\n1/0\n```").contains("data-expect-error=\"1\""));

        assertEquals("a <code>b</code>", md.renderInline("a `b`"));
        assertEquals("", md.renderInline(null));
    }

    // ------------------------------------------------------------ the course in the database

    @Test
    void syncIsIdempotentAndKeepsCustomLessons() {
        app = AppFixture.start();
        CurriculumSync.Result first = app.curriculum().sync(course);
        assertEquals(11, first.topicsCreated());
        assertEquals(32, first.lessonsCreated());

        User teacher = AppFixture.user(app, "teacher", Role.INSTRUCTOR);
        int topicId = app.topics().findBySlug("strings").orElseThrow().getId();
        Lesson custom = new Lesson();
        assertTrue(app.studio().createLesson(custom, Form.of("title", "Regex Basics", "content", "Body", "topic_id",
                String.valueOf(topicId), "minutes", "7", "practice_tags", "strings"), teacher).succeeded());

        CurriculumSync.Result second = app.curriculum().sync(course);
        assertEquals(0, second.topicsCreated());
        assertEquals(0, second.lessonsCreated());
        assertEquals(32, second.lessonsUpdated());
        assertEquals(11, app.topics().listAll().size());
        assertEquals(32, app.lessons().listApproved().size());
        Lesson kept = app.lessons().findById(custom.getId()).orElseThrow();
        assertEquals("Body", kept.getContent());
        assertEquals(ContentStatus.PENDING, kept.getStatus());

        Lesson builtin = app.lessons().listApproved().get(0);
        assertTrue(builtin.getQuestionCount() >= 2);
        assertEquals(builtin.getQuestionCount(), app.quizzes().listByLesson(builtin.getId()).size());
    }

    @Test
    void passingQuizCompletesLessonAndBestScoreIsKept() {
        startWithCourse();
        User student = AppFixture.user(app, "alice", Role.STUDENT);
        Lesson lesson = app.lessons().listApproved().get(0);
        List<QuizQuestion> questions = app.quizzes().listByLesson(lesson.getId());

        // All wrong: graded, recorded, not completed
        List<String> wrong = new ArrayList<>();
        List<String> right = new ArrayList<>();
        for (QuizQuestion q : questions) {
            wrong.add("q" + q.getId());
            wrong.add(String.valueOf((q.getAnswer() + 1) % q.getOptions().size()));
            right.add("q" + q.getId());
            right.add(String.valueOf(q.getAnswer()));
        }
        QuizOutcome failed = app.course().gradeQuiz(student, lesson, Form.of(wrong.toArray(new String[0])));
        assertEquals(0, failed.score());
        assertFalse(failed.passed());
        assertFalse(failed.progress().isCompleted());
        assertEquals(questions.size(), failed.results().size());
        assertEquals(Boolean.FALSE, failed.results().get(0).get("correct"));

        QuizOutcome passed = app.course().gradeQuiz(student, lesson, Form.of(right.toArray(new String[0])));
        assertEquals(questions.size(), passed.score());
        assertTrue(passed.passed());
        assertTrue(passed.progress().isCompleted());
        assertTrue(passed.progress().isPerfect());

        // A later bad attempt does not lower the best score or undo completion
        QuizOutcome again = app.course().gradeQuiz(student, lesson, Form.of(wrong.toArray(new String[0])));
        LessonProgress saved = app.progress().find(student.getId(), lesson.getId()).orElseThrow();
        assertEquals(questions.size(), saved.getQuizScore());
        assertEquals(3, saved.getAttempts());
        assertTrue(again.progress().isCompleted());

        CourseOverview overview = app.course().overview(student);
        assertEquals(1, overview.getDone());
        assertEquals(32, overview.getTotal());
        assertEquals(100, overview.getQuizAvg());
        assertEquals(1, overview.getPerfectQuizzes());
        assertEquals("active", overview.getTopics().get(0).getState());
        assertEquals(app.lessons().listApproved().get(1).getId(), overview.getNext().getId());
        assertNotNull(overview.getNext().getTopic().getSummary());

        List<AchievementService.Badge> badges = app.achievements().forUser(student);
        assertEquals(9, badges.size());
        assertTrue(badges.get(0).isEarned(), "First Steps");
        assertEquals(1, AchievementService.earnedCount(badges));
        assertEquals(1, app.stats().streak(student.getId()));

        app.users().delete(student.getId());
        assertTrue(app.progress().find(student.getId(), lesson.getId()).isEmpty());
    }

    @Test
    void guestQuizIsGradedButNotSaved() {
        startWithCourse();
        Lesson lesson = app.lessons().listApproved().get(0);
        QuizOutcome outcome = app.course().gradeQuiz(null, lesson, Form.of());
        assertNull(outcome.progress());
        assertEquals(0, outcome.score());
        CourseOverview overview = app.course().overview(null);
        assertEquals(0, overview.getDone());
        assertNull(overview.getQuizAvg());
        assertEquals("new", overview.getTopics().get(0).getState());
    }

    @Test
    void neighboursAndPractice() {
        startWithCourse();
        List<Lesson> all = app.lessons().listApproved();
        assertNull(app.course().neighbours(all.get(0)).previous());
        assertEquals(all.get(1).getId(), app.course().neighbours(all.get(0)).next().getId());
        assertNull(app.course().neighbours(all.get(all.size() - 1)).next());

        app.seeder().seedProblems();
        Lesson lesson = all.get(0);
        var practice = app.course().practiceProblems(lesson, java.util.Set.of(), 4);
        assertFalse(practice.isEmpty(), "tags " + lesson.getPracticeTags() + " should match seeded exercises");
        assertTrue(practice.size() <= 4);
    }

    @Test
    void instructorLessonNeedsApprovalAndQuestionsAreValidated() {
        startWithCourse();
        User admin = AppFixture.user(app, "root", Role.ADMIN);
        User teacher = AppFixture.user(app, "teacher", Role.INSTRUCTOR);
        int topicId = app.topics().listAll().get(0).getId();

        Lesson bad = new Lesson();
        assertEquals(4, app.studio().createLesson(bad, Form.of("title", "", "content", " ", "minutes", "0"), teacher).errors().size());

        Lesson lesson = new Lesson();
        assertTrue(app.studio().createLesson(lesson, Form.of("title", "Extra Tips", "content", "Hello **there**",
                "topic_id", String.valueOf(topicId), "minutes", "4"), teacher).succeeded());
        assertEquals("extra-tips", lesson.getSlug());
        assertEquals(ContentStatus.PENDING, lesson.getStatus());
        assertTrue(lesson.getPosition() >= 1000);
        assertEquals(32, app.course().overview(null).getTotal(), "pending lessons are not part of the course");
        assertEquals(1, app.lessons().listPending().size());
        assertEquals(1, app.lessons().listByAuthor(teacher.getId()).size());
        assertEquals(33, app.lessons().listByAuthor(null).size());

        assertFalse(app.studio().addQuestion(lesson, Form.of("prompt", "Q?", "option", "only one", "option", "", "answer", "0")).succeeded());
        assertFalse(app.studio().addQuestion(lesson, Form.of("prompt", "Q?", "option", "a", "option", "", "option", "c", "answer", "1")).succeeded());
        assertTrue(app.studio().addQuestion(lesson, Form.of("prompt", "Q?", "option", "a", "option", "", "option", "c",
                "answer", "2", "explanation", "c is right")).succeeded());
        QuizQuestion stored = app.quizzes().listByLesson(lesson.getId()).get(0);
        assertEquals(List.of("a", "c"), stored.getOptions());
        assertEquals(1, stored.getAnswer(), "the answer index follows the dropped empty option");

        assertFalse(app.reviews().reviewLesson(lesson, admin, "reject", "").succeeded());
        assertTrue(app.reviews().reviewLesson(lesson, admin, "approve", "").succeeded());
        assertEquals(33, app.course().overview(null).getTotal());
        assertTrue(app.messages().recentReceived(teacher.getId(), 1).get(0).getBody().contains("lesson “Extra Tips” was approved"));

        // Instructors may edit their own custom lessons but never the built-in course
        Lesson builtin = app.lessons().listApproved().get(0);
        assertFalse(builtin.editableBy(teacher));
        assertTrue(builtin.editableBy(admin));
        assertTrue(app.lessons().findById(lesson.getId()).orElseThrow().editableBy(teacher));

        Lesson live = app.lessons().findById(lesson.getId()).orElseThrow();
        assertEquals("Lesson saved and sent back to an admin for review.", app.studio().updateLesson(live,
                Form.of("title", "Extra Tips", "content", "Edited", "topic_id", String.valueOf(topicId), "minutes", "4"), teacher).message());
        app.lessons().delete(lesson.getId());
        assertTrue(app.quizzes().listByLesson(lesson.getId()).isEmpty());
    }
}
