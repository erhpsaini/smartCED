package com.hsbsoftwares.android.app.healthdiagnostic.db.model;

/**
 * Created by Bigsony on 17/11/2015.
 */
public class NumberCrisisPerDay {
    int id;
    String days;
    int numberOfCrisis;

    public NumberCrisisPerDay() {
    }
    public NumberCrisisPerDay(int id,  String days, int numberOfCrisis) {
        this.id = id;
        this.days = days;
        this.numberOfCrisis = numberOfCrisis;
    }
    public NumberCrisisPerDay(String days, int numberOfCrisis) {
        this.days = days;
        this.numberOfCrisis = numberOfCrisis;
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

    public int getNumberOfCrisis() {
        return numberOfCrisis;
    }

    public void setNumberOfCrisis(int numberOfCrisis) {
        this.numberOfCrisis = numberOfCrisis;
    }
}

