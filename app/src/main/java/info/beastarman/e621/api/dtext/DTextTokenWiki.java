package info.beastarman.e621.api.dtext;

public class DTextTokenWiki implements DTextToken
{
	public String wikiPage;
	public String title;

	public DTextTokenWiki(String wikiPage)
	{
		this.wikiPage = wikiPage;
		this.title = wikiPage;
	}

	public DTextTokenWiki(String wikiPage, String title)
	{
		this.wikiPage = wikiPage;
		this.title = title;
	}
}
