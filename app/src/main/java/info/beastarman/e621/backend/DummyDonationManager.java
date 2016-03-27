package info.beastarman.e621.backend;

import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by Douglas on 27/03/2016.
 */
public class DummyDonationManager implements DonationManagerInterface {
    @Override
    public Float getTotalDonations() {
        return 0f;
    }

    @Override
    public Float getMonthDonations() {
        return 0f;
    }

    @Override
    public ArrayList<Donator> getDonators() {
        return new ArrayList<Donator>();
    }

    @Override
    public ArrayList<Donator> getNewestDonators() {
        return new ArrayList<Donator>();
    }

    @Override
    public ArrayList<Donator> getOldestDonators() {
        return new ArrayList<Donator>();
    }

    @Override
    public Donator getHighlight() {
        return null;
    }

    @Override
    public Donator donatorFromJSONObject(JSONObject object) {
        return new Donator();
    }
}
