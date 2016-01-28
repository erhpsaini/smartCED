package com.hsbsoftwares.android.app.healthdiagnostic.db.model;

/**
 * Created by Bigsony on 17/11/2015.
 */
public class NumberCrisisPerYear {
    int id;
    String year;
    int numberOfCrisis;

    public NumberCrisisPerYear() {
    }

    public NumberCrisisPerYear(int id, String year, int numberOfCrisis) {
        this.id = id;
        this.year = year;
        this.numberOfCrisis = numberOfCrisis;
    }

    public NumberCrisisPerYear(String year, int numberOfCrisis) {
        this.year = year;
        this.numberOfCrisis = numberOfCrisis;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getyear() {
        return year;
    }

    public void setyear(String year) {
        this.year = year;
    }

    public int getNumberOfCrisis() {
        return numberOfCrisis;
    }

    public void setNumberOfCrisis(int numberOfCrisis) {
        this.numberOfCrisis = numberOfCrisis;
    }
}

