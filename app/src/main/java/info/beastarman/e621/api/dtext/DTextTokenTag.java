package info.beastarman.e621.api.dtext;

public class DTextTokenTag implements DTextToken
{
	public String tag = "";
	public String extraValue = null;
	public boolean opening = true;

	public DTextTokenTag(String tag, boolean opening, String extraValue)
	{
		this.tag = tag;
		this.extraValue = extraValue;
		this.opening = opening;
	}

	public DTextTokenTag(String tag, boolean opening)
	{
		this.tag = tag;
		this.opening = opening;
	}

	public DTextTokenTag(String tag)
	{
		this.tag = tag;
	}

	@Override
	public String toString()
	{
		return "[" +
				(opening?"":"/") +
				tag +
				(extraValue==null?"":"="+extraValue) +
				"]";
	}
}
