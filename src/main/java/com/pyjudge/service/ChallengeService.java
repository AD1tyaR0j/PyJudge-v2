package com.pyjudge.service;

import com.pyjudge.dao.ChallengeDao;
import com.pyjudge.dao.ProblemDao;
import com.pyjudge.dao.SubmissionDao;
import com.pyjudge.model.Challenge;
import com.pyjudge.model.Problem;
import com.pyjudge.model.User;
import com.pyjudge.util.Times;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** Time-boxed challenges: progress, completion and the create/edit form. */
public class ChallengeService {
    /** A challenge with one user's progress attached. */
    public static final class Summary {
        private final Challenge challenge;
        private final int solved;
        private final int total;

        Summary(Challenge challenge, int solved, int total) {
            this.challenge = challenge;
            this.solved = solved;
            this.total = total;
        }

        public Challenge getChallenge() { return challenge; }
        public int getSolved() { return solved; }
        public int getTotal() { return total; }
        public boolean isCompleted() { return total > 0 && solved == total; }
        public int getPercent() { return total > 0 ? Math.round(100f * solved / total) : 0; }
    }

    private final ChallengeDao challenges;
    private final ProblemDao problems;
    private final SubmissionDao submissions;

    public ChallengeService(ChallengeDao challenges, ProblemDao problems, SubmissionDao submissions) {
        this.challenges = challenges;
        this.problems = problems;
        this.submissions = submissions;
    }

    /** Exercises in the challenge that the user solved during its time window. */
    public Set<Integer> progress(Challenge challenge, int userId) {
        List<Integer> ids = challenge.getApprovedProblems().stream().map(Problem::getId).collect(Collectors.toList());
        return submissions.solvedInWindow(userId, ids, challenge.getStartAt(), challenge.getEndAt());
    }

    /** @param user null for a guest (progress is then zero) */
    public List<Summary> summarize(List<Challenge> list, User user) {
        List<Summary> out = new ArrayList<>();
        for (Challenge c : list) {
            int total = c.getApprovedProblems().size();
            int solved = user == null ? 0 : progress(c, user.getId()).size();
            out.add(new Summary(c, solved, total));
        }
        return out;
    }

    public boolean hasCompletedAny(int userId) {
        for (Challenge c : challenges.listAll(false)) {
            int total = c.getApprovedProblems().size();
            if (total > 0 && progress(c, userId).size() == total) {
                return true;
            }
        }
        return false;
    }

    /** A blank challenge for the "new" form: starts at the top of this hour and runs a week. */
    public Challenge draft() {
        Challenge c = new Challenge();
        LocalDateTime now = Times.now().withMinute(0).withSecond(0).withNano(0);
        c.setStartAt(now);
        c.setEndAt(now.plusDays(7));
        return c;
    }

    /** Copies the form into {@code c} and returns the validation errors. Nothing is saved. */
    public List<String> apply(Challenge c, Form form) {
        List<String> errors = new ArrayList<>();
        c.setTitle(form.trimmed("title"));
        c.setDescription(form.trimmed("description"));
        if (c.getTitle().isEmpty()) {
            errors.add("Title is required.");
        } else if (c.getTitle().length() > 150) {
            errors.add("Title must be 150 characters or fewer.");
        }
        try {
            LocalDateTime start = LocalDateTime.parse(form.trimmed("start_at"), Times.FORM);
            LocalDateTime end = LocalDateTime.parse(form.trimmed("end_at"), Times.FORM);
            c.setStartAt(start);
            c.setEndAt(end);
            if (!end.isAfter(start)) {
                errors.add("The end time must be after the start time.");
            }
        } catch (DateTimeParseException e) {
            errors.add("Enter valid start and end times.");
        }
        List<Integer> ids = form.values("problem_ids").stream().map(ChallengeService::parseId)
                .filter(Objects::nonNull).collect(Collectors.toList());
        c.setProblems(problems.findByIds(ids).stream().filter(Problem::isApproved).collect(Collectors.toList()));
        if (c.getProblems().isEmpty()) {
            errors.add("Pick at least one approved exercise.");
        }
        return errors;
    }

    private static Integer parseId(String raw) {
        try {
            return Integer.valueOf(raw.strip());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
