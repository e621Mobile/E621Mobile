package info.beastarman.e621.api;

import org.json.JSONObject;
import org.w3c.dom.Element;

public class E621TagAlias
{
	public Integer alias_id = 0;
	public Boolean pending = false;
	public Integer id = 0;
	public String name;
	
	public E621TagAlias(Integer alias_id, Integer id, Boolean pending, String name)
	{
		this.alias_id = alias_id;
		this.id = id;
		this.pending = pending;
		this.name = name;
	}
	
	public static E621TagAlias fromJson(JSONObject json)
	{
		return new E621TagAlias(
				json.optInt("alias_id",0),
				json.optInt("id",0),
				json.optBoolean("pending",false),
				json.optString("name","")
			);
	}
	
	public static E621TagAlias fromXML(Element xml)
	{
		return new E621TagAlias(
				Integer.parseInt(xml.getAttribute("alias_id")),
				Integer.parseInt(xml.getAttribute("id")),
				xml.getAttribute("pending").equals("true"),
				xml.getAttribute("name")
			);
	}
}
