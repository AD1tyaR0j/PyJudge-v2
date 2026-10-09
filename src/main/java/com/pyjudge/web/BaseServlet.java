package com.pyjudge.web;

import com.pyjudge.AppContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Base class of every servlet in the application. A subclass lists its routes once in
 * {@link #routes()}, e.g. {@code get("/admin/users/{id}/edit", this::editUserForm)}, and this class
 * dispatches each GET or POST to the first route whose pattern matches the request path.
 * It also turns {@link HttpError} and {@link LoginRequired} into the right HTTP response.
 */
public abstract class BaseServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(BaseServlet.class.getName());
    private static final Pattern VARIABLE = Pattern.compile("\\{(\\w+)}");

    /** A request handler: the method that produces the response for one route. */
    @FunctionalInterface
    protected interface Handler {
        void handle(Ctx ctx) throws Exception;
    }

    private record Route(String method, Pattern pattern, List<String> variables, Handler handler) {
    }

    private final transient List<Route> routes = new ArrayList<>();
    protected transient AppContext app;

    @Override
    public void init() throws ServletException {
        app = (AppContext) getServletContext().getAttribute(AppContext.ATTRIBUTE);
        if (app == null) {
            throw new ServletException("The application did not start. Check the server log for the reason "
                    + "(usually the database connection in application.properties).");
        }
        routes();
    }

    /** Registers this servlet's routes with {@link #get} and {@link #post}. */
    protected abstract void routes();

    protected void get(String pattern, Handler handler) {
        add("GET", pattern, handler);
    }

    protected void post(String pattern, Handler handler) {
        add("POST", pattern, handler);
    }

    /** Compiles {@code /users/{id}/edit} into a regular expression with one group per variable. */
    private void add(String method, String pattern, Handler handler) {
        List<String> variables = new ArrayList<>();
        Matcher m = VARIABLE.matcher(pattern);
        StringBuilder regex = new StringBuilder();
        int last = 0;
        while (m.find()) {
            regex.append(Pattern.quote(pattern.substring(last, m.start()))).append("([^/]+)");
            variables.add(m.group(1));
            last = m.end();
        }
        regex.append(Pattern.quote(pattern.substring(last)));
        routes.add(new Route(method, Pattern.compile(regex.toString()), variables, handler));
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        dispatch("GET", req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        dispatch("POST", req, resp);
    }

    /** The request path inside the application, without a trailing slash: {@code /admin/users/7/edit}. */
    public static String appPath(HttpServletRequest req) {
        String path = req.getRequestURI().substring(req.getContextPath().length());
        int params = path.indexOf(';');
        if (params >= 0) {
            path = path.substring(0, params);
        }
        if (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path.isEmpty() ? "/" : path;
    }

    private void dispatch(String method, HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String path = appPath(req);
        boolean pathKnown = false;
        for (Route route : routes) {
            Matcher m = route.pattern().matcher(path);
            if (!m.matches()) {
                continue;
            }
            pathKnown = true;
            if (!route.method().equals(method)) {
                continue;
            }
            Map<String, String> vars = new LinkedHashMap<>();
            for (int i = 0; i < route.variables().size(); i++) {
                vars.put(route.variables().get(i), URLDecoder.decode(m.group(i + 1), StandardCharsets.UTF_8));
            }
            run(route.handler(), new Ctx(req, resp, app, vars), path);
            return;
        }
        fail(req, resp, pathKnown ? HttpServletResponse.SC_METHOD_NOT_ALLOWED : HttpServletResponse.SC_NOT_FOUND,
                pathKnown ? "That action isn't available here." : "That page doesn't exist.");
    }

    private void run(Handler handler, Ctx ctx, String path) throws ServletException, IOException {
        HttpServletRequest req = ctx.request();
        HttpServletResponse resp = ctx.response();
        try {
            handler.handle(ctx);
        } catch (LoginRequired e) {
            if (isApi(req)) {
                Json.write(resp, HttpServletResponse.SC_UNAUTHORIZED, Map.of("error", "Login required"));
            } else {
                String query = req.getQueryString();
                String next = path + (query == null ? "" : "?" + query);
                ctx.redirect("/login?next=" + URLEncoder.encode(next, StandardCharsets.UTF_8));
            }
        } catch (HttpError e) {
            fail(req, resp, e.getStatus(), e.getMessage());
        } catch (ServletException | IOException e) {
            throw e;
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Unhandled error for " + req.getMethod() + " " + path, e);
            fail(req, resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Something went wrong on our side. Please try again.");
        }
    }

    private static boolean isApi(HttpServletRequest req) {
        return appPath(req).startsWith("/api/");
    }

    /** JSON for API calls, the error page (see web.xml) for everything else. */
    private static void fail(HttpServletRequest req, HttpServletResponse resp, int status, String message)
            throws IOException {
        if (resp.isCommitted()) {
            return;
        }
        if (isApi(req)) {
            Json.write(resp, status, Map.of("error", message));
        } else {
            resp.sendError(status, message);
        }
    }
}
