package info.beastarman.e621.api.dtext;

import android.text.Spannable;
import android.widget.TextView;

public abstract class DTextRuleStart extends DTextObject
{
	public String name;

	protected DTextRuleStart(String name)
	{
		this.name = name;
	}

	public abstract void apply(Spannable s, TextView v);
}
