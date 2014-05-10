package info.beastarman.e621.frontend;

import info.beastarman.e621.middleware.E621Middleware;
import android.app.ProgressDialog;
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
		//ProgressDialog dialog = ProgressDialog.show(SettingsActivity.this, "","Loading. Please wait...", true);
		E621Middleware.getInstance().update_tags();
		//dialog.dismiss();
	}
}
