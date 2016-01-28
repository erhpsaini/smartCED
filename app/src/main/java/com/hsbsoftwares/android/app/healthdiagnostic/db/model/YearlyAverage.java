package com.hsbsoftwares.android.app.healthdiagnostic.db.model;

/**
 * Created by Bigsony on 21/11/2015.
 */
public class YearlyAverage {
    int id;
    String year;
    String averageCrisisDuration;

    public YearlyAverage() {
    }

    public YearlyAverage(int id, String year, String averageCrisisDuration) {
        this.id = id;
        this.year = year;
        this.averageCrisisDuration = averageCrisisDuration;
    }

    public YearlyAverage(String year, String averageCrisisDuration) {
        this.year = year;
        this.averageCrisisDuration = averageCrisisDuration;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public String getAverageCrisisDuration() {
        return averageCrisisDuration;
    }

    public void setAverageCrisisDuration(String averageCrisisDuration) {
        this.averageCrisisDuration = averageCrisisDuration;
    }
}
