package com.pyjudge.service;

import com.pyjudge.dao.ProblemDao;
import com.pyjudge.dao.SubmissionDao;
import com.pyjudge.model.Difficulty;
import com.pyjudge.model.User;
import java.util.List;
import java.util.Set;

/** Badges computed from activity (nothing extra is stored). */
public class AchievementService {
    public static final class Badge {
        private final String icon;
        private final String name;
        private final String desc;
        private final boolean earned;
        private final String progress;

        Badge(String icon, String name, String desc, boolean earned, String progress) {
            this.icon = icon;
            this.name = name;
            this.desc = desc;
            this.earned = earned;
            this.progress = progress;
        }

        public String getIcon() { return icon; }
        public String getName() { return name; }
        public String getDesc() { return desc; }
        public boolean isEarned() { return earned; }
        public String getProgress() { return progress; }
    }

    private final CourseService course;
    private final StatsService stats;
    private final ChallengeService challenges;
    private final SubmissionDao submissions;
    private final ProblemDao problems;

    public AchievementService(CourseService course, StatsService stats, ChallengeService challenges,
                              SubmissionDao submissions, ProblemDao problems) {
        this.course = course;
        this.stats = stats;
        this.challenges = challenges;
        this.submissions = submissions;
        this.problems = problems;
    }

    public List<Badge> forUser(User user) {
        CourseOverview c = course.overview(user);
        Set<Integer> solvedIds = submissions.solvedProblemIds(user.getId());
        int solved = solvedIds.size();
        long hard = problems.findByIds(solvedIds).stream().filter(p -> p.getDifficulty() == Difficulty.HARD).count();
        long topicsDone = c.getTopicsDone();
        int days = stats.streak(user.getId());
        boolean challengeDone = challenges.hasCompletedAny(user.getId());
        int done = c.getDone();
        int perfect = c.getPerfectQuizzes();

        return List.of(
                new Badge("👣", "First Steps", "Complete your first lesson", done >= 1, Math.min(done, 1) + "/1"),
                new Badge("📚", "Bookworm", "Complete 10 lessons", done >= 10, Math.min(done, 10) + "/10"),
                new Badge("🧩", "Topic Master", "Finish every lesson in a topic", topicsDone >= 1, topicsDone + " topics"),
                new Badge("🎓", "Graduate", "Complete the whole Python course",
                        c.getTotal() > 0 && done == c.getTotal(), done + "/" + c.getTotal()),
                new Badge("🧠", "Quiz Whiz", "Get 5 perfect quiz scores", perfect >= 5, Math.min(perfect, 5) + "/5"),
                new Badge("✅", "Problem Solver", "Solve 10 exercises", solved >= 10, Math.min(solved, 10) + "/10"),
                new Badge("🏔️", "Hard Hitter", "Solve a Hard exercise", hard >= 1, Math.min(hard, 1) + "/1"),
                new Badge("🔥", "On Fire", "Keep a 7-day learning streak", days >= 7, Math.min(days, 7) + "/7 days"),
                new Badge("🏁", "Challenger", "Complete a challenge", challengeDone, challengeDone ? "done" : "0/1"));
    }

    public static long earnedCount(List<Badge> badges) {
        return badges.stream().filter(Badge::isEarned).count();
    }
}
