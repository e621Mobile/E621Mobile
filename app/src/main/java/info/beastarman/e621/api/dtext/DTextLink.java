package info.beastarman.e621.api.dtext;

public class DTextLink extends DTextObject
{
	public String link;
	public String title;

	public DTextLink(String link, String title)
	{
		this.link = link;
		this.title = title;
	}
}
