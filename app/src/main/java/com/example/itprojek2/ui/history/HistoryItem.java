package com.example.itprojek2.ui.history;

public class HistoryItem {
    private int id;
    private String message;
    private String date;
    private boolean selected;

    public HistoryItem(int id, String message, String date) {
        this.id = id;
        this.message = message;
        this.date = date;
        this.selected = false;
    }

    public int getId() { return id; }
    public String getMessage() { return message; }
    public String getDate() { return date; }
    public boolean isSelected() { return selected; }
    public void setSelected(boolean selected) { this.selected = selected; }
}
