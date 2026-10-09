package com.pyjudge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pyjudge.judge.Judge;
import com.pyjudge.judge.JudgeResult;
import com.pyjudge.judge.ProcessSandbox;
import com.pyjudge.model.Challenge;
import com.pyjudge.model.Difficulty;
import com.pyjudge.model.Problem;
import com.pyjudge.model.Role;
import com.pyjudge.model.Verdict;
import com.pyjudge.seed.Seeder;
import com.pyjudge.seed.Seeder.SeedProblem;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** The bundled exercise bank and the first-run seed. */
class SeedTest {
    @Test
    void bankMetadataIsComplete() {
        List<String> titles = new ArrayList<>();
        Set<String> difficulties = new HashSet<>();
        Seeder.forEachProblem(spec -> {
            titles.add(spec.title);
            difficulties.add(spec.difficulty);
            assertNotNull(Difficulty.fromLabel(spec.difficulty), spec.title);
            assertFalse(spec.description.isBlank(), spec.title);
            assertFalse(spec.tags.isBlank(), spec.title);
            assertTrue(spec.samples.size() >= 1, spec.title + " needs a sample");
            assertTrue(spec.hidden.size() >= 2, spec.title + " needs hidden tests");
        });
        assertEquals(37, titles.size());
        assertEquals(37, new HashSet<>(titles).size(), "titles are unique");
        assertEquals(Set.of("Easy", "Medium", "Hard"), difficulties);
    }

    /** Runs each reference solution through the real judge: statement, tests and solution must agree. */
    @Test
    void referenceSolutionsAreAccepted() {
        Judge judge = new Judge(new ProcessSandbox(""));
        List<String> failures = new ArrayList<>();
        int[] checked = {0};
        Seeder.forEachProblem((SeedProblem spec) -> {
            if (spec.solution == null) {
                return;
            }
            checked[0]++;
            JudgeResult result = judge.judge(spec.solution, spec.testCases(), spec.timeLimit, 128);
            if (result.verdict() != Verdict.AC) {
                failures.add(spec.title + ": " + result.verdict() + " " + result.error());
            }
        });
        assertEquals(27, checked[0]);
        assertTrue(failures.isEmpty(), String.join("\n", failures));
    }

    @Test
    void seedIsIdempotentAndBootstrapsAccounts() {
        try (AppContext app = AppFixture.start()) {
            app.seeder().run();
            assertEquals(37, app.problems().countAll());
            assertEquals(37, app.problems().listApproved().size());
            assertEquals(32, app.lessons().listApproved().size());
            assertEquals(1, app.challenges().count());
            Challenge sprint = app.challenges().listAll(true).get(0);
            assertEquals(5, sprint.getProblems().size());
            assertEquals("active", sprint.getState());
            assertEquals(1, app.users().countByRole(Role.ADMIN));
            assertEquals(3, app.users().count(), "admin plus the demo instructor and student");
            assertTrue(app.accounts().authenticate("admin", "admin12345").isPresent());

            Problem twoSum = app.problems().findBySlug("two-sum").orElseThrow();
            assertEquals(2, app.testCases().countSamples(twoSum.getId()));
            assertEquals(6, app.testCases().count(twoSum.getId()));
            assertTrue(app.testCases().listPreviews(twoSum.getId(), 2000).stream().allMatch(tc -> tc.getInput().length() <= 2000));
            assertEquals(6, app.testCases().countsByProblem(null).get(twoSum.getId()));
            assertEquals(1, app.problems().searchApproved(Difficulty.MEDIUM, "hash", "two").size());
            assertEquals(37, app.problems().searchApproved(null, "", "").size());

            // Big hidden tests survive the round trip through the database byte for byte
            long largest = app.testCases().listByProblem(app.problems().findBySlug("longest-increasing-subsequence")
                    .orElseThrow().getId()).stream().mapToLong(tc -> tc.getInput().length()).max().orElse(0);
            assertTrue(largest > 500_000, "largest input " + largest);

            app.seeder().run();
            assertEquals(37, app.problems().countAll());
            assertEquals(1, app.challenges().count());
            assertEquals(3, app.users().count());
            assertEquals(32, app.lessons().listApproved().size());
        }
    }
}
