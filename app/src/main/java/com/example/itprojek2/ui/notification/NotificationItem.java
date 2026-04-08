package com.example.itprojek2.ui.notification;

public class NotificationItem {
    public enum Type { CRITICAL, SUCCESS, WARNING, INFO }

    private String title;
    private String description;
    private Type type;
    private boolean isHeader;
    private String headerTitle;

    // Regular notification item
    public NotificationItem(String title, String description, Type type) {
        this.title = title;
        this.description = description;
        this.type = type;
        this.isHeader = false;
    }

    // Header item
    public NotificationItem(String headerTitle) {
        this.headerTitle = headerTitle;
        this.isHeader = true;
    }

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Type getType() { return type; }
    public boolean isHeader() { return isHeader; }
    public String getHeaderTitle() { return headerTitle; }
}
