package info.beastarman.e621.frontend;

import info.beastarman.e621.middleware.E621Middleware;
import android.os.Bundle;
import android.preference.PreferenceActivity;

public class SettingsActivity extends PreferenceActivity
{
	@Override
    protected void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
    }
	
	protected void updateTags()
	{
		E621Middleware.getInstance().update_tags(this);
	}
}
