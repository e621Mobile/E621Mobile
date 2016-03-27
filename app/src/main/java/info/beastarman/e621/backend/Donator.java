package info.beastarman.e621.backend;

import android.net.Uri;

import java.util.Date;

/**
 * Created by Douglas on 27/03/2016.
 */
public class Donator {
    public final Uri url;
    public final String name;
    public final float ammount;
    public final Float recent_total;
    public final Date firstDonation;
    public final Date lastDonation;

	public Donator() {
		this(null,"",0f,null,null,0f);
	}

	public Donator(Uri url, String name, float ammount, Date firstDonation, Date lastDonation, Float recent_total) {
        this.url = url;
        this.name = name;
        this.ammount = ammount;
        this.firstDonation = firstDonation;
        this.lastDonation = lastDonation;
        this.recent_total = recent_total;
    }
}
