package com.pyjudge.curriculum;

import com.pyjudge.curriculum.CurriculumParser.FrontMatter;
import com.pyjudge.curriculum.CurriculumParser.LessonSpec;
import com.pyjudge.curriculum.CurriculumParser.TopicSpec;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Reads the built-in Python course, stored as Markdown so it reads well on GitHub and is easy to edit.
 *
 * <pre>
 * curriculum/NN-topic-slug/_topic.md       topic front matter + description
 * curriculum/NN-topic-slug/NN-lesson.md    one lesson each
 * </pre>
 */
public final class CurriculumLoader {
    private CurriculumLoader() {
    }

    /** Loads the course bundled with the application (the {@code curriculum} resource folder). */
    public static List<TopicSpec> load() {
        URL url = CurriculumLoader.class.getResource("/curriculum");
        if (url == null) {
            throw new CurriculumException("The curriculum folder is missing from the classpath.");
        }
        try {
            URI uri = url.toURI();
            if ("file".equals(uri.getScheme())) {
                return load(Path.of(uri));
            }
            // Packed inside a jar: mount it as a file system and walk it the same way
            try (FileSystem fs = openFileSystem(uri)) {
                return load(fs.getPath("/curriculum"));
            }
        } catch (URISyntaxException | IOException e) {
            throw new CurriculumException("Could not read the bundled curriculum from " + url, e);
        }
    }

    private static FileSystem openFileSystem(URI uri) throws IOException {
        try {
            return FileSystems.getFileSystem(uri);
        } catch (FileSystemNotFoundException e) {
            return FileSystems.newFileSystem(uri, Map.of());
        }
    }

    public static List<TopicSpec> load(Path root) {
        List<TopicSpec> topics = new ArrayList<>();
        try {
            List<Path> folders = numbered(root, true);
            for (int position = 0; position < folders.size(); position++) {
                Path folder = folders.get(position);
                String folderName = folder.getFileName().toString().replace("/", "");
                FrontMatter fm = CurriculumParser.frontMatter(read(folder.resolve("_topic.md")), folderName);

                List<LessonSpec> lessons = new ArrayList<>();
                List<Path> files = numbered(folder, false);
                for (int i = 0; i < files.size(); i++) {
                    String fileName = files.get(i).getFileName().toString();
                    lessons.add(CurriculumParser.parseLesson(read(files.get(i)), folderName + "/" + fileName,
                            slug(fileName.replaceAll("\\.md$", "")), i));
                }
                if (!fm.meta().containsKey("title")) {
                    throw new CurriculumException(folderName + ": missing title");
                }
                topics.add(new TopicSpec(slug(folderName), position, fm.meta().get("title"),
                        fm.meta().getOrDefault("icon", "📘"), fm.meta().getOrDefault("level", "Beginner"),
                        fm.meta().getOrDefault("summary", ""), fm.body(), lessons));
            }
        } catch (IOException e) {
            throw new CurriculumException("Could not read the curriculum in " + root, e);
        }
        return topics;
    }

    /** Entries whose name starts with a digit, in name order: topic folders, or lesson files inside one. */
    private static List<Path> numbered(Path dir, boolean directories) throws IOException {
        try (Stream<Path> entries = Files.list(dir)) {
            return entries
                    .filter(p -> Files.isDirectory(p) == directories)
                    .filter(p -> Character.isDigit(p.getFileName().toString().charAt(0)))
                    .filter(p -> directories || p.getFileName().toString().endsWith(".md"))
                    .sorted((a, b) -> a.getFileName().toString().compareTo(b.getFileName().toString()))
                    .collect(Collectors.toList());
        }
    }

    private static String read(Path file) throws IOException {
        return Files.readString(file, StandardCharsets.UTF_8);
    }

    /** "03-strings" becomes "strings". */
    private static String slug(String name) {
        return name.replaceFirst("^\\d+-", "");
    }
}
