package info.beastarman.e621.views;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import info.beastarman.e621.R;
import info.beastarman.e621.middleware.E621Middleware;

public class BlackListDialog extends AlertDialog implements DialogInterface.OnClickListener
{
	E621Middleware e621;

	ArrayList<String> queriesToRemove = new ArrayList<String>();

	public BlackListDialog(final Context context)
	{
		super(context);

		setTitle("Blacklist");

		e621 = E621Middleware.getInstance(context);

		LinearLayout wrapper = new LinearLayout(context);
		wrapper.setId(R.id.linearLayout1);
		wrapper.setOrientation(LinearLayout.VERTICAL);
		wrapper.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

		Map<String,Boolean> blacklist = e621.blacklist().getBlacklist();

		for(String key : blacklist.keySet())
		{
			wrapper.addView(getView(key,blacklist.get(key)));
		}

		View blackListAdd = getLayoutInflater().inflate(R.layout.blacklist_add,null);
		blackListAdd.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				final EditText edit = new EditText(context);
				edit.setHint("Type query to blacklist...");

				(new AlertDialog.Builder(context)).setTitle("New blacklist query")
						.setView(edit).setCancelable(false)
						.setPositiveButton("Add",new OnClickListener()
						{
							@Override
							public void onClick(DialogInterface dialogInterface, int i)
							{
								addQuery(edit.getText().toString());
							}
						})
						.setNegativeButton("Cancel",new OnClickListener()
						{
							@Override
							public void onClick(DialogInterface dialogInterface, int i)
							{
							}
						}).create().show();
			}
		});
		wrapper.addView(blackListAdd);

		ScrollView scroll = new ScrollView(context);
		scroll.addView(wrapper);

		setView(scroll);

		setButton(BUTTON_NEGATIVE,"Cancel",new OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialogInterface, int i)
			{
			}
		});
		setButton(BUTTON_POSITIVE,"Save",this);

		setCanceledOnTouchOutside(false);
	}

	public void addQuery(String query)
	{
		if(query.trim().length() == 0)
		{
			Toast.makeText(getContext(),"Query cannot be empty",Toast.LENGTH_SHORT).show();

			return;
		}

		LinearLayout wrapper = (LinearLayout)findViewById(R.id.linearLayout1);

		wrapper.addView(getView(query,true),wrapper.getChildCount()-1);
	}

	private View getView(final String key, Boolean enabled)
	{
		final View blackListEntry = getLayoutInflater().inflate(R.layout.blacklist_item,null);

		TextView query = (TextView)blackListEntry.findViewById(R.id.query);
		query.setText(key);

		ImageView remove = (ImageView)blackListEntry.findViewById(R.id.imageView);
		remove.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				queriesToRemove.add(key);

				((ViewGroup)blackListEntry.getParent()).removeView(blackListEntry);
			}
		});

		final CheckBox checkBox = (CheckBox)blackListEntry.findViewById(R.id.checkBox);
		checkBox.setChecked(enabled);

		final HorizontalScrollView s = (HorizontalScrollView) blackListEntry.findViewById(R.id.horizontalScrollView);

		s.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				checkBox.performClick();
			}
		});

		final Animation a = new Animation()
		{
			@Override
			protected void applyTransformation(float interpolatedTime, Transformation t)
			{
				int scrollLength = s.getChildAt(0).getWidth() - s.getWidth();

				s.scrollTo((int)(scrollLength*interpolatedTime),0);
			}
		};

		s.post(new Runnable()
		{
			@Override
			public void run()
			{
				int scrollLength = s.getChildAt(0).getWidth() - s.getWidth();

				if(scrollLength > 0)
				{
					a.setDuration(scrollLength * 20);
					s.startAnimation(a);
					a.setRepeatMode(Animation.REVERSE);
					a.setRepeatCount(Animation.INFINITE);
					a.setStartOffset(1000);
					((View) s.getParent()).invalidate();
				}
			}
		});

		blackListEntry.setTag("BLACK");

		return blackListEntry;
	}

	@Override
	public void onClick(DialogInterface dialogInterface, int i)
	{
		LinearLayout wrapper = (LinearLayout)findViewById(R.id.linearLayout1);

		final HashMap<String,Boolean> list = new HashMap<String, Boolean>();

		for(i=0; i<wrapper.getChildCount(); i++)
		{
			View v = wrapper.getChildAt(i);

			if(v.getTag()==null || !v.getTag().equals("BLACK"))
			{
				continue;
			}

			String query = ((TextView) v.findViewById(R.id.query)).getText().toString();
			Boolean active = ((CheckBox) v.findViewById(R.id.checkBox)).isChecked();

			list.put(query,active);
		}

		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				for(String query : queriesToRemove)
				{
					e621.blacklist().remove(query);
				}

				for(String query : list.keySet())
				{
					if(list.get(query))
					{
						e621.blacklist().enable(query);
					}
					else
					{
						e621.blacklist().disable(query);
					}
				}
			}
		}).start();
	}
}
