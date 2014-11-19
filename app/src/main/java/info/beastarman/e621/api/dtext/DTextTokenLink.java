package info.beastarman.e621.api.dtext;

public class DTextTokenLink implements DTextToken
{
	public String link;
	public String title;

	public DTextTokenLink(String link)
	{
		this.link = link;
		this.title = link;
	}

	public DTextTokenLink(String link, String title)
	{
		this.link = link;
		this.title = title;
	}
}
