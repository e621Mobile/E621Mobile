package info.beastarman.e621.api;

import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import info.beastarman.e621.api.dtext.DText;

public class E621Comment
{
	public static final String DATE_FORMAT = "yyyy-MM-dd kk:mm";
	public String body = null;
	public int creator_id = 0;
	public Date created_at = null;
	public int score = 0;
	public int post_id = 0;
	public int id = 0;
	public String creator = "";
	
	public E621Comment()
	{
	}
	
	public E621Comment(String body, int creator_id, Date created_at, int score, int post_id, int id, String creator)
	{
		this.body = body;
		this.creator_id = creator_id;
		this.created_at = created_at;
		this.score = score;
		this.post_id = post_id;
		this.id = id;
		this.creator = creator;
	}
	
	public static E621Comment fromJson(JSONObject json)
	{
		E621Comment obj = new E621Comment();
		
		obj.creator_id = json.optInt("creator_id", 0);
		obj.score = json.optInt("score", 0);
		obj.post_id = json.optInt("post_id", 0);
		obj.id = json.optInt("id", 0);
		obj.creator = json.optString("creator", "");
		
		try
		{
			SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT, Locale.US);
			dateFormat.setTimeZone(TimeZone.getTimeZone("GMT+0"));
			obj.created_at = dateFormat.parse(json.optString("created_at", "1970-01-01 00:00"));
		}
		catch(ParseException e)
		{
		}
		
		obj.body = json.optString("body", "");
		
		return obj;
	}

	public DText getBodyAsDText()
	{
		return new DText(body);
	}

	@Override
	public String toString()
	{
		return body;
	}
}
