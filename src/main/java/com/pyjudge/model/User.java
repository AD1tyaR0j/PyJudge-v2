package com.pyjudge.model;

import java.time.LocalDateTime;
import java.util.Locale;

public class User {
    private int id;
    private String username;
    private String name = "";
    private String email;
    private String bio = "";
    private String passwordHash;
    private Role role = Role.STUDENT;
    private boolean blocked;
    private LocalDateTime createdAt;
    private LocalDateTime lastSeenAt;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name == null ? "" : name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio == null ? "" : bio; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public boolean isBlocked() { return blocked; }
    public void setBlocked(boolean blocked) { this.blocked = blocked; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(LocalDateTime lastSeenAt) { this.lastSeenAt = lastSeenAt; }

    public String getDisplayName() {
        return name == null || name.isBlank() ? username : name;
    }

    /** First letter of the display name, for the avatar circle. */
    public String getInitial() {
        String display = getDisplayName();
        return display == null || display.isEmpty() ? "?" : display.substring(0, 1).toUpperCase(Locale.ROOT);
    }

    public boolean isAdmin() { return role == Role.ADMIN; }
    public boolean isInstructor() { return role == Role.INSTRUCTOR; }
    public boolean isStudent() { return role == Role.STUDENT; }

    /** Admins and instructors can create exercises, lessons and challenges. */
    public boolean isAuthor() { return role.canAuthor(); }
}
