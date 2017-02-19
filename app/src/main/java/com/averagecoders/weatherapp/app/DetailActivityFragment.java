package com.averagecoders.weatherapp.app;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * A placeholder fragment containing a simple view.
 */
public class DetailActivityFragment extends Fragment {

    private static final String Log_Tag = DetailActivityFragment.class.getSimpleName();

    private static final String HashTag = "  #WeatherApp";
    private String mForeCastStr;

    public DetailActivityFragment() {
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Intent intent = getActivity().getIntent();
        View rootView = inflater.inflate(R.layout.fragment_detail2, container, false);
        if( intent != null && intent.hasExtra(Intent.EXTRA_TEXT)){
            mForeCastStr = intent.getStringExtra(Intent.EXTRA_TEXT);
            ((TextView) rootView.findViewById(R.id.detail_text)).setText(mForeCastStr);
        }
        return  rootView;
    }

    private Intent createShareForecastIntent(){
        Intent shareintent = new Intent(Intent.ACTION_SEND);
        shareintent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        shareintent.setType("text/plain");
        shareintent.putExtra(Intent.EXTRA_TEXT,mForeCastStr + HashTag);
        return shareintent;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        inflater.inflate(R.menu.deatilfragment,menu);

        MenuItem menuItem =  menu.findItem(R.id.action_Share);

        android.support.v7.widget.ShareActionProvider mShareActionProvider =
                (android.support.v7.widget.ShareActionProvider) MenuItemCompat.getActionProvider(menuItem);

        if( mShareActionProvider != null) {
             mShareActionProvider.setShareIntent(createShareForecastIntent());
        }else {
            Log.d(Log_Tag," Share  Action Provider Failure ");
        }


    }
}
