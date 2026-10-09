package com.pyjudge.service;

import com.pyjudge.dao.LessonDao;
import com.pyjudge.dao.ProblemDao;
import com.pyjudge.dao.ProgressDao;
import com.pyjudge.dao.QuizDao;
import com.pyjudge.dao.TopicDao;
import com.pyjudge.model.Lesson;
import com.pyjudge.model.LessonProgress;
import com.pyjudge.model.Problem;
import com.pyjudge.model.QuizQuestion;
import com.pyjudge.model.Topic;
import com.pyjudge.model.User;
import com.pyjudge.service.CourseOverview.TopicProgress;
import com.pyjudge.util.Times;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** The Python course: progress, quizzes, and which exercises to practise after a lesson. */
public class CourseService {
    /** Share of quiz questions a student must get right to complete a lesson. */
    public static final double PASS_RATIO = 0.5;

    /** The lessons before and after one lesson, across the whole course (either may be null). */
    public record Neighbours(Lesson previous, Lesson next) {
    }

    /** Marking of one quiz attempt, in the shape the browser expects. */
    public record QuizOutcome(int score, int total, boolean passed, List<Map<String, Object>> results,
                              LessonProgress progress) {
    }

    private final TopicDao topics;
    private final LessonDao lessons;
    private final QuizDao quizzes;
    private final ProgressDao progress;
    private final ProblemDao problems;
    private final MarkdownService markdown;

    public CourseService(TopicDao topics, LessonDao lessons, QuizDao quizzes, ProgressDao progress,
                         ProblemDao problems, MarkdownService markdown) {
        this.topics = topics;
        this.lessons = lessons;
        this.quizzes = quizzes;
        this.progress = progress;
        this.problems = problems;
        this.markdown = markdown;
    }

    /** @param user null for a guest */
    public CourseOverview overview(User user) {
        Map<Integer, LessonProgress> mine = user == null ? Map.of() : progress.byUser(user.getId());
        Map<Integer, List<Lesson>> byTopic = lessons.listApproved().stream()
                .collect(Collectors.groupingBy(Lesson::getTopicId, LinkedHashMap::new, Collectors.toList()));

        List<TopicProgress> rows = new ArrayList<>();
        List<Lesson> all = new ArrayList<>();
        for (Topic topic : topics.listAll()) {
            List<Lesson> inTopic = byTopic.get(topic.getId());
            if (inTopic == null) {
                continue; // topics without published lessons are not shown
            }
            inTopic.forEach(l -> l.setTopic(topic));
            int done = (int) inTopic.stream().filter(l -> isDone(mine, l)).count();
            boolean started = inTopic.stream().anyMatch(l -> mine.containsKey(l.getId()));
            rows.add(new TopicProgress(topic, inTopic, done, started));
            all.addAll(inTopic);
        }

        int done = (int) all.stream().filter(l -> isDone(mine, l)).count();
        Lesson next = all.stream().filter(l -> !isDone(mine, l)).findFirst().orElse(null);
        List<LessonProgress> scored = mine.values().stream().filter(LessonProgress::isScored).collect(Collectors.toList());
        Integer quizAvg = null;
        if (!scored.isEmpty()) {
            int points = scored.stream().mapToInt(LessonProgress::getQuizScore).sum();
            int outOf = scored.stream().mapToInt(LessonProgress::getQuizTotal).sum();
            quizAvg = Math.round(100f * points / outOf);
        }
        int perfect = (int) scored.stream().filter(LessonProgress::isPerfect).count();
        return new CourseOverview(rows, mine, all.size(), done, next, quizAvg, perfect);
    }

    private static boolean isDone(Map<Integer, LessonProgress> mine, Lesson lesson) {
        LessonProgress p = mine.get(lesson.getId());
        return p != null && p.isCompleted();
    }

    public Neighbours neighbours(Lesson lesson) {
        List<Lesson> ordered = lessons.listApproved();
        for (int i = 0; i < ordered.size(); i++) {
            if (ordered.get(i).getId() == lesson.getId()) {
                return new Neighbours(i > 0 ? ordered.get(i - 1) : null,
                        i + 1 < ordered.size() ? ordered.get(i + 1) : null);
            }
        }
        return new Neighbours(null, null);
    }

    /**
     * Saves progress for a logged-in user and returns the row; returns null for guests.
     *
     * @param score quiz score of this attempt, or null when no quiz was taken
     */
    public LessonProgress record(User user, Lesson lesson, Integer score, Integer total, boolean complete) {
        if (user == null) {
            return null;
        }
        LessonProgress p = progress.find(user.getId(), lesson.getId()).orElseGet(() -> {
            LessonProgress fresh = new LessonProgress();
            fresh.setUserId(user.getId());
            fresh.setLessonId(lesson.getId());
            return fresh;
        });
        if (total != null) {
            p.setAttempts(p.getAttempts() + 1);
            if (p.getQuizScore() == null || score > p.getQuizScore()) { // keep the best attempt
                p.setQuizScore(score);
                p.setQuizTotal(total);
            }
        }
        if (complete && !p.isCompleted()) {
            p.setCompletedAt(Times.now());
        }
        progress.save(p);
        return p;
    }

    /** Marks a submitted quiz ({@code q<id>=<option index>} fields) and records the attempt. */
    public QuizOutcome gradeQuiz(User user, Lesson lesson, Form form) {
        List<QuizQuestion> questions = quizzes.listByLesson(lesson.getId());
        List<Map<String, Object>> results = new ArrayList<>();
        int score = 0;
        for (QuizQuestion q : questions) {
            Integer chosen = form.integer("q" + q.getId());
            boolean correct = chosen != null && chosen == q.getAnswer();
            if (correct) {
                score++;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", q.getId());
            row.put("chosen", chosen);
            row.put("answer", q.getAnswer());
            row.put("correct", correct);
            row.put("explanation", q.getExplanation().isEmpty() ? "" : markdown.renderInline(q.getExplanation()));
            results.add(row);
        }
        int total = questions.size();
        boolean passed = score >= total * PASS_RATIO;
        LessonProgress saved = record(user, lesson, score, total, passed);
        return new QuizOutcome(score, total, passed, results, saved);
    }

    /** Approved exercises sharing a tag with the lesson: unsolved and easier ones first. */
    public List<Problem> practiceProblems(Lesson lesson, Set<Integer> solvedIds, int limit) {
        Set<String> tags = Set.copyOf(lesson.getPracticeTagList());
        if (tags.isEmpty()) {
            return List.of();
        }
        return problems.listApproved().stream()
                .filter(p -> !Collections.disjoint(tags, p.getTagList()))
                .sorted(Comparator.comparing((Problem p) -> solvedIds.contains(p.getId()))
                        .thenComparing(p -> p.getDifficulty().ordinal())
                        .thenComparing(Problem::getId))
                .limit(limit)
                .collect(Collectors.toList());
    }
}
