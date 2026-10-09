package com.pyjudge.web.servlet;

import com.pyjudge.model.Difficulty;
import com.pyjudge.model.Lesson;
import com.pyjudge.model.Problem;
import com.pyjudge.model.QuizQuestion;
import com.pyjudge.model.TestCase;
import com.pyjudge.model.User;
import com.pyjudge.service.Outcome;
import com.pyjudge.web.BaseServlet;
import com.pyjudge.web.Ctx;
import com.pyjudge.web.HttpError;
import jakarta.servlet.annotation.WebServlet;

/** The editor for exercises (with their test cases) and lessons (with their quizzes). */
@WebServlet(name = "studio", urlPatterns = "/studio/*")
public class StudioServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;
    private static final int PREVIEW_CHARS = 2000;

    @Override
    protected void routes() {
        get("/studio/exercises/new", this::newExerciseForm);
        post("/studio/exercises/new", this::createExercise);
        get("/studio/exercises/{id}/edit", this::editExerciseForm);
        post("/studio/exercises/{id}/edit", this::updateExercise);
        post("/studio/exercises/{id}/delete", this::deleteExercise);
        post("/studio/exercises/{id}/testcases", this::addTestCase);
        post("/studio/testcases/{id}/delete", this::deleteTestCase);

        get("/studio/lessons/new", this::newLessonForm);
        post("/studio/lessons/new", this::createLesson);
        get("/studio/lessons/{id}/edit", this::editLessonForm);
        post("/studio/lessons/{id}/edit", this::updateLesson);
        post("/studio/lessons/{id}/delete", this::deleteLesson);
        post("/studio/lessons/{id}/questions", this::addQuestion);
        post("/studio/questions/{id}/delete", this::deleteQuestion);
    }

    // ------------------------------------------------------------ exercises

    /** The exercise with this id, if the current user may edit it (404 if missing, 403 if not theirs). */
    private Problem editableExercise(Ctx ctx, int id) {
        Problem problem = app.problems().findById(id).orElseThrow(HttpError::notFound);
        if (!problem.editableBy(ctx.requireUser())) {
            throw HttpError.forbidden();
        }
        return problem;
    }

    private void showExercise(Ctx ctx, Problem problem, boolean isNew) throws Exception {
        ctx.set("problem", problem).set("difficulties", Difficulty.values()).set("isNew", isNew);
        if (!isNew) {
            ctx.set("testCases", app.testCases().listPreviews(problem.getId(), PREVIEW_CHARS));
        }
        ctx.render("studio/exercise_form");
    }

    private void newExerciseForm(Ctx ctx) throws Exception {
        showExercise(ctx, app.studio().draftExercise(), true);
    }

    private void createExercise(Ctx ctx) throws Exception {
        Problem problem = app.studio().draftExercise();
        Outcome outcome = app.studio().createExercise(problem, ctx.form(), ctx.requireUser());
        if (ctx.flash(outcome, "success")) {
            ctx.redirect("/studio/exercises/" + problem.getId() + "/edit");
        } else {
            showExercise(ctx, problem, true); // keep what was typed
        }
    }

    private void editExerciseForm(Ctx ctx) throws Exception {
        showExercise(ctx, editableExercise(ctx, ctx.pathInt("id")), false);
    }

    private void updateExercise(Ctx ctx) throws Exception {
        Problem problem = editableExercise(ctx, ctx.pathInt("id"));
        Outcome outcome = app.studio().updateExercise(problem, ctx.form(), ctx.requireUser());
        if (ctx.flash(outcome, "success")) {
            ctx.redirect("/studio/exercises/" + problem.getId() + "/edit");
        } else {
            showExercise(ctx, problem, false);
        }
    }

    private void deleteExercise(Ctx ctx) throws Exception {
        User user = ctx.requireUser();
        Problem problem = editableExercise(ctx, ctx.pathInt("id"));
        app.problems().delete(problem.getId());
        ctx.flash("info", "Deleted “" + problem.getTitle() + "”.");
        ctx.redirect(user.isAdmin() ? "/admin/content" : "/instructor/exercises");
    }

    private void addTestCase(Ctx ctx) throws Exception {
        Problem problem = editableExercise(ctx, ctx.pathInt("id"));
        ctx.flash(app.studio().addTestCase(problem, ctx.form()), "success");
        ctx.redirect("/studio/exercises/" + problem.getId() + "/edit#tests");
    }

    private void deleteTestCase(Ctx ctx) throws Exception {
        TestCase tc = app.testCases().findById(ctx.pathInt("id")).orElseThrow(HttpError::notFound);
        Problem problem = editableExercise(ctx, tc.getProblemId());
        app.testCases().delete(tc.getId());
        ctx.flash("info", "Test case deleted.");
        ctx.redirect("/studio/exercises/" + problem.getId() + "/edit#tests");
    }

    // ------------------------------------------------------------ lessons (tutorials)

    private Lesson editableLesson(Ctx ctx, int id) {
        Lesson lesson = app.lessons().findById(id).orElseThrow(HttpError::notFound);
        if (!lesson.editableBy(ctx.requireUser())) {
            throw HttpError.forbidden();
        }
        return lesson;
    }

    private void showLesson(Ctx ctx, Lesson lesson, boolean isNew) throws Exception {
        ctx.set("lesson", lesson).set("isNew", isNew).set("topics", app.topics().listAll());
        if (!isNew) {
            ctx.set("questions", app.quizzes().listByLesson(lesson.getId()));
        }
        ctx.render("studio/lesson_form");
    }

    private void newLessonForm(Ctx ctx) throws Exception {
        Lesson lesson = new Lesson();
        Integer topic = ctx.intParam("topic");
        if (topic != null) {
            lesson.setTopicId(topic);
        }
        showLesson(ctx, lesson, true);
    }

    private void createLesson(Ctx ctx) throws Exception {
        Lesson lesson = new Lesson();
        Outcome outcome = app.studio().createLesson(lesson, ctx.form(), ctx.requireUser());
        if (ctx.flash(outcome, "success")) {
            ctx.redirect("/studio/lessons/" + lesson.getId() + "/edit");
        } else {
            showLesson(ctx, lesson, true);
        }
    }

    private void editLessonForm(Ctx ctx) throws Exception {
        showLesson(ctx, editableLesson(ctx, ctx.pathInt("id")), false);
    }

    private void updateLesson(Ctx ctx) throws Exception {
        Lesson lesson = editableLesson(ctx, ctx.pathInt("id"));
        Outcome outcome = app.studio().updateLesson(lesson, ctx.form(), ctx.requireUser());
        if (ctx.flash(outcome, "success")) {
            ctx.redirect("/studio/lessons/" + lesson.getId() + "/edit");
        } else {
            // Show the typed values, but keep the stored topic for the "preview" link
            lesson.setTopic(app.lessons().findById(lesson.getId()).orElseThrow(HttpError::notFound).getTopic());
            showLesson(ctx, lesson, false);
        }
    }

    private void deleteLesson(Ctx ctx) throws Exception {
        Lesson lesson = editableLesson(ctx, ctx.pathInt("id"));
        if (lesson.isBuiltin()) {
            ctx.flash("error", "Built-in course lessons live in the curriculum folder and can't be deleted here.");
            ctx.redirect("/studio/lessons/" + lesson.getId() + "/edit");
            return;
        }
        app.lessons().delete(lesson.getId());
        ctx.flash("info", "Deleted lesson “" + lesson.getTitle() + "”.");
        ctx.redirect("/instructor/lessons");
    }

    private void addQuestion(Ctx ctx) throws Exception {
        Lesson lesson = editableLesson(ctx, ctx.pathInt("id"));
        ctx.flash(app.studio().addQuestion(lesson, ctx.form()), "success");
        ctx.redirect("/studio/lessons/" + lesson.getId() + "/edit#quiz");
    }

    private void deleteQuestion(Ctx ctx) throws Exception {
        QuizQuestion question = app.quizzes().findById(ctx.pathInt("id")).orElseThrow(HttpError::notFound);
        Lesson lesson = editableLesson(ctx, question.getLessonId());
        app.quizzes().delete(question.getId());
        ctx.flash("info", "Question deleted.");
        ctx.redirect("/studio/lessons/" + lesson.getId() + "/edit#quiz");
    }
}
