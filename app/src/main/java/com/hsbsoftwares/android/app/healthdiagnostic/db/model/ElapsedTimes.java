package com.hsbsoftwares.android.app.healthdiagnostic.db.model;

/**
 * Created by Bigsony on 17/11/2015.
 */
public class ElapsedTimes {
    int id;
    String elapsedTimes;

    public ElapsedTimes() {
    }

    public ElapsedTimes(String elapsedTimes) {
        this.elapsedTimes = elapsedTimes;
    }

    public ElapsedTimes(int id, String elapsedTimes) {
        this.id = id;
        this.elapsedTimes = elapsedTimes;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getElapsedTimes() {
        return elapsedTimes;
    }

    public void setElapsedTimes(String elapsedTimes) {
        this.elapsedTimes = elapsedTimes;
    }
}
