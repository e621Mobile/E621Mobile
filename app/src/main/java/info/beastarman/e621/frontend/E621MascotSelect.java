package info.beastarman.e621.frontend;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import info.beastarman.e621.R;
import info.beastarman.e621.middleware.E621Middleware;
import info.beastarman.e621.middleware.Mascot;

public class E621MascotSelect extends E621ConfirmDialogFragment
{
	private E621Middleware e621;
	
	public E621MascotSelect()
	{
		super();
		
		setTitle("Select allowed mascots");
		
		e621 = E621Middleware.getInstance();
	}
	
	@Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
	{
		final LayoutInflater inflater = getActivity().getLayoutInflater();
		
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		
		final View view = inflater.inflate(R.layout.e621_mascot_dialog,null);
		
		final HashMap<String,Mascot> mascots = Mascot.getAllMascots();
		final ArrayList<Mascot> allowed_mascot = new ArrayList<Mascot>(Arrays.asList(e621.getMascots()));
		
		view.post(new Runnable()
		{
			@Override
			public void run()
			{
				final ViewGroup group = (ViewGroup) view.findViewById(R.id.mascotContainer);
				
				for(final String mascotId : mascots.keySet())
				{
					if(group.getChildCount()>0)
					{
						View hr = inflater.inflate(R.layout.hr,null);
						hr.setBackgroundColor(getResources().getColor(R.color.black));
						
						group.addView(hr);
					}
					
					final View v = inflater.inflate(R.layout.mascot_entry,null);

					ImageView img = (ImageView) v.findViewById(R.id.imageView);
					img.setImageResource(mascots.get(mascotId).image);
					
					TextView text = (TextView) v.findViewById(R.id.textView);
					text.setText(mascots.get(mascotId).artistName);
					
					final CheckBox box = (CheckBox) v.findViewById(R.id.checkBox);
					
					if(allowed_mascot.contains(mascots.get(mascotId)))
					{
						box.setChecked(true);
					}
					
					v.setOnClickListener(new OnClickListener()
					{
						@Override
						public void onClick(View arg0)
						{
							box.setChecked(!box.isChecked());
						}
					});
					
					v.setTag(R.id.mascot,mascotId);
					
					group.addView(v);
				}
				
				TextView title = (TextView) view.findViewById(R.id.title);
				title.setText(E621MascotSelect.this.title);
				
				Button confirm = (Button) view.findViewById(R.id.confirmSignUp);
				
				confirm.setText(E621MascotSelect.this.confirmLabel);
				confirm.setOnClickListener(new OnClickListener()
				{
					@Override
					public void onClick(View v)
					{
						ArrayList<String> mascotsToDisallow = new ArrayList<String>();
						ViewGroup group = (ViewGroup) view.findViewById(R.id.mascotContainer);
						
						int i = group.getChildCount();
						
						for(i--;i>=0;i--)
						{
							View child = group.getChildAt(i);
							
							if(child.getTag(R.id.mascot) != null)
							{
								String mascot = (String) child.getTag(R.id.mascot);
								
								CheckBox checkBox = (CheckBox) child.findViewById(R.id.checkBox);
								if(!checkBox.isChecked())
								{
									mascotsToDisallow.add(mascot);
								}
							}
						}
						
						e621.setDisallowedMascots(mascotsToDisallow);
						
						if(confirmRunnable != null)
						{
							confirmRunnable.run();
						}
						
						dismiss();
					}
				});
				
				Button cancel = (Button) view.findViewById(R.id.cancelSignUp);
				
				cancel.setText(E621MascotSelect.this.cancelLabel);
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
