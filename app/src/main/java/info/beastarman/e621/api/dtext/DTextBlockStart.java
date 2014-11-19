package info.beastarman.e621.api.dtext;

import info.beastarman.e621.views.DTextView;

public abstract class DTextBlockStart extends DTextObject
{
	public String name;

	public DTextBlockStart(String name)
	{
		this.name = name;
	}

	public abstract void apply(DTextView dTextView);
}
