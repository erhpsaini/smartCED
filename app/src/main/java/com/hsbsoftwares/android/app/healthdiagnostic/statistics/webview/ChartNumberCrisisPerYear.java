package com.hsbsoftwares.android.app.healthdiagnostic.statistics.webview;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;

import com.hsbsoftwares.android.app.healthdiagnostic.R;
import com.hsbsoftwares.android.app.healthdiagnostic.db.helper.DatabaseHandler;
import com.hsbsoftwares.android.app.healthdiagnostic.db.model.DailyAverage;
import com.hsbsoftwares.android.app.healthdiagnostic.db.model.NumberCrisisPerDay;
import com.hsbsoftwares.android.app.healthdiagnostic.db.model.NumberCrisisPerYear;
import com.hsbsoftwares.android.app.healthdiagnostic.db.model.YearlyAverage;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChartNumberCrisisPerYear extends Activity implements
        View.OnClickListener {

    //UI References
    private EditText fromDateEtxt;
    private EditText toDateEtxt;
    private Button NumberCrisis;
    private Button AverageDuration;

    private DatePickerDialog fromDatePickerDialog;
    private DatePickerDialog toDatePickerDialog;

    private SimpleDateFormat dateFormatter;

    WebView webView;
    DatabaseHandler databaseHandler = new DatabaseHandler(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Adds Progrss bar Support
        this.getWindow().requestFeature(Window.FEATURE_PROGRESS);
        setContentView(R.layout.activity_chart_number_crisis);

        // Makes Progress bar Visible
        getWindow().setFeatureInt(Window.FEATURE_PROGRESS, Window.PROGRESS_VISIBILITY_ON);

        //dateFormatter = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
        //dateFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        dateFormatter = new SimpleDateFormat("yyyy", Locale.getDefault());

        findViewsById();

        setDateTimeField();

        webView = (WebView)findViewById(R.id.web_view);
        webView.getSettings().setJavaScriptEnabled(true);
        WebSettings webSettings = webView.getSettings();
        webSettings.setBuiltInZoomControls(true);

        // Sets the Chrome Client, and defines the onProgressChanged
        // This makes the Progress bar be updated.
        final Activity MyActivity = this;
        webView.setWebChromeClient(new WebChromeClient() {
            public void onProgressChanged(WebView view, int progress) {
                //Make the bar disappear after URL is loaded, and changes string to Loading...
                MyActivity.setTitle("Loading...");
                MyActivity.setProgress(progress * 100); //Make the bar disappear after URL is loaded

                // Return the app name after finish loading
                if (progress == 100)
                    MyActivity.setTitle(R.string.app_name);
            }
        });
        setupActionBar();
    }

    private void loadChartAverageDuration(String startDate, String endDate){
        databaseHandler = DatabaseHandler.getInstance(this);
        List<YearlyAverage> yearlyAverage = null;
        try {
            yearlyAverage = databaseHandler.getYearlyAverage(startDate, endDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        for(YearlyAverage ya : yearlyAverage){
            String log = "Id: " + ya.getId() + " Year: " + ya.getYear()
                    + " Average Duration: " + ya.getAverageCrisisDuration();
            Log.d("Name: ", log);
        }

        StringBuilder html = new StringBuilder();

        if(!isOnline()){
            html.append("<html>");
            html.append("<head>");
            html.append("</head>");
            html.append("<body><p>");
            html.append("No network connection.</br>Please check your network connection and try again.");
            html.append("</p></body></html>");
            webView.loadDataWithBaseURL("file:///android_asset/", html.toString(), "text/html", "UTF-8", "");
            webView.requestFocusFromTouch();
        }else if (yearlyAverage.isEmpty()){
            html.append("<html>");
            html.append("<head>");
            html.append("<link rel=\"stylesheet\" href=\"css/main.css\" />");
            html.append("</head>");
            html.append("<body>");
            html.append("<p>No data!</br>Try another date</p>");
            html.append("</body></html>");
            webView.loadDataWithBaseURL("file:///android_asset/", html.toString(), "text/html", "UTF-8", "");
            webView.requestFocusFromTouch();
        }
        else {
            html.append("<html>");
            html.append("<head>");
            html.append("<link rel=\"stylesheet\" href=\"assets/css/main.css\" />");
            html.append("<script type=\"text/javascript\" src=\"https://www.google.com/jsapi\"></script>");
            html.append("<script type=\"text/javascript\">");

            html.append("google.load(\"visualization\", \"1.0\", {\"packages\":[\"corechart\"]});");
            html.append("google.setOnLoadCallback(drawChart);");
            html.append("function drawChart() {");
            html.append("var data = google.visualization.arrayToDataTable([");
            html.append("['Day', 'AverageCrisisduration'],");
            for (YearlyAverage ya : yearlyAverage) {
                html.append("['");
                html.append(ya.getYear());
                html.append("', ");
                html.append(ya.getAverageCrisisDuration());
                html.append("],");
            }
            html.deleteCharAt(html.length() - 1);
            html.append("]);");
            html.append("var options = {title: 'Yearly Average', legend: { position: 'none' }, " +
                    "hAxis: {title: 'Year',  titleTextStyle: {color: '#333'}}, " +
                    "vAxis: {title: 'Time in minute',  titleTextStyle: {color: '#333'}, minValue: 0}};");
            html.append("var chart = new google.visualization.AreaChart(document.getElementById('chart_div'));");
            html.append("chart.draw(data, options);");
            html.append("}</script>");
            html.append("</head>");
            html.append("<body>");
            html.append("<div id=\"chart_div\" style=\"width: 300px; height: 220px;\"></div>");
            html.append("</body></html>");


            webView.loadDataWithBaseURL("file:///android_asset/", html.toString(), "text/html", "UTF-8", "");
            webView.requestFocusFromTouch();
        }
    }
    private void loadChartNumberCrisis(String startDate, String endDate){
        databaseHandler = DatabaseHandler.getInstance(this);
        List<NumberCrisisPerYear> numberCrisisPerYear = null;
        try {
            numberCrisisPerYear = databaseHandler.getNumberCrisisPerYear(startDate, endDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }


        for(NumberCrisisPerYear ncpy : numberCrisisPerYear){
            String log = "Id: " + ncpy.getId() + " Year: " + ncpy.getyear()
                    + " Number Of Crisis: " + ncpy.getNumberOfCrisis();
            Log.d("Name: ", log);
        }


        StringBuilder html = new StringBuilder();

        if(!isOnline()){
            html.append("<html>");
            html.append("<head>");
            html.append("</head>");
            html.append("<body><p>");
            html.append("No network connection.</br>Please check your network connection and try again.");
            html.append("</p></body></html>");
            webView.loadDataWithBaseURL("file:///android_asset/", html.toString(), "text/html", "UTF-8", "");
            webView.requestFocusFromTouch();
        }else if (numberCrisisPerYear.isEmpty()){
            html.append("<html>");
            html.append("<head>");
            html.append("<link rel=\"stylesheet\" href=\"css/main.css\" />");
            html.append("</head>");
            html.append("<body>");
            html.append("<p>No data!</br>Try another date</p>");
            html.append("</body></html>");
            webView.loadDataWithBaseURL("file:///android_asset/", html.toString(), "text/html", "UTF-8", "");
            webView.requestFocusFromTouch();
        }
        else {
            html.append("<html>");
            html.append("<head>");
            html.append("<link rel=\"stylesheet\" href=\"assets/css/main.css\" />");
            html.append("<script type=\"text/javascript\" src=\"https://www.google.com/jsapi\"></script>");
            html.append("<script type=\"text/javascript\">");

            html.append("google.load(\"visualization\", \"1.0\", {\"packages\":[\"corechart\"]});");
            html.append("google.setOnLoadCallback(drawChart);");
            html.append("function drawChart() {");
            html.append("var data = google.visualization.arrayToDataTable([");
            html.append("['Year', 'Number Crisis'],");
            for (NumberCrisisPerYear ncpy : numberCrisisPerYear) {
                html.append("['");
                //html.append(String.valueOf(ncpy.getyear()));
                html.append(ncpy.getyear());
                html.append("', ");
                html.append(ncpy.getNumberOfCrisis());
                html.append("],");
            }
            html.deleteCharAt(html.length() - 1);
            html.append("]);");
            html.append(" var options = {title: 'Number of Yearly Crisis', legend: { position: 'none' }, 'width':300, 'height':220, " +
                    "hAxis: {title: 'Year',  titleTextStyle: {color: '#333'}}, " +
                    "vAxis: {title: 'Number of crisis',  titleTextStyle: {color: '#333'}, minValue: 0}};");
            html.append("var chart = new google.visualization.ColumnChart(document.getElementById('chart_div'));");
            html.append("chart.draw(data, options);");
            html.append("}</script>");
            html.append("</head>");
            html.append("<body>");
            html.append("<div id='chart_div'></div>");
            html.append("</body></html>");

            webView.loadDataWithBaseURL("file:///android_asset/", html.toString(), "text/html", "UTF-8", "");
            webView.requestFocusFromTouch();
        }
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

    private void findViewsById() {
        fromDateEtxt = (EditText) findViewById(R.id.etxt_fromdate);
        fromDateEtxt.setInputType(InputType.TYPE_NULL);
        fromDateEtxt.requestFocus();

        NumberCrisis = (Button) findViewById(R.id.number_crisis);
        AverageDuration = (Button) findViewById(R.id.average_duration);

        toDateEtxt = (EditText) findViewById(R.id.etxt_todate);
        toDateEtxt.setInputType(InputType.TYPE_NULL);
    }

    private void setDateTimeField() {
        fromDateEtxt.setOnClickListener(this);
        toDateEtxt.setOnClickListener(this);

        Calendar newCalendar = Calendar.getInstance();
        fromDatePickerDialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {

            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                Calendar newDate = Calendar.getInstance();
                newDate.set(year, monthOfYear, dayOfMonth);
                fromDateEtxt.setText(dateFormatter.format(newDate.getTime()));
            }

        },newCalendar.get(Calendar.YEAR), newCalendar.get(Calendar.MONTH), newCalendar.get(Calendar.DAY_OF_MONTH));

        toDatePickerDialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {

            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                Calendar newDate = Calendar.getInstance();
                newDate.set(year, monthOfYear, dayOfMonth);
                toDateEtxt.setText(dateFormatter.format(newDate.getTime()));
            }

        },newCalendar.get(Calendar.YEAR), newCalendar.get(Calendar.MONTH), newCalendar.get(Calendar.DAY_OF_MONTH));
    }

    public void onClick(View view) {
        //SimpleDateFormat  sdfOut = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        if(view == fromDateEtxt) {
            fromDatePickerDialog.show();
        } else if(view == toDateEtxt) {
            toDatePickerDialog.show();
        } else if(view == NumberCrisis) {
            //toDatePickerDialog.show();
            loadChartNumberCrisis(fromDateEtxt.getText().toString().trim(), toDateEtxt.getText().toString().trim());
        } else if(view == AverageDuration) {
            //toDatePickerDialog.show();
            loadChartAverageDuration(fromDateEtxt.getText().toString().trim(), toDateEtxt.getText().toString().trim());
        }
    }
    public boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        //return netInfo != null && netInfo.isConnectedOrConnecting();
        return netInfo != null && netInfo.isConnected();
    }
}