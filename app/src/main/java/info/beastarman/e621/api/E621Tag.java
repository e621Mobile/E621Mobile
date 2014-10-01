package info.beastarman.e621.api;

import org.json.JSONObject;
import org.w3c.dom.Element;

import java.io.Serializable;

public class E621Tag implements Serializable, Comparable<E621Tag>
{
	private static final long serialVersionUID = 4310674854388740575L;
	
	public static Integer GENERAL = 0;
	public static Integer ARTIST = 1;
	public static Integer COPYRIGHT = 3;
	public static Integer CHARACTER = 4;
	public static Integer SPECIES = 5;
	
	private String tag;
	private Integer id;
	public Integer count;
	public Integer type;
	public Boolean ambiguous;
	
	public E621Tag(String s, Integer id)
	{
		setTag(s);
		setId(id);
	}
	
	public E621Tag(String s, Integer id, Integer count, Integer type, Boolean ambiguous)
	{
		setTag(s);
		setId(id);
		this.count = count;
		this.type = type;
		this.ambiguous = ambiguous;
	}
	
	public static E621Tag fromJson(JSONObject json)
	{
		return new E621Tag(
				json.optString("name",""),
				json.optInt("id",-1),
				json.optInt("count",-1),
				json.optInt("type",-1),
				json.optBoolean("ambiguous",false)
			);
	}
	
	public static E621Tag fromXML(Element xml)
	{
		return new E621Tag(
				xml.getAttribute("name"),
				Integer.parseInt(xml.getAttribute("id")),
				Integer.parseInt(xml.getAttribute("count")),
				Integer.parseInt(xml.getAttribute("type")),
				xml.getAttribute("ambiguous").equals("true")
			);
	}

	public String getTag()
	{
		return tag;
	}

	public void setTag(String tag)
	{
		this.tag = tag;
	}

	public Integer getId()
	{
		return id;
	}

	public void setId(Integer id)
	{
		this.id = id;
	}
	
	@Override
	public String toString()
	{
		return this.getTag();
	}

    public int compareTo(E621Tag that)
    {
        return tag.compareTo(that.tag);
    }
}
