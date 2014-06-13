package info.beastarman.e621.api;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Element;

import android.util.Log;

public class E621Image implements Serializable
{
	private static final long serialVersionUID = 4972427634331752322L;
	
	public static int PREVIEW = 1;
	public static int SAMPLE = 2;
	public static int FULL = 3;
	
	public static int ACTIVE = 1;
	public static int FLAGGED = 2;
	public static int PENDING = 3;
	public static int DELETED = 4;
	
	public static String EXPLICIT = "e";
	public static String QUESTIONABLE = "q";
	public static String SAFE = "s";
	
	public String preview_url = "";
	public String sample_url = "";
	public String file_url = "";
	public String id = "";
	public String file_ext = "";
	public String parent_id = null;
	public String rating = SAFE;
	public ArrayList<E621Tag> tags = new ArrayList<E621Tag>();
	public ArrayList<String> children = new ArrayList<String>();
	
	public int score = 0;

	public int preview_width = 0;
	public int preview_height = 0;

	public int sample_width = 0;
	public int sample_height = 0;
	
	public int width = 0;
	public int height = 0;
	
	public int status = ACTIVE;
	
	public E621Image()
	{
	}
	
	public static E621Image fromJSON(JSONObject json)
	{
		E621Image img = new E621Image();

		try {
			img.preview_url = json.getString("preview_url");
		} catch (JSONException e) {} 
		try {
			img.sample_url = json.getString("sample_url");
		} catch (JSONException e) {} 
		try {
			img.file_url = json.getString("file_url");
		} catch (JSONException e) {} 
		try {
			img.id = json.getString("id");
		} catch (JSONException e) {} 
		try {
			img.rating = json.getString("rating");
		} catch (JSONException e) {} 
		try {
			img.file_ext = json.getString("file_ext");
		} catch (JSONException e) {} 
		try {
			img.preview_width = json.getInt("preview_width");
		} catch (JSONException e) {} 
		try {
			img.preview_height = json.getInt("preview_height");
		} catch (JSONException e) {} 
		try {
			img.sample_width = json.getInt("sample_width");
		} catch (JSONException e) {} 
		try {
			img.sample_height = json.getInt("sample_height");
		} catch (JSONException e) {} 
		try {
			img.width = json.getInt("width");
		} catch (JSONException e) {} 
		try {
			img.height = json.getInt("height");
		} catch (JSONException e) {} 
		try {
			if(!json.isNull(("parent_id")))
			{
				img.parent_id = String.valueOf(json.getInt("parent_id"));
			}
		} catch (JSONException e) {} 
		try {
			img.score = json.getInt("score");
		} catch (JSONException e) {} 
		try {
			String status = json.getString("status");
			
			if(status.equals("active"))
			{
				img.status = ACTIVE;
			}
			else if(status.equals("flagged"))
			{
				img.status = FLAGGED;
			}
			else if(status.equals("pending"))
			{
				img.status = PENDING;
			}
			else if(status.equals("deleted"))
			{
				img.status = DELETED;
			}
		} catch (JSONException e) {} 
		try {
			String children = json.getString("children").trim();
			if(children.length() > 0)
			{
				img.children = new ArrayList<String>(Arrays.asList(children.split(",")));
			}
		} catch (JSONException e) {} 
		try {
			for(String tag : json.getString("tags").split("\\s"))
			{
				img.tags.add(new E621Tag(tag));
			}
		} catch (JSONException e) {} 
		
		return img;
	}
	
	public static E621Image fromXML(Element xml)
	{
		E621Image img = new E621Image();

		img.preview_url = xml.getAttribute("preview_url"); 
		img.sample_url = xml.getAttribute("sample_url"); 
		img.file_url = xml.getAttribute("file_url"); 
		img.id = xml.getAttribute("id");  
		img.rating = xml.getAttribute("rating"); 
		img.file_ext = xml.getAttribute("file_ext");
		
		if(xml.getAttribute("parent_id").length() > 0)
		{
			img.parent_id = xml.getAttribute("parent_id");
		}
		
		String children = xml.getAttribute("children").trim();
		if(children.length() > 0)
		{
			img.children = new ArrayList<String>(Arrays.asList(children.split(",")));
		}
		
		String status = xml.getAttribute("status");
		
		if(status.equals("active"))
		{
			img.status = ACTIVE;
		}
		else if(status.equals("flagged"))
		{
			img.status = FLAGGED;
		}
		else if(status.equals("pending"))
		{
			img.status = PENDING;
		}
		else if(status.equals("deleted"))
		{
			img.status = DELETED;
		}
		
		try{
			img.preview_width = Integer.parseInt(xml.getAttribute("preview_width"));
		} catch (NumberFormatException e) {}
		try{
			img.preview_height = Integer.parseInt(xml.getAttribute("preview_height"));
		} catch (NumberFormatException e) {}
		try{
			img.sample_width = Integer.parseInt(xml.getAttribute("sample_width"));
		} catch (NumberFormatException e) {}
		try{
			img.sample_height = Integer.parseInt(xml.getAttribute("sample_height"));
		} catch (NumberFormatException e) {}
		try{
			img.width = Integer.parseInt(xml.getAttribute("width"));
		} catch (NumberFormatException e) {}
		try{
			img.height = Integer.parseInt(xml.getAttribute("height"));
		} catch (NumberFormatException e) {}
		try{
			img.score = Integer.parseInt(xml.getAttribute("score"));
		} catch (NumberFormatException e) {}
		for(String tag : xml.getAttribute("tags").split("\\s"))
		{
			img.tags.add(new E621Tag(tag));
		}
		
		return img;
	}
	
	public boolean has_children()
	{
		Log.d("Msg",children.toString());
		
		return (children.size() > 0);
	}
	
	@Override
	public String toString()
	{
		return id;
	}
}
