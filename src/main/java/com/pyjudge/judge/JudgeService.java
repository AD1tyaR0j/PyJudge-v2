package com.pyjudge.judge;

import com.pyjudge.dao.ProblemDao;
import com.pyjudge.dao.SubmissionDao;
import com.pyjudge.dao.TestCaseDao;
import com.pyjudge.model.Problem;
import com.pyjudge.model.Submission;
import com.pyjudge.model.TestCase;
import com.pyjudge.model.Verdict;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Background judging. Submissions are queued on a fixed pool of worker threads so the HTTP
 * request returns immediately and the browser polls for the verdict.
 */
public class JudgeService implements AutoCloseable {
    private static final Logger LOG = Logger.getLogger(JudgeService.class.getName());

    private final Judge judge;
    private final SubmissionDao submissions;
    private final ProblemDao problems;
    private final TestCaseDao testCases;
    private final boolean synchronous;
    private final ExecutorService workers;

    /** @param synchronous judge inside the calling thread instead of the pool (used by the tests) */
    public JudgeService(Judge judge, SubmissionDao submissions, ProblemDao problems, TestCaseDao testCases,
                        int workerCount, boolean synchronous) {
        this.judge = judge;
        this.submissions = submissions;
        this.problems = problems;
        this.testCases = testCases;
        this.synchronous = synchronous;
        AtomicInteger counter = new AtomicInteger();
        ThreadFactory factory = runnable -> {
            Thread thread = new Thread(runnable, "judge-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
        this.workers = Executors.newFixedThreadPool(Math.max(1, workerCount), factory);
    }

    /** Queues a submission for judging (or judges it right away in synchronous mode). */
    public void dispatch(int submissionId) {
        if (synchronous) {
            judgeSubmission(submissionId);
        } else {
            workers.submit(() -> judgeSubmission(submissionId));
        }
    }

    void judgeSubmission(int submissionId) {
        Submission sub = submissions.findById(submissionId).orElse(null);
        if (sub == null) {
            return;
        }
        JudgeResult result;
        int total = sub.getTotal();
        try {
            Problem problem = problems.findById(sub.getProblemId()).orElseThrow();
            List<TestCase> cases = testCases.listByProblem(problem.getId());
            total = cases.size();
            result = judge.judge(sub.getCode(), cases, problem.getTimeLimit(), problem.getMemoryLimit());
        } catch (RuntimeException e) {
            LOG.log(Level.SEVERE, "Judging failed for submission " + submissionId, e);
            result = new JudgeResult(Verdict.IE, 0, total, null, "The judge hit an internal error. Please try again.");
        }
        try {
            submissions.saveResult(sub.getId(), result.verdict(), result.passed(), result.total(), result.runtimeMs(),
                    result.error());
        } catch (RuntimeException e) {
            LOG.log(Level.SEVERE, "Could not store the verdict for submission " + submissionId, e);
        }
    }

    @Override
    public void close() {
        workers.shutdown();
        try {
            if (!workers.awaitTermination(5, TimeUnit.SECONDS)) {
                workers.shutdownNow();
            }
        } catch (InterruptedException e) {
            workers.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
