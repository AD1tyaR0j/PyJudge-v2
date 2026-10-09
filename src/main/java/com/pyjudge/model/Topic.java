package com.pyjudge.model;

/** A chapter of the Python course, e.g. "Strings" or "Functions". */
public class Topic {
    private int id;
    private String slug;
    private String title;
    private String icon = "📘";
    private String level = "Beginner";
    private String summary = "";
    private String description = "";
    private int position;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
}
