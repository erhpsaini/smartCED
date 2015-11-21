package com.hsbsoftwares.android.app.healthdiagnostic.db.model;

/**
 * Created by Bigsony on 17/11/2015.
 */
public class Crisi {
    //private variables
    int id;
    String startDate;
    String endDate;
    double latitude;
    double longitude;
    String locality;
    String country;
    String ElapsedTime;
    //String _location_image;

    // Empty constructor
    public Crisi() {
    }

    public Crisi(int id, String startDate, String endDate, double latitude, double longitude, String locality, String country) {
        this.id = id;
        this.startDate = startDate;
        this.endDate = endDate;
        this.latitude = latitude;
        this.longitude = longitude;
        this.locality = locality;
        this.country = country;
    }

    public Crisi(String startDate, String endDate, double latitude, double longitude, String locality, String country) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.latitude = latitude;
        this.longitude = longitude;
        this.locality = locality;
        this.country = country;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getLocality() {
        return locality;
    }

    public void setLocality(String locality) {
        this.locality = locality;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getElapsedTime() {
        return ElapsedTime;
    }

    public void setElapsedTime(String elapsedTime) {
        ElapsedTime = elapsedTime;
    }
}
