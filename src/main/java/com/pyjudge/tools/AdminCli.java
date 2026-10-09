package com.pyjudge.tools;

import com.pyjudge.AppContext;
import com.pyjudge.config.AppConfig;
import com.pyjudge.curriculum.CurriculumSync;
import com.pyjudge.model.Role;
import com.pyjudge.model.User;
import java.util.Map;

/**
 * Maintenance commands, run against the configured database:
 *
 * <pre>
 * mvn -q compile exec:java -Dexec.args="seed"
 * mvn -q compile exec:java -Dexec.args="set-role alice instructor"
 * </pre>
 */
public final class AdminCli {
    private AdminCli() {
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            usage();
            return;
        }
        // The command line never seeds implicitly and judges nothing, so start-up stays quick
        AppConfig config = AppConfig.of(Map.of("seed.onStartup", "false", "judge.workers", "1"));
        try (AppContext app = AppContext.start(config)) {
            switch (args[0]) {
                case "seed" -> seed(app);
                case "set-role" -> setRole(app, args);
                default -> usage();
            }
        }
    }

    private static void seed(AppContext app) {
        app.seeder().bootstrapAccounts();
        System.out.println("Added " + app.seeder().seedProblems() + " exercise(s).");
        CurriculumSync.Result course = app.curriculum().sync();
        System.out.println("Course: " + course.topicsCreated() + " new topic(s), " + course.lessonsCreated()
                + " new lesson(s), " + course.lessonsUpdated() + " lesson(s) refreshed.");
        app.seeder().seedChallenge();
    }

    private static void setRole(AppContext app, String[] args) {
        Role role = args.length == 3 ? Role.fromKey(args[2]) : null;
        if (role == null) {
            System.err.println("Usage: set-role <username> <student|instructor|admin>");
            return;
        }
        User user = app.users().findByUsername(args[1]).orElse(null);
        if (user == null) {
            System.err.println("No user named '" + args[1] + "'. Register on the site first.");
            return;
        }
        user.setRole(role);
        app.users().update(user);
        System.out.println(user.getUsername() + " is now a " + role.key() + ".");
    }

    private static void usage() {
        System.out.println("Commands:");
        System.out.println("  seed                          add the starter exercises, the Python course and a sample challenge");
        System.out.println("  set-role <username> <role>    change a user's role: student, instructor or admin");
    }
}
