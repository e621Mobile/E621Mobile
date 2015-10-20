package info.beastarman.e621.backend.errorReport;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

/**
 * Created by beastarman on 10/19/2015.
 */
public class ErrorReportReport
{
	public String hash = "";
	public String log = "";
	public String text = "";
	public ArrayList<String> tags = new ArrayList<String>();
	public Date time = new Date();

	private static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-d HH:mm:ss.SZZZZZ", Locale.US);

	public ErrorReportReport(JSONObject jsonObject)
	{
		text = jsonObject.optString("text","");
		log = jsonObject.optString("log", "");
		hash = jsonObject.optString("hash", "");

		tags = new ArrayList<String>();

		try
		{
			JSONArray tagArray = jsonObject.getJSONArray("tags");

			for(int i=0; i<tagArray.length(); i++)
			{
				tags.add(tagArray.getString(i));
			}
		}
		catch(JSONException e)
		{
			e.printStackTrace();
		}

		try
		{
			time = DATE_FORMAT.parse(jsonObject.optString("time",""));
		}
		catch(ParseException e)
		{
			e.printStackTrace();
			time = null;
		}
	}

	public ErrorReportReport(String log, String text, ArrayList<String> tags)
	{
		this.hash = "";
		this.log = log;
		this.text = text;
		this.tags = tags;
		this.time = new Date();
	}

	public ErrorReportReport()
	{
	}
}
