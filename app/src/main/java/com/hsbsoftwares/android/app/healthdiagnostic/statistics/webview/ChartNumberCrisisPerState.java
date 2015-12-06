package com.hsbsoftwares.android.app.healthdiagnostic.statistics.webview;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.hsbsoftwares.android.app.healthdiagnostic.R;
import com.hsbsoftwares.android.app.healthdiagnostic.db.helper.DatabaseHandler;
import com.hsbsoftwares.android.app.healthdiagnostic.db.model.NumberCrisisPerState;

import java.io.IOException;
import java.util.List;

public class ChartNumberCrisisPerState extends Activity {

    WebView webView;
    DatabaseHandler databaseHandler = new DatabaseHandler(this);
    private static Context mContext;

    //GPSTracker gps = new GPSTracker(ChartNumberCrisisPerState.this);
    //Address address = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);

        webView = (WebView)findViewById(R.id.web);
        webView.getSettings().setJavaScriptEnabled(true);
        WebSettings webSettings = webView.getSettings();
        webSettings.setBuiltInZoomControls(true);


        webView.loadDataWithBaseURL("file:///android_asset/", buildHtml().toString(), "text/html", "UTF-8", "");
        webView.requestFocusFromTouch();
        setupActionBar();
    }

    private StringBuilder buildHtml(){
        databaseHandler = DatabaseHandler.getInstance(this);
        List<NumberCrisisPerState> numberCrisisPerState = databaseHandler.getNumberCrisisPerState();

        StringBuilder html = new StringBuilder();

        html.append("<html>");
        html.append("<head>");
        html.append("<script type='text/javascript' src='https://www.google.com/jsapi'></script>");
        html.append("<script type='text/javascript'>");

        html.append("google.load('visualization', '1', {'packages':['geochart']});");
        html.append("google.setOnLoadCallback(drawMarkersMap);");
        html.append("function drawMarkersMap() {");
        html.append("var data = google.visualization.arrayToDataTable([");
        html.append("['Country', 'Number of Crisis'],");
        for (NumberCrisisPerState ncps  : numberCrisisPerState){
            html.append("['");
            //html.append(ncps.getLatitude());
            html.append(ncps.getCountryName());
            html.append("', ");
            //html.append(ncps.getLongitude());
            //html.append(", ");
            html.append(ncps.getNumberOfCrisis());
            html.append("],");
        }
        html.deleteCharAt(html.length() - 1);
        html.append("]);");
        html.append("var options = {sizeAxis: { minValue: 0, maxValue: 100 }, " +
                "region: 'world', displayMode: 'markers', colorAxis: {colors: ['#96ab90', '#24ac00']}};");
        //html.append("var options = {};");
        html.append("var chart = new google.visualization.GeoChart(document.getElementById('chart_div'));");
        html.append("chart.draw(data, options);");
        html.append("}</script>");
        html.append("</head>");
        html.append("<body>");
        html.append("<div id=\"chart_div\" style=\"width: 900px; height: 500px;\"></div>");
        html.append("</body></html>");

        return html;
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void setupActionBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            // Show the Up button in the action bar.
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            // This ID represents the Home or Up button. In the case of this
            // activity, the Up button is shown. Use NavUtils to allow users
            // to navigate up one level in the application structure.
            //Back button functionality
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public Address getAddress(double latitude, double longitude) throws IOException {
        if (latitude == 0 || longitude == 0) {
            return new Address(null);
        }
        Geocoder gc=new Geocoder(mContext);
        Address address=null;
        List<Address> addresses=gc.getFromLocation(latitude, longitude, 1);
        if (addresses.size() > 0) {
            address=addresses.get(0);
        }
        return address;
    }
}