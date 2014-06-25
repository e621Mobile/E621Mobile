package info.beastarman.e621.frontend;

import info.beastarman.e621.R;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class E621ConfirmDialogFragment extends DialogFragment
{
	private String title = "";
	private String confirmLabel = "Confirm";
	private String cancelLabel = "Cancel";
	private Runnable confirmRunnable = null;
	private Runnable cancelRunnable = null;
	
	public void setTitle(String title)
	{
		this.title = title;
	}
	
	public void setConfirmLabel(String confirmLabel)
	{
		this.confirmLabel = confirmLabel;
	}
	
	public void setCancelLabel(String cancelLabel)
	{
		this.cancelLabel = cancelLabel;
	}
	
	public void setConfirmRunnable(Runnable confirmRunnable)
	{
		this.confirmRunnable = confirmRunnable;
	}
	
	public void setCancelRunnable(Runnable cancelRunnable)
	{
		this.cancelRunnable = cancelRunnable;
	}
	
	@Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
	{
		LayoutInflater inflater = getActivity().getLayoutInflater();
		
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		
		final View view = inflater.inflate(R.layout.e621_confirm_dialog,null);
		
		TextView title = (TextView) view.findViewById(R.id.title);
		title.setText(this.title);
		
		view.post(new Runnable()
		{
			@Override
			public void run()
			{
				Button confirm = (Button) view.findViewById(R.id.confirmSignUp);
				
				confirm.setText(E621ConfirmDialogFragment.this.confirmLabel);
				confirm.setOnClickListener(new OnClickListener()
				{
					@Override
					public void onClick(View v)
					{
						if(confirmRunnable != null)
						{
							confirmRunnable.run();
						}
						
						dismiss();
					}
				});
				
				Button cancel = (Button) view.findViewById(R.id.cancelSignUp);
				
				confirm.setText(E621ConfirmDialogFragment.this.cancelLabel);
				cancel.setOnClickListener(new OnClickListener()
				{
					@Override
					public void onClick(View v)
					{
						if(cancelRunnable != null)
						{
							cancelRunnable.run();
						}
						
						dismiss();
					}
				});
			}
		});
		
		builder.setView(view);
		
        return builder.create();
    }
}