package com.averagecoders.weatherapp.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * A placeholder fragment containing a simple view.
 */
public class ForecastFragment extends Fragment {

    private ArrayAdapter<String> mForecastAdapter;
    private List<String> myData;


    public int getCount(){
        return myData.size();
    }
    public ForecastFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        // Inflate the menu; this adds items to the action bar if it is present.

        menuInflater.inflate(R.menu.menu_main, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        /*if (id == R.id.refreshmenu) {
            updateWeather();
            return true;
        }
        */return super.onOptionsItemSelected(item);
    }
    private void updateWeather(){
        FetchWeather weather = new FetchWeather();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String location = prefs.getString(getString(R.string.pref_location_key),
                getString(R.string.pref_location_default));
        weather.execute(location);

    }

    @Override
    public void onStart() {
        super.onStart();
        updateWeather();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootview = inflater.inflate(R.layout.fragment_main, container, false);
        /*String[] forecast = {
                    "Item 1",
                    "Item 2",
                "Item3 ",
                "item4",
                "Item5",
                "Item6"
        };*/
        //List<String> myData = new ArrayList<String>(Arrays.asList(forecast));

         mForecastAdapter = new ArrayAdapter<String>(
                 getActivity()
                , R.layout.item_list
                , R.id.text_list_item
                , new ArrayList<String>());

        ListView listView = (ListView) rootview.findViewById(R.id.list_view);
        listView.setAdapter(mForecastAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                String forecast = mForecastAdapter.getItem(position).toString();
                Intent intent = new Intent(getActivity(),DetailActivity.class).putExtra(Intent.EXTRA_TEXT,forecast);
                startActivity(intent);

                Toast.makeText(getActivity(),forecast,Toast.LENGTH_LONG).show();
            }
        });

        return rootview;

    }
    public class FetchWeather extends AsyncTask< String,Void ,String[] >{

        private final String Log_TAG = FetchWeather.class.getSimpleName();

        /* The date/time conversion code is going to be moved outside the asynctask later,
   * so for convenience we're breaking it out into its own method now.
   */
        private String getReadableDateString(long time){
            // Because the API returns a unix timestamp (measured in seconds),
            // it must be converted to milliseconds in order to be converted to valid date.
            SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
            return shortenedDateFormat.format(time);
        }

        /**
         * Prepare the weather high/lows for presentation.
         */
        private String formatHighLows(double high, double low) {
            // For presentation, assume the user doesn't care about tenths of a degree.
            SharedPreferences Sharedprefs =
                    PreferenceManager.getDefaultSharedPreferences(getActivity());
            String UnitType = Sharedprefs.getString(
                    getString(R.string.pref_units_key),
                    getString(R.string.pref_units_metric));

            if(UnitType.equals(getString(R.string.pref_units_imperial))){
                high = (high *1.8) + 32;
                low = (low * 1.8) + 32 ;
            }else if (!UnitType.equals(getString(R.string.pref_units_metric))){
                Log.d(Log_TAG," Unit Not  Found " + UnitType);
            }
            long roundedHigh = Math.round(high);
            long roundedLow = Math.round(low);

            String highLowStr = roundedHigh + "/" + roundedLow;
            return highLowStr;
        }
        private void changeDays(){
            SharedPreferences Sharedpref =
                    PreferenceManager.getDefaultSharedPreferences(getActivity());
            String Days = Sharedpref.getString(
                    getString(R.string.pref_days_key),
                    getString(R.string.pref_days_default));

        }
        /**
         * Take the String representing the complete forecast in JSON Format and
         * pull out the data we need to construct the Strings needed for the wireframes.
         *
         * Fortunately parsing is easy:  constructor takes the JSON string and converts it
         * into an Object hierarchy for us.
         */
        public String[] getWeatherDataFromJson(String forecastJsonStr, int numDays)
                throws JSONException {

            // These are the names of the JSON objects that need to be extracted.
            final String OWM_LIST = "list";
            final String OWM_WEATHER = "weather";
            final String OWM_TEMPERATURE = "temp";
            final String OWM_MAX = "temp_max";
            final String OWM_MIN = "temp_min";
            final String OWM_DESCRIPTION = "main";
            final String details = "description";
            JSONObject forecastJson = new JSONObject(forecastJsonStr);
            JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

            // OWM returns daily forecasts based upon the local time of the city that is being
            // asked for, which means that we need to know the GMT offset to translate this data
            // properly.

            // Since this data is also sent in-order and the first day is always the
            // current day, we're going to take advantage of that to get a nice
            // normalized UTC date for all of our weather.

            Time dayTime = new Time();
            dayTime.setToNow();

            // we start at the day returned by local time. Otherwise this is a mess.
            int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);

            // now we work exclusively in UTC
            dayTime = new Time();

            String[] resultStrs = new String[numDays];
            for(int i = 0; i < weatherArray.length(); i++) {
                // For now, using the format "Day, description, hi/low"
                String day;
                String description;
                String highAndLow;
                //String ctemp;
                // Get the JSON object representing the day
                JSONObject dayForecast = weatherArray.getJSONObject(i);

                // The date/time is returned as a long.  We need to convert that
                // into something human-readable, since most people won't read "1400356800" as
                // "this saturday".
                long dateTime;
                // Cheating to convert this to UTC time, which is what we want anyhow
                dateTime = dayTime.setJulianDay(julianStartDay+i);
                day = getReadableDateString(dateTime);

                // description is in a child array called "weather", which is 1 element long.
                JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
                description = weatherObject.getString(details);

                // Temperatures are in a child object called "temp".  Try not to name variables
                // "temp" when working with temperature.  It confuses everybody.
                //JSONObject temperatureObject = dayForecast.getJSONArray(OWM_DESCRIPTION).getJSONObject(1);
                JSONObject temperatureObject = dayForecast.getJSONObject(OWM_DESCRIPTION);
                double high = temperatureObject.getDouble(OWM_MAX);
                double low = temperatureObject.getDouble(OWM_MIN);
                /*
                ctemp = temparatureObject.getString(OWM_TEMPERATURE);
                String high = temparatureObject.getString(OWM_MAX);
                String low = temparatureObject.getString(OWM_MIN);
                *///String highAndlow = high + low ;
                highAndLow = formatHighLows(high, low);
                resultStrs[i] = day + " - " + description + " - " + highAndLow;
            }

            /*for(String s : resultStrs){
                Log.v(Log_TAG,"Forecast entry : " + s);
            }*/
            return resultStrs;
        }


        @Override
        protected String[] doInBackground(String... params) {

            if(params.length == 0 ) {
                return null;
            }

            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

// Will contain the raw JSON response as a string.
            String forecastJsonStr = null;
            String format = "JSON";
            String units = "metric";
            String id = "0549f1616b13617688f57828fe93a2ee";
            int numDays = 7;

            try {
                // Construct the URL for the OpenWeatherMap query
                // Possible parameters are avaiable at OWM's forecast API page, at
                // http://openweathermap.org/API#forecast
                final  String Base_url = "http://api.openweathermap.org/data/2.5/forecast";
                final  String query = "q";
                final String mode = "mode";
                final String type = "units";
                final String days ="cnt";
                final String appid = "appid";

                Uri builtUri = Uri.parse(Base_url).buildUpon()
                        .appendQueryParameter(query,params[0])
                        //.appendQueryParameter(mode,format)
                        .appendQueryParameter(type,units)
                        .appendQueryParameter(days,Integer.toString(numDays))
                        .appendQueryParameter(appid,id)
                        .build();


                URL url = new URL(builtUri.toString());
               // Log.v(Log_TAG,"Built Uri " + builtUri.toString());
                // Create the request to OpenWeatherMap, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }
                forecastJsonStr = buffer.toString();
               // Log.v(Log_TAG,"Json data    "+ forecastJsonStr);
            } catch (IOException e) {
                Log.e(Log_TAG, "Error ", e);
                // If the code didn't successfully get the weather data, there's no point in attemping
                // to parse it.
                return null;
            } finally{
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(Log_TAG, "Error closing stream", e);
                    }
                }
            }

            try {
                return getWeatherDataFromJson(forecastJsonStr,numDays);
            }catch (JSONException e){
                Log.e(Log_TAG,e.getMessage(),e);
                e.printStackTrace();
             }
            return null;
        }

        @Override
        protected void onPostExecute(String[] result) {
            if(result != null){
                mForecastAdapter.clear();

                for (String dayForecastStr : result) {

                    //Log.v(Log_TAG,"onPostExecute : " + dayForecastStr + myData);

                   mForecastAdapter.add(dayForecastStr);
                }
            mForecastAdapter.notifyDataSetChanged();
            }
        }
    }
}
