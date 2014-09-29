package info.beastarman.e621.api;

public class DTextString implements DTextObject
{
	String text = "";
	
	@Override
	public void parse(String text)
	{
		this.text = text;
	}

	@Override
	public String raw()
	{
		return text;
	}

}
