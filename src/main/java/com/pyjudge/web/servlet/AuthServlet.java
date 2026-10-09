package com.pyjudge.web.servlet;

import com.pyjudge.model.Role;
import com.pyjudge.model.User;
import com.pyjudge.web.BaseServlet;
import com.pyjudge.web.Ctx;
import jakarta.servlet.SessionCookieConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpSession;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Sign-up, login and logout. The logged-in user's id is the only thing kept in the HTTP session. */
@WebServlet(name = "auth", urlPatterns = {"/login", "/register", "/logout"})
public class AuthServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;
    private static final int REMEMBER_SECONDS = 30 * 24 * 60 * 60;

    @Override
    protected void routes() {
        get("/register", this::registerForm);
        post("/register", this::register);
        get("/login", this::loginForm);
        post("/login", this::login);
        post("/logout", this::logout);
    }

    private void registerForm(Ctx ctx) throws Exception {
        if (redirectedAway(ctx)) {
            return;
        }
        ctx.set("form", Map.of("username", "", "name", "", "email", ""));
        ctx.render("register");
    }

    /** Logged-in users go to their dashboard; when sign-ups are closed everyone goes to the login page. */
    private boolean redirectedAway(Ctx ctx) throws Exception {
        if (ctx.user() != null) {
            ctx.redirect("/dashboard");
            return true;
        }
        if (!app.settings().flag("registration_open")) {
            ctx.flash("info", "Sign-ups are closed right now. Ask an administrator for an account.");
            ctx.redirect("/login");
            return true;
        }
        return false;
    }

    private void register(Ctx ctx) throws Exception {
        if (redirectedAway(ctx)) {
            return;
        }
        Map<String, String> form = new LinkedHashMap<>();
        form.put("username", ctx.param("username"));
        form.put("name", ctx.param("name"));
        form.put("email", ctx.param("email").toLowerCase(Locale.ROOT));
        String password = ctx.form().get("password");

        List<String> errors = app.accounts().validate(form.get("username"), form.get("name"), form.get("email"),
                password, ctx.form().get("confirm"), null);
        if (!errors.isEmpty()) {
            ctx.flashErrors(errors);
            ctx.set("form", form);
            ctx.render("register");
            return;
        }
        // Self-registration always creates a student; staff accounts are created by an admin
        User user = app.accounts().create(form.get("username"), form.get("name"), form.get("email"), password, Role.STUDENT);
        signIn(ctx, user, false);
        ctx.flash("success", "Welcome, " + user.getDisplayName() + "! Pick an exercise to get started.");
        ctx.redirect("/dashboard");
    }

    private void loginForm(Ctx ctx) throws Exception {
        if (ctx.user() != null) {
            ctx.redirect("/dashboard");
            return;
        }
        ctx.set("identifier", "");
        ctx.render("login");
    }

    private void login(Ctx ctx) throws Exception {
        if (ctx.user() != null) {
            ctx.redirect("/dashboard");
            return;
        }
        String identifier = ctx.param("identifier");
        Optional<User> match = app.accounts().authenticate(identifier, ctx.form().get("password"));
        if (match.isEmpty()) {
            ctx.flash("error", "Invalid username/email or password.");
        } else if (match.get().isBlocked()) {
            ctx.flash("error", "This account has been blocked. Contact an administrator.");
        } else {
            signIn(ctx, match.get(), ctx.form().checked("remember"));
            ctx.redirect(safeNext(ctx.param("next")));
            return;
        }
        ctx.set("identifier", identifier);
        ctx.render("login");
    }

    /** Only paths inside this application are followed after login, never another site. */
    private static String safeNext(String target) {
        boolean local = target.startsWith("/") && !target.startsWith("//") && !target.contains("\\")
                && !target.contains("\r") && !target.contains("\n");
        return local ? target : "/dashboard";
    }

    private void signIn(Ctx ctx, User user, boolean remember) {
        ctx.request().changeSessionId(); // a fresh id after login defeats session fixation
        HttpSession session = ctx.request().getSession();
        session.setAttribute(Ctx.SESSION_USER_ID, user.getId());
        if (remember) {
            // Keep the session for 30 days and make its cookie outlive the browser window
            session.setMaxInactiveInterval(REMEMBER_SECONDS);
            SessionCookieConfig config = getServletContext().getSessionCookieConfig();
            Cookie cookie = new Cookie(config.getName() == null ? "JSESSIONID" : config.getName(), session.getId());
            String context = ctx.request().getContextPath();
            cookie.setPath(context.isEmpty() ? "/" : context);
            cookie.setHttpOnly(true);
            cookie.setSecure(ctx.request().isSecure());
            cookie.setMaxAge(REMEMBER_SECONDS);
            ctx.response().addCookie(cookie);
        }
    }

    private void logout(Ctx ctx) throws Exception {
        ctx.requireUser();
        ctx.request().getSession().invalidate();
        ctx.flash("info", "You have been logged out.");
        ctx.redirect("/");
    }
}
