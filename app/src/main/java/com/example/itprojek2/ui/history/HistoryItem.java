package com.example.itprojek2.ui.history;

public class HistoryItem {
    private String key;        // Firebase push key (ex: "-NxABC...")
    private String message;
    private String date;
    private long timestamp;
    private String type;       // "pump", "auto", "schedule", "threshold", "mode", "offline"
    private boolean selected;

    public HistoryItem(String key, String message, String date, long timestamp, String type) {
        this.key = key;
        this.message = message;
        this.date = date;
        this.timestamp = timestamp;
        this.type = type;
        this.selected = false;
    }

    // Backward compat constructor (pakai int id → diabaikan)
    public HistoryItem(int id, String message, String date) {
        this(String.valueOf(id), message, date, System.currentTimeMillis(), "pump");
    }

    public String getKey() { return key; }
    public String getMessage() { return message; }
    public String getDate() { return date; }
    public long getTimestamp() { return timestamp; }
    public String getType() { return type; }
    public boolean isSelected() { return selected; }
    public void setSelected(boolean selected) { this.selected = selected; }

    // Backward compat
    public int getId() { return 0; }
}
