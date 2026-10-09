package com.pyjudge.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Reader;

/** JSON in and out for the API servlet and for data embedded in pages. */
public final class Json {
    /**
     * Gson's default HTML-safe mode writes {@code <}, {@code >} and {@code &} as unicode escapes, so the
     * output can sit inside a script element without a stray closing tag ending it.
     */
    private static final Gson GSON = new GsonBuilder().serializeNulls().create();

    private Json() {
    }

    public static String stringify(Object value) {
        return GSON.toJson(value);
    }

    public static void write(HttpServletResponse resp, int status, Object body) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(GSON.toJson(body));
    }

    /** The request body as a JSON object; an empty object when it is missing or malformed. */
    public static JsonObject readObject(HttpServletRequest req) throws IOException {
        try (Reader reader = req.getReader()) {
            var parsed = JsonParser.parseReader(reader);
            return parsed.isJsonObject() ? parsed.getAsJsonObject() : new JsonObject();
        } catch (JsonParseException | IllegalStateException e) {
            return new JsonObject();
        }
    }

    /** A string property, or null when it is absent or not a string. */
    public static String string(JsonObject obj, String name) {
        var el = obj.get(name);
        return el != null && el.isJsonPrimitive() && el.getAsJsonPrimitive().isString() ? el.getAsString() : null;
    }

    /** A whole-number property, or null when it is absent or not a number. */
    public static Integer integer(JsonObject obj, String name) {
        var el = obj.get(name);
        try {
            return el != null && el.isJsonPrimitive() && el.getAsJsonPrimitive().isNumber() ? el.getAsInt() : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
