package info.beastarman.e621.frontend;

import android.app.Application;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

import java.util.HashMap;

public class E621Application extends Application
{
    public enum TrackerName {
        APP_TRACKER, // Tracker used only in this app.
    }

    HashMap<TrackerName, Tracker> mTrackers = new HashMap<TrackerName, Tracker>();

    synchronized Tracker getTracker()
    {
        TrackerName trackerId = TrackerName.APP_TRACKER;

        if (!mTrackers.containsKey(trackerId))
        {
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
            Tracker t = analytics.newTracker("UA-55342416-1");
            mTrackers.put(trackerId, t);
        }

        return mTrackers.get(trackerId);
    }
}
