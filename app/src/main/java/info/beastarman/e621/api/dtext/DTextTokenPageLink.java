package info.beastarman.e621.api.dtext;

public class DTextTokenPageLink implements DTextToken
{
	public String page;
	public int number;

	public DTextTokenPageLink(String page, int number)
	{
		this.page = page;
		this.number = number;
	}
}
