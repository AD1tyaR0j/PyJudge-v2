package com.pyjudge.service;

import com.pyjudge.model.Lesson;
import com.pyjudge.model.LessonProgress;
import com.pyjudge.model.Topic;
import java.util.List;
import java.util.Map;

/** The course as one user sees it: topics with published lessons, their progress, and where to continue. */
public class CourseOverview {
    /** One topic with its lessons and how far the user has got. */
    public static final class TopicProgress {
        private final Topic topic;
        private final List<Lesson> lessons;
        private final int done;
        private final boolean started;

        TopicProgress(Topic topic, List<Lesson> lessons, int done, boolean started) {
            this.topic = topic;
            this.lessons = lessons;
            this.done = done;
            this.started = started;
        }

        public Topic getTopic() { return topic; }
        public List<Lesson> getLessons() { return lessons; }
        public int getDone() { return done; }
        public int getTotal() { return lessons.size(); }
        public int getPercent() { return Math.round(100f * done / lessons.size()); }
        public int getMinutes() { return lessons.stream().mapToInt(Lesson::getMinutes).sum(); }

        /** {@code done}, {@code active} (started) or {@code new}. */
        public String getState() {
            if (done == lessons.size()) {
                return "done";
            }
            return started || done > 0 ? "active" : "new";
        }
    }

    private final List<TopicProgress> topics;
    private final Map<Integer, LessonProgress> progress;
    private final int total;
    private final int done;
    private final Lesson next;
    private final Integer quizAvg;
    private final int perfectQuizzes;

    CourseOverview(List<TopicProgress> topics, Map<Integer, LessonProgress> progress, int total, int done, Lesson next,
                   Integer quizAvg, int perfectQuizzes) {
        this.topics = topics;
        this.progress = progress;
        this.total = total;
        this.done = done;
        this.next = next;
        this.quizAvg = quizAvg;
        this.perfectQuizzes = perfectQuizzes;
    }

    public List<TopicProgress> getTopics() { return topics; }
    /** {lessonId: progress} for the user (empty for guests). */
    public Map<Integer, LessonProgress> getProgress() { return progress; }
    public int getTotal() { return total; }
    public int getDone() { return done; }
    public int getPercent() { return total > 0 ? Math.round(100f * done / total) : 0; }
    /** The first lesson the user has not completed, or null when the course is finished. */
    public Lesson getNext() { return next; }
    /** Average quiz score as a percentage, or null before the first quiz. */
    public Integer getQuizAvg() { return quizAvg; }
    public int getPerfectQuizzes() { return perfectQuizzes; }

    public long getTopicsDone() {
        return topics.stream().filter(t -> t.getState().equals("done")).count();
    }
}
