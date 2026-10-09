package com.pyjudge.web;

import com.pyjudge.model.Difficulty;
import com.pyjudge.model.Problem;
import com.pyjudge.model.Verdict;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Small view models shared by several pages. */
public final class Views {
    private static final Map<Difficulty, String> COLOURS = Map.of(
            Difficulty.EASY, "green", Difficulty.MEDIUM, "orange", Difficulty.HARD, "red");

    private Views() {
    }

    /** One row per difficulty: how many approved exercises exist and how many of them are solved. */
    public static List<Map<String, Object>> byDifficulty(List<Problem> approved, Set<Integer> solvedIds) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Difficulty d : Difficulty.values()) {
            long total = approved.stream().filter(p -> p.getDifficulty() == d).count();
            long solved = approved.stream().filter(p -> p.getDifficulty() == d && solvedIds.contains(p.getId())).count();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("difficulty", d);
            row.put("colour", COLOURS.get(d));
            row.put("solved", solved);
            row.put("total", total);
            row.put("percent", total > 0 ? Math.round(100f * solved / total) : 0);
            rows.add(row);
        }
        return rows;
    }

    /** The profile page's "submissions by verdict" table, in a fixed order. */
    public static List<Map<String, Object>> verdictRows(Map<String, Integer> counts) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Verdict v : List.of(Verdict.AC, Verdict.WA, Verdict.TLE, Verdict.MLE, Verdict.RE, Verdict.CE)) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("verdict", v);
            row.put("count", counts.getOrDefault(v.name(), 0));
            rows.add(row);
        }
        return rows;
    }

    public static int score(List<Problem> solved) {
        return solved.stream().mapToInt(p -> p.getDifficulty().getPoints()).sum();
    }
}
