package com.pyjudge.service;

import com.pyjudge.dao.ProgressDao;
import com.pyjudge.dao.SubmissionDao;
import com.pyjudge.dao.SubmissionDao.ActivityRow;
import com.pyjudge.dao.SubmissionDao.Solve;
import com.pyjudge.dao.UserDao;
import com.pyjudge.model.User;
import com.pyjudge.model.Verdict;
import com.pyjudge.util.Times;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** Aggregations behind the dashboards, charts and leaderboards. */
public class StatsService {
    /** One line of a leaderboard. */
    public static final class LeaderRow {
        private final User user;
        private final int solved;
        private final int score;

        LeaderRow(User user, int solved, int score) {
            this.user = user;
            this.solved = solved;
            this.score = score;
        }

        public User getUser() { return user; }
        public int getSolved() { return solved; }
        public int getScore() { return score; }
    }

    private final SubmissionDao submissions;
    private final UserDao users;
    private final ProgressDao progress;

    public StatsService(SubmissionDao submissions, UserDao users, ProgressDao progress) {
        this.submissions = submissions;
        this.users = users;
        this.progress = progress;
    }

    /** The last {@code days} calendar days (UTC), oldest first, ending today. */
    static List<LocalDate> lastDays(int days) {
        LocalDate today = Times.now().toLocalDate();
        List<LocalDate> out = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            out.add(today.minusDays(i));
        }
        return out;
    }

    private static List<String> labels(List<LocalDate> days) {
        return days.stream().map(Times.DAY_LABEL::format).collect(Collectors.toList());
    }

    /** How many of the timestamps fall on each of the given days. */
    private static List<Integer> dailyCounts(Collection<LocalDateTime> timestamps, List<LocalDate> days) {
        Map<LocalDate, Integer> counts = new HashMap<>();
        timestamps.stream().filter(Objects::nonNull).forEach(ts -> counts.merge(ts.toLocalDate(), 1, Integer::sum));
        return days.stream().map(d -> counts.getOrDefault(d, 0)).collect(Collectors.toList());
    }

    /** Per-day submissions (accepted vs not), sign-ups and active students for the admin charts. */
    public Map<String, Object> activityOverview(int days) {
        LocalDateTime since = Times.now().minusDays(days);
        List<ActivityRow> rows = submissions.activitySince(since);
        List<LocalDate> labels = lastDays(days);

        List<LocalDateTime> accepted = new ArrayList<>();
        List<LocalDateTime> other = new ArrayList<>();
        Map<LocalDate, Set<Integer>> activeUsers = new HashMap<>();
        for (ActivityRow row : rows) {
            (row.verdict() == Verdict.AC ? accepted : other).add(row.submittedAt());
            activeUsers.computeIfAbsent(row.submittedAt().toLocalDate(), d -> new HashSet<>()).add(row.userId());
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("labels", labels(labels));
        out.put("accepted", dailyCounts(accepted, labels));
        out.put("other", dailyCounts(other, labels));
        out.put("active", labels.stream().map(d -> activeUsers.getOrDefault(d, Set.of()).size()).collect(Collectors.toList()));
        out.put("signups", dailyCounts(users.createdSince(since), labels));
        return out;
    }

    /** Cumulative count of distinct exercises solved, per day, for the last {@code days} days. */
    public Map<String, Object> solveTimeline(int userId, int days) {
        List<LocalDate> firstSolves = submissions.firstAcceptedTimes(userId).stream()
                .map(LocalDateTime::toLocalDate).collect(Collectors.toList());
        List<LocalDate> labels = lastDays(days);
        List<Integer> cumulative = labels.stream()
                .map(day -> (int) firstSolves.stream().filter(d -> !d.isAfter(day)).count())
                .collect(Collectors.toList());
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("labels", labels(labels));
        out.put("solved", cumulative);
        return out;
    }

    /** Consecutive days (ending today or yesterday) with a submission or a completed lesson. */
    public int streak(int userId) {
        Set<LocalDate> activeDays = new HashSet<>();
        submissions.submissionTimes(userId).forEach(ts -> activeDays.add(ts.toLocalDate()));
        progress.completionTimes(userId).forEach(ts -> activeDays.add(ts.toLocalDate()));
        LocalDate today = Times.now().toLocalDate();
        LocalDate cursor = activeDays.contains(today) ? today : today.minusDays(1);
        int count = 0;
        while (activeDays.contains(cursor)) {
            count++;
            cursor = cursor.minusDays(1);
        }
        return count;
    }

    public List<LeaderRow> leaderboard() {
        return leaderboard(null, null, null, null);
    }

    /**
     * Ranks users by points for the approved exercises they solved. Blocked users are left out.
     *
     * @param limit      keep only the top rows (null for everyone)
     * @param problemIds only count these exercises (null for all)
     * @param since      only count solves in this time window (either end may be null)
     */
    public List<LeaderRow> leaderboard(Integer limit, Collection<Integer> problemIds, LocalDateTime since,
                                       LocalDateTime until) {
        Map<Integer, Integer> solved = new HashMap<>();
        Map<Integer, Integer> score = new HashMap<>();
        for (Solve solve : submissions.solves(problemIds, since, until)) {
            solved.merge(solve.userId(), 1, Integer::sum);
            score.merge(solve.userId(), solve.difficulty() == null ? 10 : solve.difficulty().getPoints(), Integer::sum);
        }
        if (score.isEmpty()) {
            return List.of();
        }
        Map<Integer, User> byId = users.findByIds(score.keySet());
        List<LeaderRow> rows = byId.values().stream()
                .filter(u -> !u.isBlocked())
                .map(u -> new LeaderRow(u, solved.get(u.getId()), score.get(u.getId())))
                .sorted(Comparator.comparingInt(LeaderRow::getScore).reversed()
                        .thenComparing(Comparator.comparingInt(LeaderRow::getSolved).reversed())
                        .thenComparing(r -> r.getUser().getUsername().toLowerCase(Locale.ROOT)))
                .collect(Collectors.toList());
        return limit != null && rows.size() > limit ? rows.subList(0, limit) : rows;
    }

    /** 1-based position on the global leaderboard, or null when the user has not solved anything. */
    public Integer rankOf(int userId) {
        List<LeaderRow> rows = leaderboard();
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i).getUser().getId() == userId) {
                return i + 1;
            }
        }
        return null;
    }
}
