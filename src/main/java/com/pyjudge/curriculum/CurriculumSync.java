package com.pyjudge.curriculum;

import com.pyjudge.curriculum.CurriculumParser.LessonSpec;
import com.pyjudge.curriculum.CurriculumParser.QuizSpec;
import com.pyjudge.curriculum.CurriculumParser.TopicSpec;
import com.pyjudge.dao.LessonDao;
import com.pyjudge.dao.QuizDao;
import com.pyjudge.dao.TopicDao;
import com.pyjudge.db.Database;
import com.pyjudge.model.ContentStatus;
import com.pyjudge.model.Lesson;
import com.pyjudge.model.QuizQuestion;
import com.pyjudge.model.Topic;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Copies the built-in course into the database. Safe to run again: it updates what it created before. */
public class CurriculumSync {
    /** What one run changed. */
    public record Result(int topicsCreated, int lessonsCreated, int lessonsUpdated) {
    }

    private final Database db;
    private final TopicDao topics;
    private final LessonDao lessons;
    private final QuizDao quizzes;

    public CurriculumSync(Database db, TopicDao topics, LessonDao lessons, QuizDao quizzes) {
        this.db = db;
        this.topics = topics;
        this.lessons = lessons;
        this.quizzes = quizzes;
    }

    public Result sync() {
        return sync(CurriculumLoader.load());
    }

    /** Creates or updates built-in topics and lessons. Custom (instructor) lessons are never touched. */
    public Result sync(List<TopicSpec> course) {
        return db.inTransaction(() -> {
            int createdTopics = 0;
            int created = 0;
            int updated = 0;
            for (TopicSpec spec : course) {
                Optional<Topic> existing = topics.findBySlug(spec.slug());
                Topic topic = existing.orElseGet(Topic::new);
                topic.setSlug(spec.slug());
                topic.setTitle(spec.title());
                topic.setIcon(spec.icon());
                topic.setLevel(spec.level());
                topic.setSummary(spec.summary());
                topic.setDescription(spec.description());
                topic.setPosition(spec.position());
                if (existing.isPresent()) {
                    topics.update(topic);
                } else {
                    topics.insert(topic);
                    createdTopics++;
                }

                for (LessonSpec ls : spec.lessons()) {
                    Optional<Lesson> found = lessons.find(topic.getId(), ls.slug());
                    if (found.isPresent() && !found.get().isBuiltin()) {
                        continue; // an instructor's lesson happens to share the slug: leave it alone
                    }
                    Lesson lesson = found.orElseGet(Lesson::new);
                    lesson.setTopicId(topic.getId());
                    lesson.setSlug(ls.slug());
                    lesson.setTitle(ls.title());
                    lesson.setContent(ls.content());
                    lesson.setMinutes(ls.minutes());
                    lesson.setPracticeTags(ls.practice());
                    lesson.setPosition(ls.position());
                    if (found.isPresent()) {
                        lessons.update(lesson);
                        updated++;
                    } else {
                        lesson.setSource(Lesson.BUILTIN);
                        lesson.setStatus(ContentStatus.APPROVED);
                        lessons.insert(lesson);
                        created++;
                    }
                    quizzes.replaceForLesson(lesson.getId(), toQuestions(ls.quiz()));
                }
            }
            return new Result(createdTopics, created, updated);
        });
    }

    private static List<QuizQuestion> toQuestions(List<QuizSpec> specs) {
        List<QuizQuestion> out = new ArrayList<>();
        for (QuizSpec spec : specs) {
            QuizQuestion q = new QuizQuestion();
            q.setPrompt(spec.prompt());
            q.setOptions(spec.options());
            q.setAnswer(spec.answer());
            q.setExplanation(spec.explanation());
            out.add(q);
        }
        return out;
    }
}
