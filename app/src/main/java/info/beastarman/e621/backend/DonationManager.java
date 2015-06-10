package info.beastarman.e621.backend;

import android.net.Uri;
import android.util.Patterns;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

public class DonationManager
{
	private final Uri baseUrl;
	public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZZZZZ");

	public DonationManager(Uri baseUrl)
	{
		this.baseUrl = baseUrl;
	}

	ArrayList<Donator> _donators = null;

	private ArrayList<Donator> retrieveDonators()
	{
		if(_donators == null)
		{
			try
			{
				Uri url = Uri.withAppendedPath(baseUrl, "json/");

				HttpClient client = new PersistentHttpClient(new DefaultHttpClient(), 5);

				HttpResponse response = null;

				try
				{
					response = client.execute(new HttpGet(url.toString()));
				}
				catch(ClientProtocolException e)
				{
					e.printStackTrace();
				}

				if(response != null)
				{
					StatusLine statusLine = response.getStatusLine();

					if(statusLine.getStatusCode() == HttpStatus.SC_OK)
					{
						ByteArrayOutputStream out = new ByteArrayOutputStream();
						response.getEntity().writeTo(out);
						out.close();
						String responseString = out.toString();

						JSONArray jsonResponse = null;

						try
						{
							jsonResponse = new JSONArray(responseString);
						}
						catch(JSONException e)
						{
							e.printStackTrace();
						}

						if(jsonResponse != null)
						{
							int i = 0;

							_donators = new ArrayList<Donator>();

							for(i=0; i<jsonResponse.length(); i++)
							{
								try
								{
									_donators.add(donatorFromJSONObject(jsonResponse.getJSONObject(i)));
								}
								catch(JSONException e)
								{
									e.printStackTrace();
								}
							}
						}
					}
				}
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
		}

		return _donators;
	}

	private Float totalDonations = null;

	public Float getTotalDonations()
	{
		if(totalDonations == null)
		{
			ArrayList<Donator> donators = retrieveDonators();
			totalDonations = 0f;

			for(Donator donator : donators)
			{
				totalDonations += donator.ammount;
			}
		}

		return totalDonations;
	}

	public ArrayList<Donator> getDonators()
	{
		ArrayList<Donator> ret = retrieveDonators();

		if(ret == null) return null;

		ret = new ArrayList<Donator>(ret);
		Collections.sort(ret,getAmmountComparator());
		return ret;
	}

	public ArrayList<Donator> getNewestDonators()
	{
		ArrayList<Donator> ret = retrieveDonators();

		if(ret == null) return null;

		ret = new ArrayList<Donator>(ret);
		Collections.sort(ret,getNewestComparator());
		return ret;
	}

	public ArrayList<Donator> getOldestDonators()
	{
		ArrayList<Donator> ret = retrieveDonators();

		if(ret == null) return null;

		ret = new ArrayList<Donator>(ret);
		Collections.sort(ret,getOldestComparator());
		return ret;
	}

	public class Donator
	{
		public final Uri url;
		public final String name;
		public final float ammount;
		public final Date firstDonation;
		public final Date lastDonation;

		public Donator(Uri url, String name, float ammount, Date firstDonation, Date lastDonation)
		{
			this.url = url;
			this.name = name;
			this.ammount = ammount;
			this.firstDonation = firstDonation;
			this.lastDonation = lastDonation;
		}
	}

	public Donator donatorFromJSONObject(JSONObject object)
	{
		try
		{
			String s = object.optString("url","");
			Uri uri = null;

			if(Patterns.WEB_URL.matcher(s).matches())
			{
				uri = Uri.parse(s);
			}

			return new Donator
						   (
								   uri,
								   object.optString("name","Anonymous"),
								   (float)object.getDouble("ammount"),
								   DATE_FORMAT.parse(object.getString("first_donation")),
								   DATE_FORMAT.parse(object.getString("last_donation"))
						   );
		}
		catch(JSONException e)
		{
			e.printStackTrace();
		}
		catch(ParseException e)
		{
			e.printStackTrace();
		}

		return null;
	}

	private Comparator<Donator> getAmmountComparator()
	{
		return new Comparator<Donator>()
		{
			@Override
			public int compare(Donator d1, Donator d2)
			{
				return new Float(d2.ammount).compareTo(d1.ammount);
			}
		};
	}

	private Comparator<Donator> getNewestComparator()
	{
		return new Comparator<Donator>()
		{
			@Override
			public int compare(Donator d1, Donator d2)
			{
				return d2.firstDonation.compareTo(d1.firstDonation);
			}
		};
	}

	private Comparator<Donator> getOldestComparator()
	{
		return new Comparator<Donator>()
		{
			@Override
			public int compare(Donator d1, Donator d2)
			{
				return d1.lastDonation.compareTo(d2.lastDonation);
			}
		};
	}
}
