package info.beastarman.e621.api;

import org.apache.commons.lang3.StringEscapeUtils;
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
import info.beastarman.e621.backend.Pair;

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

	public String created_at_raw = "";
	public Date created_at = null;

	public String preview_url = "";
	public String sample_url = "";
	public String file_url = "";
	public Integer id = null;
	public String file_ext = "";
	public String parent_id = null;
	public String rating = SAFE;
	public String description = "";

	public Integer creator_id = null;
	public String author = null;

	public Integer file_size = 0;

	public ArrayList<E621Tag> tags = new ArrayList<E621Tag>();
	public ArrayList<String> children = new ArrayList<String>();
	public ArrayList<String> sources = new ArrayList<String>();
	public ArrayList<String> artist = new ArrayList<String>();
	public boolean has_children = false;
	
	public int score = 0;
	public int fav_count = 0;

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
		artist = new ArrayList<String>(that.artist);
		has_children = that.has_children;
		score = that.score;
		fav_count = that.fav_count;
		preview_width = that.preview_width;
		preview_height = that.preview_height;
		sample_width = that.sample_width;
		sample_height = that.sample_height;
		width = that.width;
		height = that.height;
		status = that.status;
		has_comments = that.has_comments;

		creator_id = that.creator_id;
		author = that.author;

		file_size = that.file_size;
	}
	
	public static E621Image fromJSON(JSONObject json)
	{
		E621Image img = new E621Image();

		img.file_url = json.optString("file_url","");
		if(img.file_url.startsWith("//")) img.file_url = "http:" + img.file_url;
		else if(img.file_url.startsWith("/")) img.file_url = "http://e621.net" + img.file_url;

		img.sample_url = json.optString("sample_url",img.file_url);
		if(img.sample_url.startsWith("//")) img.sample_url = "http:" + img.sample_url;
		else if(img.sample_url.startsWith("/")) img.sample_url = "http://e621.net" + img.sample_url;

		img.preview_url = json.optString("preview_url",img.sample_url);
		if(img.preview_url.startsWith("//")) img.preview_url = "http:" + img.preview_url;
		else if(img.preview_url.startsWith("/")) img.preview_url = "http://e621.net" + img.preview_url;

		img.description = json.optString("description","").trim();

		img.id = json.optInt("id", 666);
		
		img.rating = json.optString("rating", EXPLICIT);
		
		img.file_ext = json.optString("file_ext","jpg");
		
		img.preview_width = json.optInt("preview_width", 1);
		
		img.preview_height = json.optInt("preview_height",1);
		
		img.sample_width = json.optInt("sample_width",1); 
		
		img.sample_height = json.optInt("sample_height",1);
		
		img.width = json.optInt("width",1); 
		
		img.height = json.optInt("height",1);
		
		img.has_children = json.optBoolean("has_children", false);

		img.creator_id = json.optInt("creator_id", 0);
		img.author = json.optString("author", "");

		img.file_size = json.optInt("file_size", 0);
		
		if(!json.isNull(("parent_id")))
		{
			img.parent_id = String.valueOf(json.optInt("parent_id"));
		}
		
		img.score = json.optInt("score",0);
		img.fav_count = json.optInt("fav_count",0);

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
			img.created_at_raw = String.valueOf(json.getJSONObject("created_at").optLong("s",-1));
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

		JSONArray artist = json.optJSONArray("artist");
		img.artist = new ArrayList<String>();

		if(artist != null)
		{
			for(int i=0; i<artist.length(); i++)
			{
				img.artist.add(artist.optString(i,"").trim());
			}
		}
		
		for(String tag : json.optString("tags","").split("\\s"))
		{
			if(tag.length() > 0)img.tags.add(new E621Tag(tag,null));
		}
		
		img.has_comments = json.optBoolean("has_comments",false);
		
		return img;
	}

	private static String getValueFromNode(Element node, String name)
	{
		return node.getElementsByTagName(name).item(0).getTextContent();
	}
	
	public static E621Image fromXML(Element xml)
	{
		E621Image img = new E621Image();

		img.file_url = getValueFromNode(xml,"file_url");
		if(img.file_url.startsWith("//")) img.file_url = "http:" + img.file_url;
		else if(img.file_url.startsWith("/")) img.file_url = "http://e621.net" + img.file_url;

		img.sample_url = getValueFromNode(xml,"sample_url");
		if(img.sample_url.startsWith("//")) img.sample_url = "http:" + img.sample_url;
		else if(img.sample_url.startsWith("/")) img.sample_url = "http://e621.net" + img.sample_url;

		img.preview_url = getValueFromNode(xml,"preview_url");
		if(img.preview_url.startsWith("//")) img.preview_url = "http:" + img.preview_url;
		else if(img.preview_url.startsWith("/")) img.preview_url = "http://e621.net" + img.preview_url;

		img.description = getValueFromNode(xml,"description").trim();

		img.id = Integer.parseInt(getValueFromNode(xml,"id"));
		img.rating = getValueFromNode(xml,"rating"); 
		img.file_ext = getValueFromNode(xml,"file_ext");
		img.has_comments = getValueFromNode(xml,"has_comments").equals("true");
		img.has_children= getValueFromNode(xml,"has_children").equals("true");

		try
		{
			img.file_size = Integer.parseInt(getValueFromNode(xml,"file_size"));
		}
		catch (NumberFormatException e)
		{
			img.file_size = 0;
		}

		try
		{
			img.creator_id = Integer.parseInt(getValueFromNode(xml,"creator_id"));
		}
		catch (NumberFormatException e)
		{
			img.creator_id = 0;
		}

		img.author = getValueFromNode(xml,"author");
		
		if(getValueFromNode(xml,"parent_id").length() > 0)
		{
			img.parent_id = getValueFromNode(xml,"parent_id");
		}
		
		String children = getValueFromNode(xml,"children").trim();
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
			img.created_at_raw = getValueFromNode(xml,"created_at");
			img.created_at = DATE_FORMAT_XML.parse(img.created_at_raw);
		} catch (ParseException e)
		{
			e.printStackTrace();
		}

		String sources = StringEscapeUtils.unescapeXml(getValueFromNode(xml,"sources"));
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

		String artist = StringEscapeUtils.unescapeXml(getValueFromNode(xml,"artist"));
		img.artist = new ArrayList<String>();

		if(artist.length() > 0)
		{
			try
			{
				JSONArray artistArray = new JSONArray(artist);

				for(int i=0; i<artistArray.length(); i++)
				{
					img.artist.add(artistArray.optString(i,"").trim());
				}
			}
			catch (JSONException e)
			{
				e.printStackTrace();
			}
		}
		
		String status = getValueFromNode(xml,"status");
		
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
			img.preview_width = Integer.parseInt(getValueFromNode(xml,"preview_width"));
		} catch (NumberFormatException e) {}
		try{
			img.preview_height = Integer.parseInt(getValueFromNode(xml,"preview_height"));
		} catch (NumberFormatException e) {}
		try{
			img.sample_width = Integer.parseInt(getValueFromNode(xml,"sample_width"));
		} catch (NumberFormatException e) {}
		try{
			img.sample_height = Integer.parseInt(getValueFromNode(xml,"sample_height"));
		} catch (NumberFormatException e) {}
		try{
			img.width = Integer.parseInt(getValueFromNode(xml,"width"));
		} catch (NumberFormatException e) {}
		try{
			img.height = Integer.parseInt(getValueFromNode(xml,"height"));
		} catch (NumberFormatException e) {}
		try{
			img.score = Integer.parseInt(getValueFromNode(xml,"score"));
		} catch (NumberFormatException e) {}
		try{
			img.fav_count = Integer.parseInt(getValueFromNode(xml,"fav_count"));
		} catch (NumberFormatException e) {}
		for(String tag : getValueFromNode(xml,"tags").split("\\s"))
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

	public Pair<Integer,Integer> getSize(int size)
	{
		switch (size)
		{
			case PREVIEW:
				return new Pair<Integer, Integer>(preview_width,preview_height);
			case SAMPLE:
				return new Pair<Integer, Integer>(sample_width,sample_height);
			default:
				return new Pair<Integer, Integer>(width,height);
		}
	}
}
