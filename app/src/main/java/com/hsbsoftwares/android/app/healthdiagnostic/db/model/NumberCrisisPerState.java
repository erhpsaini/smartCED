package com.hsbsoftwares.android.app.healthdiagnostic.db.model;

/**
 * Created by Bigsony on 17/11/2015.
 */
public class NumberCrisisPerState {
    int id;
    double latitude;
    double longitude;
    String locality;
    String countryName;
    int numberOfCrisis;

    public NumberCrisisPerState() {
    }

    public NumberCrisisPerState(int id, double latitude, double longitude, String locality, String countryName, int numberOfCrisis) {
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
        this.locality = locality;
        this.countryName = countryName;
        this.numberOfCrisis = numberOfCrisis;
    }

    public NumberCrisisPerState(double latitude, double longitude, String locality, String countryName, int numberOfCrisis) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.locality = locality;
        this.countryName = countryName;
        this.numberOfCrisis = numberOfCrisis;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public String getCountryName() {
        return countryName;
    }

    public void setCountryName(String countryName) {
        this.countryName = countryName;
    }

    public int getNumberOfCrisis() {
        return numberOfCrisis;
    }

    public void setNumberOfCrisis(int numberOfCrisis) {
        this.numberOfCrisis = numberOfCrisis;
    }
}

