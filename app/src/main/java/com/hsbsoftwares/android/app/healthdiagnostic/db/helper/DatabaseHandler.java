package com.hsbsoftwares.android.app.healthdiagnostic.db.helper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.hsbsoftwares.android.app.healthdiagnostic.db.model.Crisi;
import com.hsbsoftwares.android.app.healthdiagnostic.db.model.DailyAverage;
import com.hsbsoftwares.android.app.healthdiagnostic.db.model.ElapsedTimes;
import com.hsbsoftwares.android.app.healthdiagnostic.db.model.MonthlyAverage;
import com.hsbsoftwares.android.app.healthdiagnostic.db.model.NumberCrisisPerDay;
import com.hsbsoftwares.android.app.healthdiagnostic.db.model.NumberCrisisPerMonth;
import com.hsbsoftwares.android.app.healthdiagnostic.db.model.NumberCrisisPerState;
import com.hsbsoftwares.android.app.healthdiagnostic.db.model.NumberCrisisPerYear;
import com.hsbsoftwares.android.app.healthdiagnostic.db.model.YearlyAverage;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by Bigsony on 17/11/2015.
 */
public class DatabaseHandler extends SQLiteOpenHelper {

    // All Static variables
    private static final String TAG = "DatabaseHandler";
    private static Context mContext;
    private Address address;
    // Database Version
    private static final int DATABASE_VERSION = 1;

    // Database Name
    private static final String DATABASE_NAME = "crisisManager";

    // Crisis table name
    private static final String TABLE_CRISIS = "crisis";
    //Singleton
    private static DatabaseHandler instance;
    public static DatabaseHandler getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHandler(context.getApplicationContext());
        }
        return instance;
    }

    // Crisis Table Columns names
    private static final String KEY_ID = "id";
    private static final String KEY_START_DATE = "start_date";
    private static final String KEY_END_DATE = "end_date";
    private static final String KEY_LATITUDE = "latitude";
    private static final String KEY_LONGITUDE = "longitude";
    private static final String KEY_LOCALITY = "locality";
    private static final String KEY_COUNTRY = "country";

    public DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mContext = context;
    }

    // Creating Tables
    public void onCreate(SQLiteDatabase db) {
        String CREATE_CRISIS_TABLE = "CREATE TABLE " + TABLE_CRISIS + "("
                + KEY_ID + " INTEGER PRIMARY KEY,"
                + KEY_START_DATE + " TEXT,"
                + KEY_END_DATE + " TEXT,"
                + KEY_LATITUDE + " DOUBLE,"
                + KEY_LONGITUDE + " DOUBLE,"
                + KEY_LOCALITY + " TEXT,"
                + KEY_COUNTRY + " TEXT" + ")";
        db.execSQL(CREATE_CRISIS_TABLE);
    }

    // Upgrading database
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CRISIS);

        // Create tables again
        onCreate(db);
    }

    /**
     * All CRUD(Create, Read, Update, Delete) Operations
     */

    // Adding new crisi
    public void addCrisi(Crisi crisi) {
        Log.d("Add Crisi", crisi.toString());
        //SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_START_DATE, crisi.getStartDate()); // Start Date
        values.put(KEY_END_DATE, crisi.getEndDate()); // End Date
        values.put(KEY_LATITUDE, crisi.getLatitude()); // Latitude
        values.put(KEY_LONGITUDE, crisi.getLongitude()); // Longitude
        values.put(KEY_LOCALITY, crisi.getLocality()); // Locality
        values.put(KEY_COUNTRY, crisi.getCountry()); // Country

        // Inserting Row Async Inserting
        //AsyncTask is anonymous class
        new AsyncTask<ContentValues, Void, Void>() {
            @Override
            protected Void doInBackground(ContentValues... params) {

                // 1. get reference to writable DB
                SQLiteDatabase db = DatabaseHandler.this.getWritableDatabase();

                // 3. insert
                db.insert(TABLE_CRISIS, null, params[0]);

                // 4. close
                db.close();
                return null;
            }

            protected void onPostExecute(){
                Log.d(TAG, "Executing onPostExecute...");
                Toast.makeText(mContext, "Save.", Toast.LENGTH_LONG);
            }
        }.execute(values);
        //db.insert(TABLE_CRISIS, null, values);
        //db.close(); // Closing database connection
    }

    // Getting All Crisis
    public List<Crisi> getAllCrisis() {
        List<Crisi> crisiList = new ArrayList<Crisi>();
        // Select All Query
        String selectQuery = "SELECT  * FROM " + TABLE_CRISIS;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                Crisi crisi = new Crisi();
                crisi.setId(Integer.parseInt(cursor.getString(0)));
                //crisi.setName(cursor.getString(1));
                //crisi.setPhoneNumber(cursor.getString(2));
                crisi.setStartDate(cursor.getString(1));
                crisi.setEndDate(cursor.getString(2));
                crisi.setLatitude(Double.parseDouble(cursor.getString(3)));
                crisi.setLongitude(Double.parseDouble(cursor.getString(4)));
                crisi.setLocality(cursor.getString(5));
                crisi.setCountry(cursor.getString(6));
                // Adding crisi to list
                crisiList.add(crisi);
            } while (cursor.moveToNext());
        }

        // return crisi list
        return crisiList;
    }

    // Updating single crisi
    public int updateCrisi(Crisi crisi) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_START_DATE, crisi.getStartDate());
        values.put(KEY_END_DATE, crisi.getEndDate());
        values.put(KEY_LATITUDE, crisi.getLatitude()); // Latitude
        values.put(KEY_LONGITUDE, crisi.getLongitude()); // Longitude
        values.put(KEY_LOCALITY, crisi.getLocality()); // Locality
        values.put(KEY_COUNTRY, crisi.getCountry()); // Country

        // updating row
        return db.update(TABLE_CRISIS, values, KEY_ID + " = ?",
                new String[] { String.valueOf(crisi.getId()) });
    }
    // Updating single crisi
    public int updateCrisi2(Crisi crisi) {
        String locality = null;
        String country = null;
        SQLiteDatabase db = this.getWritableDatabase();

        try {
            address = getAddress(crisi.getLatitude(), crisi.getLongitude());
            locality = address.getLocality();
            country = address.getCountryName();
        } catch (IOException e) {
            e.printStackTrace();
        }

        ContentValues values = new ContentValues();
        values.put(KEY_START_DATE, crisi.getStartDate());
        values.put(KEY_END_DATE, crisi.getEndDate());
        values.put(KEY_LATITUDE, crisi.getLatitude()); // Latitude
        values.put(KEY_LONGITUDE, crisi.getLongitude()); // Longitude
        values.put(KEY_LOCALITY, locality); // Locality
        values.put(KEY_COUNTRY, country); // Country

        // updating row
        return db.update(TABLE_CRISIS, values, KEY_ID + " = ?",
                new String[] { String.valueOf(crisi.getId()) });
    }

    // Deleting single crisi
    public void deleteCrisi(Crisi crisi) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_CRISIS, KEY_ID + " = ?",
                new String[]{String.valueOf(crisi.getId())});
        db.close();
    }

    // Getting crisis Count
    public int getCrisisCount() {
        String countQuery = "SELECT  * FROM " + TABLE_CRISIS;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        cursor.close();

        // return count
        return cursor.getCount();
    }

    public List<ElapsedTimes> getElapsedTimes() {
        List<ElapsedTimes> elapsedTimesList = new ArrayList<ElapsedTimes>();
        // Select All Query
        //String selectQuery = "ID, TIMEDIFF(EndDate, StartDate) AS CrisisDuration FROM " + TABLE_CRISIS;
        //SELECT ID, TIMEDIFF(EndDate, StartDate) AS CrisisDuration FROM crisis
        String selectQuery = "SELECT " + KEY_ID + ", (STRFTIME ('%s', "
                + KEY_END_DATE + ") - STRFTIME ('%s', " + KEY_START_DATE + ")) AS CrisisDuration FROM " + TABLE_CRISIS;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                ElapsedTimes elapsedTimes = new ElapsedTimes();
                elapsedTimes.setId(Integer.parseInt(cursor.getString(0)));
                elapsedTimes.setElapsedTimes(cursor.getString(1));
                // Adding crisi to list
                elapsedTimesList.add(elapsedTimes);
            } while (cursor.moveToNext());
        }

        // return crisi list
        return elapsedTimesList;
    }

    public List<NumberCrisisPerDay> getNumberCrisisPerDay(String startDate, String endDate) {
        List<NumberCrisisPerDay> numberCrisisPerDayList = new ArrayList<NumberCrisisPerDay>();
        // Select All Query

        //String selectQuery = "SELECT " + KEY_ID + ", (STRFTIME ('%s', "
        //      + KEY_END_DATE + ") - STRFTIME ('%s', " + KEY_START_DATE + ")) AS CrisisDuration FROM " + TABLE_CRISIS;
        //SELECT DATE(startDate) AS Days, COUNT(ID) AS NumberOfCrisis FROM crisi GROUP BY DATE(startDate)
       //Select all crisis
//       String selectQuery = "SELECT STRFTIME('%Y-%m-%d', "
//                + KEY_START_DATE + ") AS Days, COUNT (" + KEY_ID + ") AS NumberOfCrisis FROM "
//                + TABLE_CRISIS + " GROUP BY STRFTIME('%Y-%m-%d'," + KEY_START_DATE + ")";

//        SELECT DATE(startDate) AS Days, COUNT(ID) AS NumberOfCrisis
//        FROM crisi
//        WHERE DATE(`StartDate`) >= '2015-11-06' AND DATE(`StartDate`) <= '2014-11-12'
//        GROUP BY DATE(startDate);
        String selectQuery = "SELECT STRFTIME('%Y-%m-%d', "
                + KEY_START_DATE + ") AS Days, COUNT (" + KEY_ID + ") AS NumberOfCrisis FROM "
                + TABLE_CRISIS + " WHERE STRFTIME('%Y-%m-%d', " + KEY_START_DATE + ") >= STRFTIME('%Y-%m-%d', '" + startDate
                + "') AND STRFTIME('%Y-%m-%d', " + KEY_START_DATE + ") <= STRFTIME('%Y-%m-%d', '" + endDate
                + "') GROUP BY STRFTIME('%Y-%m-%d'," + KEY_START_DATE + ")";

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            int i=1;
            do {
                NumberCrisisPerDay numberCrisisPerDay = new NumberCrisisPerDay();
                numberCrisisPerDay.setId(i);
                numberCrisisPerDay.setDays(cursor.getString(0));
                numberCrisisPerDay.setNumberOfCrisis(Integer.parseInt(cursor.getString(1)));
                // Adding crisi to list
                numberCrisisPerDayList.add(numberCrisisPerDay);
                i++;
            } while (cursor.moveToNext());
        }
        db.close();

        // return crisi list
        return numberCrisisPerDayList;
    }

    public List<NumberCrisisPerMonth> getNumberCrisisPerMonth(String startDate, String endDate) {
        List<NumberCrisisPerMonth> numberCrisisPerMonthList = new ArrayList<NumberCrisisPerMonth>();
        // Select All Query

        //SELECT monthname(startDate) AS Month, COUNT(ID) AS NumberOfCrisis
        //FROM crisi WHERE YEAR (`StartDate`) = '2015'
        //GROUP BY monthname(startDate);
        String selectQuery = "SELECT STRFTIME('%Y-%m', "
                + KEY_START_DATE + ") AS Month, COUNT (" + KEY_ID + ") AS NumberOfCrisis FROM "
                + TABLE_CRISIS +  " WHERE STRFTIME('%Y-%m-%d', " + KEY_START_DATE + ") >= STRFTIME('%Y-%m-%d', '" + startDate
                + "') AND STRFTIME('%Y-%m-%d', " + KEY_START_DATE + ") <= STRFTIME('%Y-%m-%d', '" + endDate
                + "') GROUP BY STRFTIME('%Y-%m', " + KEY_START_DATE + ")";

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            int i=1;
            do {
                NumberCrisisPerMonth numberCrisisPerMonth = new NumberCrisisPerMonth();
                numberCrisisPerMonth.setId(i);
                numberCrisisPerMonth.setMonth(cursor.getString(0));
                numberCrisisPerMonth.setNumberOfCrisis(Integer.parseInt(cursor.getString(1)));
                // Adding crisi to list
                numberCrisisPerMonthList.add(numberCrisisPerMonth);
                i++;
            } while (cursor.moveToNext());
        }
        db.close();

        // return crisi list
        return numberCrisisPerMonthList;
    }

    public List<NumberCrisisPerYear> getNumberCrisisPerYear(String startDate, String endDate) {
        List<NumberCrisisPerYear> numberCrisisPerYearList = new ArrayList<NumberCrisisPerYear>();
        // Select All Query

        //SELECT YEAR(startDate) AS Year, COUNT(ID) AS NumberOfCrisis
        //FROM crisi
        //GROUP BY YEAR(startDate);
        /*
        String selectQuery = "SELECT STRFTIME('%Y-%m-%d', "
                + KEY_START_DATE + ") AS Days, COUNT (" + KEY_ID + ") AS NumberOfCrisis FROM "
                + TABLE_CRISIS + " GROUP BY STRFTIME('%Y-%m-%d'," + KEY_START_DATE + ")";
        */
//        String selectQuery = "SELECT STRFTIME('%Y', "
//                + KEY_START_DATE + ") AS Year, COUNT (" + KEY_ID + ") AS NumberOfCrisis FROM "
//                + TABLE_CRISIS + " GROUP BY STRFTIME('%Y'," + KEY_START_DATE + ")";

        String selectQuery = "SELECT STRFTIME('%Y', "
                + KEY_START_DATE + ") AS Year, COUNT (" + KEY_ID + ") AS NumberOfCrisis FROM "
                + TABLE_CRISIS + " WHERE STRFTIME('%Y', " + KEY_START_DATE + ") >= STRFTIME('%Y-%m-%d', '" + startDate
                + "') AND STRFTIME('%Y-%m-%d', " + KEY_START_DATE + ") <= STRFTIME('%Y-%m-%d', '" + endDate
                + "') GROUP BY STRFTIME('%Y'," + KEY_START_DATE + ")";


        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            int i=1;
            do {
                NumberCrisisPerYear numberCrisisPerYear = new NumberCrisisPerYear();
                numberCrisisPerYear.setId(i);
                numberCrisisPerYear.setyear(cursor.getString(0));
                numberCrisisPerYear.setNumberOfCrisis(Integer.parseInt(cursor.getString(1)));
                // Adding crisi to list
                numberCrisisPerYearList.add(numberCrisisPerYear);
                i++;
            } while (cursor.moveToNext());
        }
        db.close();

        // return crisi list
        return numberCrisisPerYearList;
    }

    public List<NumberCrisisPerState> getNumberCrisisPerState() {
        List<NumberCrisisPerState> numberCrisisPerStateList = new ArrayList<NumberCrisisPerState>();
        Address address =null;
        // Select All Query

        //String selectQuery = "SELECT " + KEY_ID + ", (STRFTIME ('%s', "
        //      + KEY_END_DATE + ") - STRFTIME ('%s', " + KEY_START_DATE + ")) AS CrisisDuration FROM " + TABLE_CRISIS;
        //SELECT DATE(startDate) AS Days, COUNT(ID) AS NumberOfCrisis FROM crisi GROUP BY DATE(startDate)
        //String selectQuery = "SELECT STRFTIME('%Y-%m-%d', "
        //+ KEY_START_DATE + ") AS Days, COUNT (" + KEY_ID + ") AS NumberOfCrisis FROM "
        //        + TABLE_CRISIS + " GROUP BY STRFTIME('%Y-%m-%d'," + KEY_START_DATE + ")";
        //SELECT latitude, longitude, COUNT(ID) AS NumberOfCrisis
        //FROM crisi
        //GROUP BY latitude, longitude;

//        String selectQuery =  "SELECT " + KEY_LATITUDE + ", " + KEY_LONGITUDE
//                + ", COUNT (" + KEY_ID + ") AS NumberOfCrisis FROM " + TABLE_CRISIS
//                + " GROUP BY " + KEY_LATITUDE + ", " + KEY_LONGITUDE;
        String selectQuery =  "SELECT " + KEY_COUNTRY
                + ", COUNT (" + KEY_ID + ") AS NumberOfCrisis FROM " + TABLE_CRISIS
                + " GROUP BY " + KEY_COUNTRY;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            int i=1;
            do {
                NumberCrisisPerState numberCrisisPerState = new NumberCrisisPerState();
                numberCrisisPerState.setId(i);
                numberCrisisPerState.setCountryName(cursor.getString(0));
                //numberCrisisPerState.setLatitude(Double.parseDouble(cursor.getString(0)));
                //numberCrisisPerState.setLongitude(Double.parseDouble(cursor.getString(1)));
               /* try {
                    address = getAddress(Double.parseDouble(cursor.getString(0)), Double.parseDouble(cursor.getString(0)));
                    Log.d("Log: ", address.getAddressLine(0));
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }*/
                //String CountryName = address.getAddressLine(0);
                //numberCrisisPerState.setCountryName(CountryName);
                //numberCrisisPerState.setLocality(address.getLocality());
                numberCrisisPerState.setNumberOfCrisis(Integer.parseInt(cursor.getString(1)));
                //numberCrisisPerState.setNumberOfCrisis(Integer.parseInt(cursor.getString(1)));
                // Adding crisi to list
                numberCrisisPerStateList.add(numberCrisisPerState);
                i++;
            } while (cursor.moveToNext());
        }
        db.close();

        // return crisi list
        return numberCrisisPerStateList;
    }

    public List<DailyAverage> getDailyAverage(String startDate, String endDate){
        List<DailyAverage> dailyAverageList = new ArrayList<>();

        //SELECT DATE(`StartDate`) AS Days,
        //SEC_TO_TIME(AVG(TIMEDIFF(EndDate, StartDate))) AS AverageCrisisDuration,
        //COUNT(ID) AS NumberOfCrisis
        //FROM crisi
        //GROUP BY DATE(`StartDate`)
//        String selectQuery = "SELECT STRFTIME('%Y-%m-%d', "
//                + KEY_START_DATE + ") AS Days, (AVG(STRFTIME ('%s', " + KEY_END_DATE
//                + ") - STRFTIME ('%s', " + KEY_START_DATE + ")))/60 AS AverageCrisisDuration, COUNT (" + KEY_ID
//                + ") AS NumberOfCrisis FROM "
//                + TABLE_CRISIS + " GROUP BY STRFTIME('%Y-%m-%d'," + KEY_START_DATE + ")";

//        SELECT DATE(`StartDate`) AS Days,
//        SEC_TO_TIME(AVG(TIMEDIFF(EndDate, StartDate))) AS AverageCrisisDuration
//        FROM crisi
//        WHERE `StartDate` >= '2014-11-05' AND `StartDate` <= '2015-11-06'
//        GROUP BY DATE(`StartDate`)

        String selectQuery = "SELECT STRFTIME('%Y-%m-%d', "
                + KEY_START_DATE + ") AS Days, (AVG(STRFTIME ('%s', " + KEY_END_DATE
                + ") - STRFTIME ('%s', " + KEY_START_DATE + ")))/60 AS AverageCrisisDuration FROM "
                + TABLE_CRISIS + " WHERE STRFTIME('%Y-%m-%d', " + KEY_START_DATE + ") >= STRFTIME('%Y-%m-%d', '" + startDate
                + "') AND STRFTIME('%Y-%m-%d', " + KEY_START_DATE + ") <= STRFTIME('%Y-%m-%d', '" + endDate
                + "') GROUP BY STRFTIME('%Y-%m-%d'," + KEY_START_DATE + ")";

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            int i=1;
            do {
                DailyAverage dailyAverage = new DailyAverage();
                dailyAverage.setId(i);
                dailyAverage.setDays(cursor.getString(0));
                dailyAverage.setAverageCrisisDuration(cursor.getString(1));
                //dailyAverage.setNumberOfCrisis(Integer.parseInt(cursor.getString(2)));
                // Adding crisi to list
                dailyAverageList.add(dailyAverage);
                i++;
            } while (cursor.moveToNext());
        }
        db.close();
        // return crisi list
        return dailyAverageList;
    }
    public List<MonthlyAverage> getMonthlyAverage(String startDate, String endDate){
        List<MonthlyAverage> monthlyAverageList = new ArrayList<>();

        //SELECT DATE(`StartDate`) AS Days,
        //SEC_TO_TIME(AVG(TIMEDIFF(EndDate, StartDate))) AS AverageCrisisDuration,
        //COUNT(ID) AS NumberOfCrisis
        //FROM crisi
        //GROUP BY DATE(`StartDate`)
//        String selectQuery = "SELECT STRFTIME('%Y-%m-%d', "
//                + KEY_START_DATE + ") AS Days, (AVG(STRFTIME ('%s', " + KEY_END_DATE
//                + ") - STRFTIME ('%s', " + KEY_START_DATE + ")))/60 AS AverageCrisisDuration, COUNT (" + KEY_ID
//                + ") AS NumberOfCrisis FROM "
//                + TABLE_CRISIS + " GROUP BY STRFTIME('%Y-%m-%d'," + KEY_START_DATE + ")";

//        SELECT DATE(`StartDate`) AS Days,
//        SEC_TO_TIME(AVG(TIMEDIFF(EndDate, StartDate))) AS AverageCrisisDuration
//        FROM crisi
//        WHERE `StartDate` >= '2014-11-05' AND `StartDate` <= '2015-11-06'
//        GROUP BY DATE(`StartDate`)

        String selectQuery = "SELECT STRFTIME('%Y-%m-%d', "
                + KEY_START_DATE + ") AS Days, (AVG(STRFTIME ('%s', " + KEY_END_DATE
                + ") - STRFTIME ('%s', " + KEY_START_DATE + ")))/60 AS AverageCrisisDuration FROM "
                + TABLE_CRISIS + " WHERE STRFTIME('%Y-%m-%d', " + KEY_START_DATE + ") >= STRFTIME('%Y-%m-%d', '" + startDate
                + "') AND STRFTIME('%Y-%m-%d', " + KEY_START_DATE + ") <= STRFTIME('%Y-%m-%d', '" + endDate
                + "') GROUP BY STRFTIME('%Y-%m-%d'," + KEY_START_DATE + ")";

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            int i=1;
            do {
                MonthlyAverage monthlyAverage = new MonthlyAverage();
                monthlyAverage.setId(i);
                monthlyAverage.setMonth(cursor.getString(0));
                monthlyAverage.setAverageCrisisDuration(cursor.getString(1));
                //dailyAverage.setNumberOfCrisis(Integer.parseInt(cursor.getString(2)));
                // Adding crisi to list
                monthlyAverageList.add(monthlyAverage);
                i++;
            } while (cursor.moveToNext());
        }
        db.close();
        // return crisi list
        return monthlyAverageList;
    }
    public List<YearlyAverage> getYearlyAverage(String startDate, String endDate){
        List<YearlyAverage> yearlyAverageList = new ArrayList<YearlyAverage>();

        //SELECT DATE(`StartDate`) AS Days,
        //SEC_TO_TIME(AVG(TIMEDIFF(EndDate, StartDate))) AS AverageCrisisDuration,
        //COUNT(ID) AS NumberOfCrisis
        //FROM crisi
        //GROUP BY DATE(`StartDate`)
//        String selectQuery = "SELECT STRFTIME('%Y-%m-%d', "
//                + KEY_START_DATE + ") AS Days, (AVG(STRFTIME ('%s', " + KEY_END_DATE
//                + ") - STRFTIME ('%s', " + KEY_START_DATE + ")))/60 AS AverageCrisisDuration, COUNT (" + KEY_ID
//                + ") AS NumberOfCrisis FROM "
//                + TABLE_CRISIS + " GROUP BY STRFTIME('%Y-%m-%d'," + KEY_START_DATE + ")";

//        SELECT DATE(`StartDate`) AS Days,
//        SEC_TO_TIME(AVG(TIMEDIFF(EndDate, StartDate))) AS AverageCrisisDuration
//        FROM crisi
//        WHERE `StartDate` >= '2014-11-05' AND `StartDate` <= '2015-11-06'
//        GROUP BY DATE(`StartDate`)

        String selectQuery = "SELECT STRFTIME('%Y', "
                + KEY_START_DATE + ") AS Year, (AVG(STRFTIME ('%s', " + KEY_END_DATE
                + ") - STRFTIME ('%s', " + KEY_START_DATE + ")))/60 AS AverageCrisisDuration FROM "
                + TABLE_CRISIS + " WHERE STRFTIME('%Y', " + KEY_START_DATE + ") >= STRFTIME('%Y', '" + startDate
                + "') AND STRFTIME('%Y', " + KEY_START_DATE + ") <= STRFTIME('%Y', '" + endDate
                + "') GROUP BY STRFTIME('%Y'," + KEY_START_DATE + ")";

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            int i=1;
            do {
                YearlyAverage yearlyAverage = new YearlyAverage();
                yearlyAverage.setId(i);
                yearlyAverage.setYear(cursor.getString(0));
                yearlyAverage.setAverageCrisisDuration(cursor.getString(1));
                //dailyAverage.setNumberOfCrisis(Integer.parseInt(cursor.getString(2)));
                // Adding crisi to list
                yearlyAverageList.add(yearlyAverage);
                i++;
            } while (cursor.moveToNext());
        }
        db.close();
        // return crisi list
        return yearlyAverageList;
    }

    /***
     * get datetime
     ***/
    private String getDateTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        Date date = new Date();
        return dateFormat.format(date);
    }

    public Address getAddress(double latitude, double longitude) throws IOException {
        if (latitude == 0 || longitude == 0) {
            return new Address(null);
        }
        Geocoder gc=new Geocoder(mContext);
        Address address=null;
        List<Address> addresses=gc.getFromLocation(latitude,longitude,1);
        if (addresses.size() > 0) {
            address=addresses.get(0);
        }
        return address;
    }

}

