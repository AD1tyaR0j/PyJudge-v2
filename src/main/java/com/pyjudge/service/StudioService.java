package com.pyjudge.service;

import com.pyjudge.dao.LessonDao;
import com.pyjudge.dao.ProblemDao;
import com.pyjudge.dao.QuizDao;
import com.pyjudge.dao.TestCaseDao;
import com.pyjudge.dao.TopicDao;
import com.pyjudge.model.ContentStatus;
import com.pyjudge.model.Difficulty;
import com.pyjudge.model.Lesson;
import com.pyjudge.model.Problem;
import com.pyjudge.model.QuizQuestion;
import com.pyjudge.model.TestCase;
import com.pyjudge.model.Topic;
import com.pyjudge.model.User;
import com.pyjudge.util.Slugs;
import com.pyjudge.util.Times;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** Authoring of exercises and lessons, shared by instructors (their own content) and admins (all of it). */
public class StudioService {
    private final ProblemDao problems;
    private final TestCaseDao testCases;
    private final LessonDao lessons;
    private final TopicDao topics;
    private final QuizDao quizzes;
    private final SettingsService settings;

    public StudioService(ProblemDao problems, TestCaseDao testCases, LessonDao lessons, TopicDao topics,
                         QuizDao quizzes, SettingsService settings) {
        this.problems = problems;
        this.testCases = testCases;
        this.lessons = lessons;
        this.topics = topics;
        this.quizzes = quizzes;
        this.settings = settings;
    }

    /** Instructor content goes through admin review while "require approval" is on; admin content never does. */
    public boolean needsReview(User author) {
        return author.isInstructor() && settings.flag("require_approval");
    }

    // ------------------------------------------------------------ exercises

    /** A blank exercise with the site's default limits, for the "new" form. */
    public Problem draftExercise() {
        Problem p = new Problem();
        p.setTimeLimit(settings.number("default_time_limit"));
        p.setMemoryLimit(settings.number("default_memory_limit"));
        return p;
    }

    private List<String> applyExercise(Problem p, Form form) {
        List<String> errors = new ArrayList<>();
        String title = form.trimmed("title");
        Difficulty difficulty = Difficulty.fromLabel(form.raw("difficulty") == null ? "Easy" : form.get("difficulty"));
        Integer timeLimit = form.raw("time_limit") == null ? Integer.valueOf(2) : form.integer("time_limit");
        Integer memoryLimit = form.raw("memory_limit") == null ? Integer.valueOf(128) : form.integer("memory_limit");

        if (title.isEmpty()) {
            errors.add("Title is required.");
        } else if (title.length() > 150) {
            errors.add("Title must be 150 characters or fewer.");
        }
        if (form.get("description").isBlank()) {
            errors.add("Description is required.");
        }
        if (difficulty == null) {
            errors.add("Pick a valid difficulty.");
        }
        if (timeLimit == null || timeLimit < 1 || timeLimit > 10) {
            errors.add("Time limit must be 1–10 seconds.");
        }
        if (memoryLimit == null || memoryLimit < 16 || memoryLimit > 1024) {
            errors.add("Memory limit must be 16–1024 MB.");
        }
        String tags = Problem.joinTags(form.get("tags"));
        if (tags.length() > 255) {
            errors.add("Too many tags (255 characters at most).");
        }

        p.setTitle(title);
        p.setDescription(form.get("description"));
        if (difficulty != null) {
            p.setDifficulty(difficulty);
        }
        p.setTimeLimit(timeLimit == null ? 0 : timeLimit);
        p.setMemoryLimit(memoryLimit == null ? 0 : memoryLimit);
        p.setTags(tags);
        String starter = form.get("starter_code");
        p.setStarterCode(starter.isEmpty() ? Problem.DEFAULT_STARTER : starter);
        return errors;
    }

    /** Fills {@code p} from the form and inserts it. On failure {@code p} keeps the typed values for redisplay. */
    public Outcome createExercise(Problem p, Form form, User author) {
        List<String> errors = applyExercise(p, form);
        if (!errors.isEmpty()) {
            return Outcome.fail(errors);
        }
        p.setSlug(problems.uniqueSlug(p.getTitle(), null));
        p.setCreatedBy(author.getId());
        String message;
        if (needsReview(author)) {
            p.setStatus(ContentStatus.PENDING);
            message = "Exercise created and sent to an admin for approval. Add test cases while you wait.";
        } else {
            p.setStatus(ContentStatus.APPROVED);
            p.setReviewedBy(author.getId());
            p.setReviewedAt(Times.now());
            message = "Exercise created and published. Now add some test cases.";
        }
        problems.insert(p);
        return Outcome.ok(message);
    }

    /** Saves edits. An instructor editing a live or rejected exercise sends it back for review. */
    public Outcome updateExercise(Problem p, Form form, User editor) {
        List<String> errors = applyExercise(p, form);
        if (!errors.isEmpty()) {
            return Outcome.fail(errors);
        }
        p.setSlug(problems.uniqueSlug(p.getTitle(), p.getId()));
        String message = "Exercise saved.";
        if (needsReview(editor) && p.getStatus() != ContentStatus.PENDING) {
            p.setStatus(ContentStatus.PENDING);
            p.setReviewNote(null);
            message = "Exercise saved and sent back to an admin for review.";
        }
        problems.update(p);
        return Outcome.ok(message);
    }

    public Outcome addTestCase(Problem p, Form form) {
        String expected = form.get("expected_output");
        if (expected.isBlank()) {
            return Outcome.fail("Expected output is required.");
        }
        TestCase tc = new TestCase(form.get("input"), expected, form.checked("is_sample"));
        tc.setProblemId(p.getId());
        testCases.insert(tc);
        return Outcome.ok("Test case added.");
    }

    // ------------------------------------------------------------ lessons

    private List<String> applyLesson(Lesson lesson, Form form) {
        List<String> errors = new ArrayList<>();
        lesson.setTitle(form.trimmed("title"));
        lesson.setContent(form.get("content"));
        lesson.setPracticeTags(Problem.joinTags(form.get("practice_tags")));
        Integer topicId = form.integer("topic_id");
        Topic topic = topicId == null ? null : topics.findById(topicId).orElse(null);
        Integer minutes = form.raw("minutes") == null ? Integer.valueOf(5) : form.integer("minutes");
        lesson.setMinutes(minutes == null ? 0 : minutes);

        if (lesson.getTitle().isEmpty()) {
            errors.add("Title is required.");
        } else if (lesson.getTitle().length() > 150) {
            errors.add("Title must be 150 characters or fewer.");
        }
        if (lesson.getContent().isBlank()) {
            errors.add("Lesson content is required.");
        }
        if (topic == null) {
            errors.add("Pick a topic.");
        } else {
            lesson.setTopicId(topic.getId());
            lesson.setTopic(topic);
        }
        if (lesson.getMinutes() < 1 || lesson.getMinutes() > 120) {
            errors.add("Reading time must be 1–120 minutes.");
        }
        return errors;
    }

    public Outcome createLesson(Lesson lesson, Form form, User author) {
        List<String> errors = applyLesson(lesson, form);
        if (!errors.isEmpty()) {
            return Outcome.fail(errors);
        }
        String base = Slugs.slugify(lesson.getTitle());
        String slug = base;
        for (int n = 2; lessons.slugTaken(lesson.getTopicId(), slug); n++) {
            slug = base + "-" + n;
        }
        lesson.setSlug(slug);
        lesson.setSource(Lesson.CUSTOM);
        lesson.setCreatedBy(author.getId());
        lesson.setPosition(1000 + lessons.countInTopic(lesson.getTopicId())); // after the built-in lessons
        String message;
        if (needsReview(author)) {
            lesson.setStatus(ContentStatus.PENDING);
            message = "Lesson created and sent to an admin for approval. Add quiz questions while you wait.";
        } else {
            lesson.setStatus(ContentStatus.APPROVED);
            lesson.setReviewedBy(author.getId());
            lesson.setReviewedAt(Times.now());
            message = "Lesson published. Add a few quiz questions to check understanding.";
        }
        lessons.insert(lesson);
        return Outcome.ok(message);
    }

    public Outcome updateLesson(Lesson lesson, Form form, User editor) {
        List<String> errors = applyLesson(lesson, form);
        if (!errors.isEmpty()) {
            return Outcome.fail(errors);
        }
        String message = "Lesson saved.";
        if (needsReview(editor) && lesson.getStatus() != ContentStatus.PENDING) {
            lesson.setStatus(ContentStatus.PENDING);
            lesson.setReviewNote(null);
            message = "Lesson saved and sent back to an admin for review.";
        }
        lessons.update(lesson);
        return Outcome.ok(message);
    }

    /** Adds a multiple-choice question; the form has up to four {@code option} fields and an {@code answer} index. */
    public Outcome addQuestion(Lesson lesson, Form form) {
        String prompt = form.trimmed("prompt");
        List<String> options = form.values("option").stream().map(String::strip).collect(Collectors.toList());
        Integer answer = form.integer("answer");
        List<Integer> filled = new ArrayList<>();
        for (int i = 0; i < options.size(); i++) {
            if (!options.get(i).isEmpty()) {
                filled.add(i);
            }
        }
        if (prompt.isEmpty() || filled.size() < 2) {
            return Outcome.fail("A question needs a prompt and at least two options.");
        }
        if (answer == null || !filled.contains(answer)) {
            return Outcome.fail("Mark which (non-empty) option is correct.");
        }
        QuizQuestion q = new QuizQuestion();
        q.setLessonId(lesson.getId());
        q.setPrompt(prompt);
        q.setOptions(filled.stream().map(options::get).collect(Collectors.toList()));
        q.setAnswer(filled.indexOf(answer)); // empty options are dropped, so the index shifts
        q.setExplanation(form.trimmed("explanation"));
        q.setPosition(quizzes.listByLesson(lesson.getId()).size());
        quizzes.insert(q);
        return Outcome.ok("Question added.");
    }
}
