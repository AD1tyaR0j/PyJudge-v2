package com.pyjudge.web.servlet;

import com.pyjudge.model.Lesson;
import com.pyjudge.model.LessonProgress;
import com.pyjudge.model.Problem;
import com.pyjudge.model.QuizQuestion;
import com.pyjudge.model.Topic;
import com.pyjudge.model.User;
import com.pyjudge.service.CourseOverview;
import com.pyjudge.service.CourseOverview.TopicProgress;
import com.pyjudge.service.CourseService;
import com.pyjudge.service.CourseService.Neighbours;
import com.pyjudge.service.CourseService.QuizOutcome;
import com.pyjudge.web.BaseServlet;
import com.pyjudge.web.Ctx;
import com.pyjudge.web.HttpError;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** The Python course: the roadmap, topic pages, lesson pages with runnable examples, and quizzes. */
@WebServlet(name = "learn", urlPatterns = {"/learn", "/learn/*"})
public class LearnServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void routes() {
        get("/learn", this::index);
        get("/learn/{topic}", this::topic);
        get("/learn/{topic}/{lesson}", this::lesson);
        post("/learn/{topic}/{lesson}/quiz", this::quiz);
        post("/learn/{topic}/{lesson}/complete", this::complete);
    }

    private void index(Ctx ctx) throws Exception {
        ctx.set("course", app.course().overview(ctx.user()));
        ctx.render("learn/index");
    }

    private Set<Integer> solvedIds(User user) {
        return user == null ? Set.of() : app.submissions().solvedProblemIds(user.getId());
    }

    private void topic(Ctx ctx) throws Exception {
        User user = ctx.user();
        Topic topic = app.topics().findBySlug(ctx.path("topic")).orElseThrow(HttpError::notFound);
        CourseOverview course = app.course().overview(user);
        TopicProgress info = course.getTopics().stream()
                .filter(t -> t.getTopic().getId() == topic.getId()).findFirst()
                .orElseThrow(HttpError::notFound); // a topic without published lessons is not shown

        Set<Integer> solved = solvedIds(user);
        // Up to three exercises per lesson, without repeats, six in total
        Map<Integer, Problem> practice = new LinkedHashMap<>();
        for (Lesson lesson : info.getLessons()) {
            app.course().practiceProblems(lesson, solved, 3).forEach(p -> practice.putIfAbsent(p.getId(), p));
        }
        ctx.set("info", info)
                .set("topic", info.getTopic())
                .set("course", course)
                .set("practice", practice.values().stream().limit(6).collect(Collectors.toList()))
                .set("solved", solved)
                .set("number", course.getTopics().indexOf(info) + 1);
        ctx.render("learn/topic");
    }

    /** The lesson at /learn/{topic}/{lesson}. Unpublished lessons are visible only to those who may edit them. */
    private Lesson findLesson(Ctx ctx) {
        Topic topic = app.topics().findBySlug(ctx.path("topic")).orElseThrow(HttpError::notFound);
        Lesson lesson = app.lessons().find(topic.getId(), ctx.path("lesson"))
                .filter(l -> l.visibleTo(ctx.user()))
                .orElseThrow(HttpError::notFound);
        lesson.setTopic(topic);
        return lesson;
    }

    private static String url(Lesson lesson) {
        return "/learn/" + lesson.getTopic().getSlug() + "/" + lesson.getSlug();
    }

    private void lesson(Ctx ctx) throws Exception {
        User user = ctx.user();
        Lesson lesson = findLesson(ctx);
        CourseOverview course = app.course().overview(user);
        Neighbours neighbours = app.course().neighbours(lesson);
        Set<Integer> solved = solvedIds(user);

        List<Map<String, Object>> questions = new ArrayList<>();
        for (QuizQuestion q : app.quizzes().listByLesson(lesson.getId())) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", q.getId());
            row.put("prompt", app.markdown().render(q.getPrompt()));
            row.put("options", q.getOptions().stream().map(app.markdown()::renderInline).collect(Collectors.toList()));
            questions.add(row);
        }
        // The sidebar lists the topic's published lessons; a preview of an unpublished one stands alone
        List<Lesson> siblings = !lesson.isApproved() ? List.of(lesson)
                : course.getTopics().stream().filter(t -> t.getTopic().getId() == lesson.getTopicId())
                        .findFirst().map(TopicProgress::getLessons).orElse(List.of(lesson));

        ctx.set("topic", lesson.getTopic())
                .set("lesson", lesson)
                .set("body", app.markdown().renderLesson(lesson.getContent()))
                .set("questions", questions)
                .set("progress", course.getProgress().get(lesson.getId()))
                .set("siblings", siblings)
                .set("course", course)
                .set("prevLesson", neighbours.previous())
                .set("nextLesson", neighbours.next())
                .set("practice", app.course().practiceProblems(lesson, solved, 4))
                .set("solved", solved)
                .set("passPercent", (int) Math.round(CourseService.PASS_RATIO * 100))
                .set("lessonJs", Map.of("loggedIn", user != null, "loginUrl", ctx.request().getContextPath()
                        + "/login?next=" + URLEncoder.encode(url(lesson), StandardCharsets.UTF_8)));
        ctx.render("learn/lesson");
    }

    /** Marks a quiz and answers with JSON; learn.js shows the result without reloading the page. */
    private void quiz(Ctx ctx) throws Exception {
        Lesson lesson = findLesson(ctx);
        if (lesson.getQuestionCount() == 0) {
            ctx.json(HttpServletResponse.SC_BAD_REQUEST, Map.of("error", "This lesson has no quiz."));
            return;
        }
        QuizOutcome outcome = app.course().gradeQuiz(ctx.user(), lesson, ctx.form());
        LessonProgress saved = outcome.progress();
        Lesson next = app.course().neighbours(lesson).next();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("score", outcome.score());
        body.put("total", outcome.total());
        body.put("passed", outcome.passed());
        body.put("results", outcome.results());
        body.put("saved", saved != null); // guests are graded but nothing is stored
        body.put("completed", saved != null && saved.isCompleted());
        body.put("best", saved == null ? null : saved.getQuizScore());
        body.put("next_url", ctx.request().getContextPath() + (next == null ? "/learn" : url(next)));
        ctx.json(HttpServletResponse.SC_OK, body);
    }

    /** For lessons without a quiz: mark as read. */
    private void complete(Ctx ctx) throws Exception {
        Lesson lesson = findLesson(ctx);
        User user = ctx.user();
        if (user == null) {
            ctx.json(HttpServletResponse.SC_UNAUTHORIZED, Map.of("error", "Log in to save your progress."));
        } else if (lesson.getQuestionCount() > 0) {
            ctx.json(HttpServletResponse.SC_BAD_REQUEST, Map.of("error", "Pass the quiz to complete this lesson."));
        } else {
            app.course().record(user, lesson, null, null, true);
            ctx.json(HttpServletResponse.SC_OK, Map.of("completed", true));
        }
    }
}
