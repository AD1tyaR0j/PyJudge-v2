package com.pyjudge.curriculum;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses the Markdown files of the built-in Python course.
 *
 * <pre>
 * ---
 * title: Variables and Types
 * minutes: 8
 * practice: basics, math          (exercise tags to suggest for practice)
 * ---
 * Markdown body. A ```pyrun fence becomes a runnable playground; inside it, a line
 * `### stdin` separates the code from the input it should be run with.
 *
 * &lt;!-- quiz --&gt;
 * Q: Question text (Markdown allowed)
 * - [ ] wrong option
 * - [x] correct option
 * &gt; Explanation shown after answering.
 * </pre>
 */
public final class CurriculumParser {
    public static final String QUIZ_MARKER = "<!-- quiz -->";
    public static final String STDIN_MARKER = "### stdin";
    /** {@code ```pyrun} runs cleanly; {@code ```pyrun-error} demonstrates an exception on purpose. */
    public static final Pattern PYRUN = Pattern.compile("```(pyrun(?:-error)?)\\n(.*?)```", Pattern.DOTALL);

    /** A runnable code block inside a lesson. */
    public record Playground(String code, String stdin, boolean expectsError) {
    }

    public record QuizSpec(String prompt, List<String> options, int answer, String explanation) {
    }

    public record LessonSpec(String slug, int position, String title, int minutes, String practice, String content,
                             List<QuizSpec> quiz) {
    }

    public record TopicSpec(String slug, int position, String title, String icon, String level, String summary,
                            String description, List<LessonSpec> lessons) {
    }

    /** The key/value header between the {@code ---} lines, and everything after it. */
    record FrontMatter(Map<String, String> meta, String body) {
    }

    private CurriculumParser() {
    }

    static FrontMatter frontMatter(String text, String where) {
        text = text.replace("\r\n", "\n");
        if (!text.startsWith("---\n")) {
            throw new CurriculumException(where + ": missing front matter");
        }
        String rest = text.substring(4);
        int end = rest.indexOf("\n---\n");
        String head = end < 0 ? rest : rest.substring(0, end);
        String body = end < 0 ? "" : rest.substring(end + 5);
        Map<String, String> meta = new LinkedHashMap<>();
        for (String line : head.split("\n")) {
            if (!line.isBlank()) {
                int colon = line.indexOf(':');
                String key = colon < 0 ? line : line.substring(0, colon);
                String value = colon < 0 ? "" : line.substring(colon + 1);
                meta.put(key.strip(), value.strip());
            }
        }
        return new FrontMatter(meta, stripNewlines(body));
    }

    public static List<QuizSpec> parseQuiz(String text, String where) {
        // Mutable while parsing; converted to immutable QuizSpec records at the end
        final class Draft {
            String prompt;
            final List<String> options = new ArrayList<>();
            Integer answer;
            String explanation = "";
        }
        List<Draft> drafts = new ArrayList<>();
        Draft current = null;
        for (String raw : text.split("\n")) {
            String line = raw.stripTrailing();
            if (line.startsWith("Q: ")) {
                current = new Draft();
                current.prompt = line.substring(3).strip();
                drafts.add(current);
            } else if (current == null || line.isBlank()) {
                continue;
            } else if (line.startsWith("- [ ] ") || line.startsWith("- [x] ")) {
                if (line.startsWith("- [x] ")) {
                    if (current.answer != null) {
                        throw new CurriculumException(where + ": two correct answers in “" + current.prompt + "”");
                    }
                    current.answer = current.options.size();
                }
                current.options.add(line.substring(6).strip());
            } else if (line.startsWith("> ")) {
                current.explanation = (current.explanation + " " + line.substring(2).strip()).strip();
            } else if (current.options.isEmpty()) {
                current.prompt += "\n" + line; // multi-line prompt (e.g. a code snippet)
            }
        }
        List<QuizSpec> out = new ArrayList<>();
        for (Draft d : drafts) {
            if (d.options.size() < 2 || d.answer == null) {
                throw new CurriculumException(where + ": “" + d.prompt + "” needs 2+ options and exactly one [x]");
            }
            out.add(new QuizSpec(d.prompt, List.copyOf(d.options), d.answer, d.explanation));
        }
        return out;
    }

    public static LessonSpec parseLesson(String text, String where, String slug, int position) {
        FrontMatter fm = frontMatter(text, where);
        int marker = fm.body().indexOf(QUIZ_MARKER);
        String content = marker < 0 ? fm.body() : fm.body().substring(0, marker);
        String quiz = marker < 0 ? "" : fm.body().substring(marker + QUIZ_MARKER.length());
        if (!fm.meta().containsKey("title")) {
            throw new CurriculumException(where + ": missing title");
        }
        int minutes;
        try {
            minutes = Integer.parseInt(fm.meta().getOrDefault("minutes", "5"));
        } catch (NumberFormatException e) {
            throw new CurriculumException(where + ": minutes must be a number");
        }
        return new LessonSpec(slug, position, fm.meta().get("title"), minutes, fm.meta().getOrDefault("practice", ""),
                content.strip(), quiz.isBlank() ? List.of() : parseQuiz(quiz, where));
    }

    /** Splits a {@code ```pyrun} block into the code and the stdin it should be run with. */
    public static Playground playground(String block, boolean expectsError) {
        String separator = "\n" + STDIN_MARKER + "\n";
        int at = block.indexOf(separator);
        if (at < 0) {
            if (block.startsWith(STDIN_MARKER + "\n")) {
                return new Playground("", block.substring(STDIN_MARKER.length() + 1), expectsError);
            }
            return new Playground(block.replaceAll("\n+$", "") + "\n", "", expectsError);
        }
        String code = block.substring(0, at).replaceAll("\n+$", "") + "\n";
        return new Playground(code, block.substring(at + separator.length()), expectsError);
    }

    /** Every runnable block in a lesson. */
    public static List<Playground> playgrounds(String content) {
        List<Playground> out = new ArrayList<>();
        Matcher m = PYRUN.matcher(content);
        while (m.find()) {
            out.add(playground(m.group(2), "pyrun-error".equals(m.group(1))));
        }
        return out;
    }

    private static String stripNewlines(String s) {
        return s.replaceAll("^\n+|\n+$", "");
    }
}
