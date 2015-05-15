package info.beastarman.e621.views;

import android.content.Context;

import info.beastarman.e621.middleware.BlackList;

public class HighlightDialog extends BlackListDialog
{
	public HighlightDialog(Context context, BlackList blacklist)
	{
		super(context, blacklist);

		setTitle("Highlights");
	}

	@Override
	protected String getAddTitle()
	{
		return "New highlight";
	}

	@Override
	protected String getAddHint()
	{
		return "Type query to highlight...";
	}
}
