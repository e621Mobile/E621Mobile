package info.beastarman.e621.views;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.widget.CheckBox;
import android.widget.EditText;

import info.beastarman.e621.R;

public class AlphaFeatureFeedbackAlertDialog extends AlertDialog
{
	public AlphaFeatureFeedbackAlertDialog(Context context)
	{
		super(context);

		LayoutInflater inflater = getLayoutInflater();

		setView(inflater.inflate(R.layout.alpha_feature_feedback_alert_dialog, null));
		setTitle("Feature Feedback");
	}

	public boolean sendStatistics()
	{
		return ((CheckBox)findViewById(R.id.checkBox)).isChecked();
	}

	public String text()
	{
		return ((EditText)findViewById(R.id.editText)).getText().toString();
	}
}
