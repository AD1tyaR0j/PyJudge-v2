package com.pyjudge.dao;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.pyjudge.db.Database;
import com.pyjudge.model.QuizQuestion;
import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class QuizDao extends BaseDao {
    private static final String SELECT = "SELECT id, lesson_id, prompt, options_json, answer, explanation, sort_order FROM quiz_questions";
    private static final Gson GSON = new Gson();
    private static final Type STRING_LIST = new TypeToken<List<String>>() { }.getType();

    public QuizDao(Database db) {
        super(db);
    }

    private static QuizQuestion map(ResultSet rs) throws SQLException {
        QuizQuestion q = new QuizQuestion();
        q.setId(rs.getInt("id"));
        q.setLessonId(rs.getInt("lesson_id"));
        q.setPrompt(rs.getString("prompt"));
        List<String> options = GSON.fromJson(rs.getString("options_json"), STRING_LIST);
        q.setOptions(options == null ? new ArrayList<>() : options);
        q.setAnswer(rs.getInt("answer"));
        q.setExplanation(rs.getString("explanation"));
        q.setPosition(rs.getInt("sort_order"));
        return q;
    }

    public Optional<QuizQuestion> findById(int id) {
        return db.queryOne(SELECT + " WHERE id = ?", QuizDao::map, id);
    }

    public List<QuizQuestion> listByLesson(int lessonId) {
        return db.query(SELECT + " WHERE lesson_id = ? ORDER BY sort_order, id", QuizDao::map, lessonId);
    }

    public QuizQuestion insert(QuizQuestion q) {
        q.setId(db.insert("INSERT INTO quiz_questions (lesson_id, prompt, options_json, answer, explanation, sort_order)"
                        + " VALUES (?, ?, ?, ?, ?, ?)",
                q.getLessonId(), q.getPrompt(), GSON.toJson(q.getOptions()), q.getAnswer(), q.getExplanation(),
                q.getPosition()));
        return q;
    }

    /** Swaps a lesson's whole quiz for {@code questions} (used when the built-in course is refreshed). */
    public void replaceForLesson(int lessonId, List<QuizQuestion> questions) {
        db.inTransaction(() -> {
            db.update("DELETE FROM quiz_questions WHERE lesson_id = ?", lessonId);
            List<Object[]> rows = new ArrayList<>();
            for (int i = 0; i < questions.size(); i++) {
                QuizQuestion q = questions.get(i);
                rows.add(new Object[] {lessonId, q.getPrompt(), GSON.toJson(q.getOptions()), q.getAnswer(),
                        q.getExplanation(), i});
            }
            db.batch("INSERT INTO quiz_questions (lesson_id, prompt, options_json, answer, explanation, sort_order)"
                    + " VALUES (?, ?, ?, ?, ?, ?)", rows);
        });
    }

    public void delete(int id) {
        db.update("DELETE FROM quiz_questions WHERE id = ?", id);
    }
}
