package info.beastarman.e621.api;

import org.json.JSONException;
import org.json.JSONObject;

public class E621Image
{
	public static int PREVIEW = 1;
	public static int SAMPLE = 2;
	public static int FULL = 3;
	
	public String preview_url = "";
	public String sample_url = "";
	public String file_url = "";
	public String id = "";
	public String file_ext = "";
	
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
			img.file_ext = json.getString("file_ext");
		} catch (JSONException e) {} 
		
		return img;
	}
}
