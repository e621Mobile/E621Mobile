package info.beastarman.e621.api;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Element;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import info.beastarman.e621.api.dtext.DText;

public class E621Image implements Serializable
{
	private static final long serialVersionUID = 4972427634331752322L;

	public static final SimpleDateFormat DATE_FORMAT_XML = new SimpleDateFormat("EEE MMM d HH:mm:ss ZZZ yyyy");
	
	public static final int PREVIEW = 1;
	public static final int SAMPLE = 2;
	public static final int FULL = 3;
	
	public static int ACTIVE = 1;
	public static int FLAGGED = 2;
	public static int PENDING = 3;
	public static int DELETED = 4;
	
	public static String EXPLICIT = "e";
	public static String QUESTIONABLE = "q";
	public static String SAFE = "s";

	public Date created_at = null;
	public String preview_url = "";
	public String sample_url = "";
	public String file_url = "";
	public Integer id = null;
	public String file_ext = "";
	public String parent_id = null;
	public String rating = SAFE;
	public String description = "";
	public ArrayList<E621Tag> tags = new ArrayList<E621Tag>();
	public ArrayList<String> children = new ArrayList<String>();
	public ArrayList<String> sources = new ArrayList<String>();
	public boolean has_children = false;
	
	public int score = 0;

	public int preview_width = 0;
	public int preview_height = 0;

	public int sample_width = 0;
	public int sample_height = 0;
	
	public int width = 0;
	public int height = 0;
	
	public int status = ACTIVE;
	
	public boolean has_comments = false;

	public E621Image()
	{
	}

	public E621Image(E621Image that)
	{
		preview_url = that.preview_url;
		sample_url = that.sample_url;
		file_url = that.file_url;
		description = that.description;

		id = that.id;
		file_ext = that.file_ext;
		parent_id = that.parent_id;
		rating = that.rating;
		tags = new ArrayList<E621Tag>(that.tags);
		children = new ArrayList<String>(that.children);
		sources = new ArrayList<String>(that.sources);
		has_children = that.has_children;
		score = that.score;
		preview_width = that.preview_width;
		preview_height = that.preview_height;
		sample_width = that.sample_width;
		sample_height = that.sample_height;
		width = that.width;
		height = that.height;
		status = that.status;
		has_comments = that.has_comments;
	}
	
	public static E621Image fromJSON(JSONObject json)
	{
		E621Image img = new E621Image();

		img.preview_url = json.optString("preview_url","");
		
		img.sample_url = json.optString("sample_url","");
		
		img.file_url = json.optString("file_url","");

		img.description = json.optString("description","").trim();

		img.id = json.optInt("id",666);
		
		img.rating = json.optString("rating",EXPLICIT);
		
		img.file_ext = json.optString("file_ext","jpg");
		
		img.preview_width = json.optInt("preview_width",1);
		
		img.preview_height = json.optInt("preview_height",1);
		
		img.sample_width = json.optInt("sample_width",1); 
		
		img.sample_height = json.optInt("sample_height",1);
		
		img.width = json.optInt("width",1); 
		
		img.height = json.optInt("height",1);
		
		img.has_children = json.optBoolean("has_children",false);
		
		if(!json.isNull(("parent_id")))
		{
			img.parent_id = String.valueOf(json.optInt("parent_id"));
		}
		
		img.score = json.optInt("score",0);
		
		String status = json.optString("status","active");
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
		
		String children = json.optString("children","").trim();
		if(children.length() > 0)
		{
			img.children = new ArrayList<String>(Arrays.asList(children.split(",")));
		}
		else
		{
			img.children = new ArrayList<String>();
		}

		try
		{
			img.created_at = new Date(json.getJSONObject("created_at").optLong("s",0)*1000);
		} catch (JSONException e)
		{
			e.printStackTrace();
		}

		JSONArray sources = json.optJSONArray("sources");
		img.sources = new ArrayList<String>();

		if(sources != null)
		{
			for(int i=0; i<sources.length(); i++)
			{
				img.sources.add(sources.optString(i,"").trim());
			}
		}
		
		for(String tag : json.optString("tags","").split("\\s"))
		{
			if(tag.length() > 0)img.tags.add(new E621Tag(tag,null));
		}
		
		img.has_comments = json.optBoolean("has_comments",false);
		
		return img;
	}
	
	public static E621Image fromXML(Element xml)
	{
		E621Image img = new E621Image();

		img.preview_url = xml.getAttribute("preview_url"); 
		img.sample_url = xml.getAttribute("sample_url"); 
		img.file_url = xml.getAttribute("file_url");

		img.description = xml.getAttribute("description").trim();

		img.id = Integer.parseInt(xml.getAttribute("id"));
		img.rating = xml.getAttribute("rating"); 
		img.file_ext = xml.getAttribute("file_ext");
		img.has_comments = xml.getAttribute("has_comments").equals("true");
		img.has_children= xml.getAttribute("has_children").equals("true");
		
		if(xml.getAttribute("parent_id").length() > 0)
		{
			img.parent_id = xml.getAttribute("parent_id");
		}
		
		String children = xml.getAttribute("children").trim();
		if(children.length() > 0)
		{
			img.children = new ArrayList<String>(Arrays.asList(children.split(",")));
		}
		else
		{
			img.children = new ArrayList<String>();
		}

		try
		{
			img.created_at = DATE_FORMAT_XML.parse(xml.getAttribute("created_at"));
		} catch (ParseException e)
		{
			e.printStackTrace();
		}

		String sources = xml.getAttribute("sources");
		img.sources = new ArrayList<String>();

		if(sources.length() > 0)
		{
			try
			{
				JSONArray sourcesArray = new JSONArray(sources);

				for(int i=0; i<sourcesArray.length(); i++)
				{
					img.sources.add(sourcesArray.optString(i,"").trim());
				}
			}
			catch (JSONException e)
			{
				e.printStackTrace();
			}
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
			img.tags.add(new E621Tag(tag,null));
		}
		
		return img;
	}

	public DText getDescriptionAsDText()
	{
		return new DText(description);
	}
	
	@Override
	public String toString()
	{
		return String.valueOf(id);
	}
}
