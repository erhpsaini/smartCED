package com.hsbsoftwares.android.app.healthdiagnostic.db.model;

/**
 * Created by Bigsony on 17/11/2015.
 */
public class DailyAverage {
    int id;
    String days;
    String averageCrisisDuration;

    public DailyAverage() {
    }

    public DailyAverage(int id, String days, String averageCrisisDuration) {
        this.id = id;
        this.days = days;
        this.averageCrisisDuration = averageCrisisDuration;
    }

    public DailyAverage(String days, String averageCrisisDuration) {
        this.days = days;
        this.averageCrisisDuration = averageCrisisDuration;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDays() {
        return days;
    }

    public void setDays(String days) {
        this.days = days;
    }

    public String getAverageCrisisDuration() {
        return averageCrisisDuration;
    }

    public void setAverageCrisisDuration(String averageCrisisDuration) {
        this.averageCrisisDuration = averageCrisisDuration;
    }
}

