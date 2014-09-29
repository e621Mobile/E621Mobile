package info.beastarman.e621.api;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

import org.json.JSONObject;
import org.w3c.dom.Element;

public class E621Image implements Serializable
{
	private static final long serialVersionUID = 4972427634331752322L;
	
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
	
	public String preview_url = "";
	public String sample_url = "";
	public String file_url = "";
	public Integer id = null;
	public String file_ext = "";
	public String parent_id = null;
	public String rating = SAFE;
	public ArrayList<E621Tag> tags = new ArrayList<E621Tag>();
	public ArrayList<String> children = new ArrayList<String>();
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
	
	public static E621Image fromJSON(JSONObject json)
	{
		E621Image img = new E621Image();

		img.preview_url = json.optString("preview_url","");
		
		img.sample_url = json.optString("sample_url","");
		
		img.file_url = json.optString("file_url","");
		
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
	
	@Override
	public String toString()
	{
		return String.valueOf(id);
	}
}
