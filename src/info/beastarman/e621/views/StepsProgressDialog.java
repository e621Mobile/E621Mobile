package info.beastarman.e621.views;

import java.util.ArrayList;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;

public class StepsProgressDialog extends ProgressDialog
{
	public StepsProgressDialog(Context context)
	{
		super(context);
		
		setup();
	}
	
	public StepsProgressDialog(Context context, int theme)
	{
		super(context, theme);
		
		setup();
	}

	ArrayList<String> steps = new ArrayList<String>();
	
	private void setup()
	{
		setButton(BUTTON_POSITIVE,"Ok!",new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog,int which)
			{
				StepsProgressDialog.this.dismiss();
			}
		});
		
		setOnShowListener(new OnShowListener()
		{
			@Override
			public void onShow(DialogInterface dialog)
			{
				getButton(BUTTON_POSITIVE).setVisibility(View.GONE);
			}
		});
		
		setIndeterminate(true);
		setCancelable(false);
	}
	
	public StepsProgressDialog addStep(String message)
	{
		steps.add(message);
		
		return this;
	}
	
	private String getStepsMessage()
	{
		if(steps.size() == 0)
		{
			return "";
		}
		
		if(steps.size() == 1)
		{
			return "• " + steps.get(0) + "...";
		}
		
		String message = "";
		
		for(String step : steps.subList(0, steps.size()-1))
		{
			if(message.length() > 0)
			{
				message += " Done\n";
			}
			
			message += "• " + step + "...";
		}
		
		message += " Done\n• " + steps.get(steps.size()-1) + "...";
		
		return message;
	}
	
	public void showStepsMessage()
	{
		setMessage(getStepsMessage());
	}
	
	public void allowDismiss()
	{
		getButton(BUTTON_POSITIVE).setText("Continue at background.");
		getButton(BUTTON_POSITIVE).setVisibility(View.VISIBLE);
	}
	
	public void setDone(String doneMessage)
	{
		String message = getStepsMessage();
		
		if(message.length() == 0)
		{
			message = "• " + doneMessage;
		}
		else
		{
			message += " Done\n• " + doneMessage;
		}
		
		setMessage(message);
		
		setIndeterminateDrawable(new ColorDrawable(Color.TRANSPARENT));
		
		getButton(BUTTON_POSITIVE).setText("Ok!");
		getButton(BUTTON_POSITIVE).setVisibility(View.VISIBLE);
	}
}
