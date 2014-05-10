package info.beastarman.e621.api;

public class E621Tag
{
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
