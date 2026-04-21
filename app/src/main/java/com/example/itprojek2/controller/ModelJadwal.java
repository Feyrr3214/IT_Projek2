package com.example.itprojek2.controller;

/**
 * ModelJadwal — Model data representasi 1 jadwal penyiraman.
 *
 * Diisi dari node Firebase:
 *   devices/{id}/control/schedules/{idJadwal}/
 */
public class ModelJadwal {
    private String id;
    private int hour;
    private int minute;
    private int duration;
    private boolean enabled;
    private String time;

    // Konstruktor kosong dibutuhkan untuk Firebase Realtime Database
    public ModelJadwal() {}

    public ModelJadwal(String id, int hour, int minute, int duration, boolean enabled) {
        this.id = id;
        this.hour = hour;
        this.minute = minute;
        this.duration = duration;
        this.enabled = enabled;
        this.time = String.format(java.util.Locale.getDefault(), "%02d:%02d", hour, minute);
    }

    // Getter dan Setter
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public int getHour() { return hour; }
    public void setHour(int hour) { this.hour = hour; }

    public int getMinute() { return minute; }
    public void setMinute(int minute) { this.minute = minute; }

    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
}
