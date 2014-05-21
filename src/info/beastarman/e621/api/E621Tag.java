package info.beastarman.e621.api;

import java.io.Serializable;

public class E621Tag implements Serializable
{
	private static final long serialVersionUID = 4310674854388740575L;
	
	private String tag;
	
	public E621Tag(String s)
	{
		setTag(s);
	}

	public String getTag()
	{
		return tag;
	}

	public void setTag(String tag)
	{
		this.tag = tag;
	}
	
	@Override
	public String toString()
	{
		return this.getTag();
	}
}
