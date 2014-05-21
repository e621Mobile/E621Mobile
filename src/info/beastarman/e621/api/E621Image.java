package info.beastarman.e621.api;

import java.io.Serializable;
import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Element;

public class E621Image implements Serializable
{
	private static final long serialVersionUID = 4972427634331752322L;
	
	public static int PREVIEW = 1;
	public static int SAMPLE = 2;
	public static int FULL = 3;
	
	public static String EXPLICIT = "e";
	public static String QUESTIONABLE = "q";
	public static String SAFE = "s";
	
	public String preview_url = "";
	public String sample_url = "";
	public String file_url = "";
	public String id = "";
	public String file_ext = "";
	public String rating = "s";
	public ArrayList<E621Tag> tags = new ArrayList<E621Tag>();

	public int preview_width = 0;
	public int preview_height = 0;

	public int sample_width = 0;
	public int sample_height = 0;
	
	public int width = 0;
	public int height = 0;
	
	
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
		for(String tag : xml.getAttribute("tags").split("\\s"))
		{
			img.tags.add(new E621Tag(tag));
		}
		
		return img;
	}
}
