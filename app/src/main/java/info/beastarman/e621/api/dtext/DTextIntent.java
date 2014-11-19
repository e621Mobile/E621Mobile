package info.beastarman.e621.api.dtext;

import android.content.Intent;

public class DTextIntent extends DTextObject
{
	public String title;
	public Class c;
	public Intent intent;

	public DTextIntent(String title, Class c, Intent intent)
	{
		this.title = title;
		this.c = c;
		this.intent = intent;
	}
}
