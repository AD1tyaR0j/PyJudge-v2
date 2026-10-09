package com.pyjudge.db;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Creates the tables on first start by running {@code schema.sql} (every statement is IF NOT EXISTS). */
public final class SchemaInitializer {
    private SchemaInitializer() {
    }

    public static void run(Database db) {
        List<String> statements = statements(read("/schema.sql"));
        db.execute(connection -> {
            try (Statement st = connection.createStatement()) {
                for (String sql : statements) {
                    st.execute(sql);
                }
            }
            return null;
        });
    }

    static String read(String resource) {
        try (InputStream in = SchemaInitializer.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new DaoException("Missing resource " + resource);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new DaoException("Could not read " + resource, e);
        }
    }

    /** Splits a script on semicolons, dropping {@code --} comment lines. */
    static List<String> statements(String script) {
        StringBuilder withoutComments = new StringBuilder();
        for (String line : script.split("\n")) {
            if (!line.stripLeading().startsWith("--")) {
                withoutComments.append(line).append('\n');
            }
        }
        List<String> out = new ArrayList<>();
        for (String part : withoutComments.toString().split(";")) {
            if (!part.isBlank()) {
                out.add(part.strip());
            }
        }
        return out;
    }
}
