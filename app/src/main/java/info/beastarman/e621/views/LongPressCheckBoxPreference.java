package info.beastarman.e621.views;

import android.content.Context;
import android.preference.CheckBoxPreference;
import android.view.View;

public abstract class LongPressCheckBoxPreference extends CheckBoxPreference implements View.OnLongClickListener
{
	public LongPressCheckBoxPreference(Context context)
	{
		super(context);
	}

	public abstract boolean onLongClick(View v);
}
