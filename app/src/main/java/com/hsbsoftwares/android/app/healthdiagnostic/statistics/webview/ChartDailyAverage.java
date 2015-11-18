package com.hsbsoftwares.android.app.healthdiagnostic.statistics.webview;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.webkit.WebSettings;
import android.webkit.WebView;


import com.hsbsoftwares.android.app.healthdiagnostic.R;
import com.hsbsoftwares.android.app.healthdiagnostic.db.helper.DatabaseHandler;
import com.hsbsoftwares.android.app.healthdiagnostic.db.model.DailyAverage;

import java.util.List;

public class ChartDailyAverage extends Activity {
    WebView webView;
    DatabaseHandler databaseHandler = new DatabaseHandler(this);

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
        List<DailyAverage> dailyAverage = databaseHandler.getDailyAverage();
        StringBuilder html = new StringBuilder();

        html.append("<html>");
        html.append("<head>");
        html.append("<script type=\"text/javascript\" src=\"https://www.google.com/jsapi\"></script>");
        html.append("<script type=\"text/javascript\">");

        html.append("google.load(\"visualization\", \"1.0\", {\"packages\":[\"corechart\"]});");
        html.append("google.setOnLoadCallback(drawChart);");
        html.append("function drawChart() {");
        html.append("var data = google.visualization.arrayToDataTable([");
        html.append("['Day', 'AverageCrisisduration', 'NumberofCrisis'],");
        for (DailyAverage da : dailyAverage){
            html.append("['");
            html.append(da.getDays());
            html.append("', ");
            html.append(da.getAverageCrisisDuration());
            html.append(", ");
            html.append(da.getNumberOfCrisis());
            html.append("],");
        }
        html.deleteCharAt(html.length() - 1);
        html.append("]);");
        html.append("var options = {title: 'Daily Average', hAxis: {title: 'Day',  titleTextStyle: {color: '#333'}}, vAxis: {minValue: 0}};");
        //html.append("var map = new google.visualization.Map(document.getElementById('map_div'));");
        html.append("var chart = new google.visualization.AreaChart(document.getElementById('chart_div'));");
        html.append("chart.draw(data, options);");
        html.append("}</script>");
        html.append("</head>");
        html.append("<body>");
        html.append("<div id=\"chart_div\" style=\"width: 600px; height: 320px;\"></div>");
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
}
