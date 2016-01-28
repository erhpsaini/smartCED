package com.hsbsoftwares.android.app.healthdiagnostic.db.model;

/**
 * Created by Bigsony on 17/11/2015.
 */
public class NumberCrisisPerMonth {
    int id;
    String month;
    int numberOfCrisis;

    public NumberCrisisPerMonth() {
    }

    public NumberCrisisPerMonth(int id, String month, int numberOfCrisis) {
        this.id = id;
        this.month = month;
        this.numberOfCrisis = numberOfCrisis;
    }

    public NumberCrisisPerMonth(String month, int numberOfCrisis) {
        this.month = month;
        this.numberOfCrisis = numberOfCrisis;
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

    public int getNumberOfCrisis() {
        return numberOfCrisis;
    }

    public void setNumberOfCrisis(int numberOfCrisis) {
        this.numberOfCrisis = numberOfCrisis;
    }
}