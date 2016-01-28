package com.hsbsoftwares.android.app.healthdiagnostic.db.model;

/**
 * Created by Bigsony on 21/11/2015.
 */
public class MonthlyAverage {
    int id;
    String month;
    String averageCrisisDuration;

    public MonthlyAverage() {
    }

    public MonthlyAverage(int id, String month, String averageCrisisDuration) {
        this.id = id;
        this.month = month;
        this.averageCrisisDuration = averageCrisisDuration;
    }

    public MonthlyAverage(String month, String averageCrisisDuration) {
        this.month = month;
        this.averageCrisisDuration = averageCrisisDuration;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public String getAverageCrisisDuration() {
        return averageCrisisDuration;
    }

    public void setAverageCrisisDuration(String averageCrisisDuration) {
        this.averageCrisisDuration = averageCrisisDuration;
    }
}
