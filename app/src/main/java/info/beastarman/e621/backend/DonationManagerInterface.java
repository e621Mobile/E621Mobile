package info.beastarman.e621.backend;

import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by Douglas on 27/03/2016.
 */
public interface DonationManagerInterface {
    Float getTotalDonations();

    Float getMonthDonations();

    ArrayList<Donator> getDonators();

    ArrayList<Donator> getNewestDonators();

    ArrayList<Donator> getOldestDonators();

    Donator getHighlight();

    Donator donatorFromJSONObject(JSONObject object);
}
