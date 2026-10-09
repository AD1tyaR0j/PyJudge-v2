package com.pyjudge.service;

import com.pyjudge.curriculum.CurriculumParser;
import com.pyjudge.curriculum.CurriculumParser.Playground;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

/** Markdown to HTML for exercise statements, challenge descriptions and lessons. */
public class MarkdownService {
    private static final String TOKEN = "PYJUDGEPLAYGROUND%dX";

    private final Parser parser;
    private final HtmlRenderer renderer;

    public MarkdownService() {
        var extensions = List.of(TablesExtension.create());
        this.parser = Parser.builder().extensions(extensions).build();
        // escapeHtml: raw HTML typed by an author is shown as text, never run in a student's browser
        this.renderer = HtmlRenderer.builder().extensions(extensions).escapeHtml(true).build();
    }

    public String render(String text) {
        return renderer.render(parser.parse(text == null ? "" : text));
    }

    /** Markdown for short snippets (quiz options): no wrapping paragraph. */
    public String renderInline(String text) {
        String html = render(text).strip();
        if (html.startsWith("<p>") && html.endsWith("</p>") && html.indexOf("<p>", 3) < 0) {
            return html.substring(3, html.length() - 4);
        }
        return html;
    }

    /** Markdown to HTML, with {@code ```pyrun} blocks turned into runnable playgrounds. */
    public String renderLesson(String content) {
        List<Playground> blocks = new ArrayList<>();
        Matcher m = CurriculumParser.PYRUN.matcher(content == null ? "" : content);
        StringBuilder text = new StringBuilder();
        while (m.find()) {
            blocks.add(CurriculumParser.playground(m.group(2), "pyrun-error".equals(m.group(1))));
            m.appendReplacement(text, Matcher.quoteReplacement("\n\n" + String.format(TOKEN, blocks.size() - 1) + "\n\n"));
        }
        m.appendTail(text);

        String html = render(text.toString());
        for (int i = 0; i < blocks.size(); i++) {
            html = html.replace("<p>" + String.format(TOKEN, i) + "</p>", playgroundHtml(blocks.get(i)));
        }
        return html;
    }

    private static String playgroundHtml(Playground block) {
        String code = block.code();
        String stdin = block.stdin();
        int rows = Math.max(3, lineCount(code));
        int stdinRows = Math.max(2, lineCount(stdin));
        return "<div class=\"playground\" data-expect-error=\"" + (block.expectsError() ? 1 : 0) + "\">"
                + "<div class=\"pg-bar\"><span class=\"pg-title\">▶ Try it"
                + (block.expectsError() ? " (this one fails on purpose)" : "") + "</span>"
                + "<span class=\"pg-actions\"><button type=\"button\" class=\"pg-reset\">Reset</button>"
                + "<button type=\"button\" class=\"pg-run\">Run</button></span></div>"
                + "<textarea class=\"pg-code\" spellcheck=\"false\" rows=\"" + rows + "\" aria-label=\"Python code\">"
                + escape(code) + "</textarea>"
                + "<details class=\"pg-input\"" + (stdin.isEmpty() ? "" : " open") + "><summary>Input (stdin)</summary>"
                + "<textarea class=\"pg-stdin\" spellcheck=\"false\" rows=\"" + stdinRows
                + "\" aria-label=\"Program input\">" + escape(stdin) + "</textarea></details>"
                + "<pre class=\"pg-output\" hidden></pre></div>";
    }

    private static int lineCount(String text) {
        return (int) text.chars().filter(c -> c == '\n').count() + 1;
    }

    public static String escape(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(text.length() + 16);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '&' -> out.append("&amp;");
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '"' -> out.append("&#34;");
                case '\'' -> out.append("&#39;");
                default -> out.append(c);
            }
        }
        return out.toString();
    }
}
