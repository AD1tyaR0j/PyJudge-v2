package com.pyjudge.service;

import com.pyjudge.dao.UserDao;
import com.pyjudge.model.Role;
import com.pyjudge.model.User;
import com.pyjudge.util.PasswordHasher;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/** Sign-up, login, profile editing and the admin's user management. */
public class AccountService {
    public static final int MIN_PASSWORD = 8;
    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9_]{3,30}$");
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final UserDao users;

    public AccountService(UserDao users) {
        this.users = users;
    }

    /**
     * Shared by sign-up, profile editing and admin user management.
     *
     * @param username  null when the username is not being changed
     * @param password  null when the password is not being changed
     * @param confirm   null when there is no confirmation field
     * @param excludeId the account being edited, so its own username/email do not count as taken
     */
    public List<String> validate(String username, String name, String email, String password, String confirm,
                                 Integer excludeId) {
        List<String> errors = new ArrayList<>();
        if (username != null && !USERNAME.matcher(username).matches()) {
            errors.add("Username must be 3–30 characters: letters, numbers, or underscores.");
        }
        if (name == null || name.isEmpty() || name.length() > 100) {
            errors.add("Enter a name (up to 100 characters).");
        }
        if (email == null || !EMAIL.matcher(email).matches() || email.length() > 100) {
            errors.add("Enter a valid email address.");
        }
        if (password != null) {
            if (password.length() < MIN_PASSWORD) {
                errors.add("Password must be at least " + MIN_PASSWORD + " characters.");
            }
            if (confirm != null && !password.equals(confirm)) {
                errors.add("Passwords do not match.");
            }
        }
        if (username != null && isTaken(users.findByUsername(username), excludeId)) {
            errors.add("That username is taken.");
        }
        if (email != null && isTaken(users.findByEmail(email), excludeId)) {
            errors.add("An account with that email already exists.");
        }
        return errors;
    }

    private static boolean isTaken(Optional<User> existing, Integer excludeId) {
        return existing.filter(u -> excludeId == null || u.getId() != excludeId).isPresent();
    }

    /** Creates an account. Call {@link #validate} first. */
    public User create(String username, String name, String email, String password, Role role) {
        User user = new User();
        user.setUsername(username);
        user.setName(name);
        user.setEmail(email.toLowerCase(Locale.ROOT));
        user.setRole(role);
        user.setPasswordHash(PasswordHasher.hash(password));
        return users.insert(user);
    }

    /** The account matching a username or email and password, blocked or not; empty when either is wrong. */
    public Optional<User> authenticate(String identifier, String password) {
        return users.findByLogin(identifier).filter(u -> PasswordHasher.verify(password, u.getPasswordHash()));
    }

    /** Profile form on the account page. Returns validation errors. */
    public List<String> updateProfile(User user, String name, String email, String bio) {
        List<String> errors = validate(null, name, email, null, null, user.getId());
        if (errors.isEmpty()) {
            user.setName(name);
            user.setEmail(email);
            user.setBio(bio.length() > 1000 ? bio.substring(0, 1000) : bio);
            users.update(user);
        }
        return errors;
    }

    /** Password form on the account page. Returns an error message, or null on success. */
    public String changePassword(User user, String current, String next, String confirm) {
        if (!PasswordHasher.verify(current, user.getPasswordHash())) {
            return "Your current password is incorrect.";
        }
        if (next.length() < MIN_PASSWORD) {
            return "New password must be at least " + MIN_PASSWORD + " characters.";
        }
        if (!next.equals(confirm)) {
            return "New passwords do not match.";
        }
        user.setPasswordHash(PasswordHasher.hash(next));
        users.update(user);
        return null;
    }

    /** Admin "create user" form. */
    public List<String> adminCreate(Form form) {
        String username = form.trimmed("username");
        String name = form.trimmed("name");
        String email = form.trimmed("email").toLowerCase(Locale.ROOT);
        String password = form.get("password");
        Role role = Role.fromKey(form.trimmed("role"));
        List<String> errors = validate(username, name, email, password, null, null);
        if (role == null) {
            errors.add("Pick a valid role.");
        }
        if (errors.isEmpty()) {
            create(username, name, email, password, role);
        }
        return errors;
    }

    /** Admin "edit user" form: name, email, role, optional new password, block/unblock. */
    public List<String> adminUpdate(User target, User admin, Form form) {
        String name = form.trimmed("name");
        String email = form.trimmed("email").toLowerCase(Locale.ROOT);
        String password = form.get("password");
        String roleKey = form.raw("role");
        Role role = roleKey == null ? target.getRole() : Role.fromKey(roleKey);
        boolean blocked = form.checked("is_blocked");

        List<String> errors = validate(null, name, email, password.isEmpty() ? null : password, null, target.getId());
        if (role == null) {
            errors.add("Pick a valid role.");
        }
        if (target.getId() == admin.getId() && (role != Role.ADMIN || blocked)) {
            errors.add("You can't remove your own admin access or block yourself.");
        }
        if (errors.isEmpty()) {
            target.setName(name);
            target.setEmail(email);
            target.setRole(role);
            target.setBlocked(blocked);
            if (!password.isEmpty()) {
                target.setPasswordHash(PasswordHasher.hash(password));
            }
            users.update(target);
        }
        return errors;
    }
}
