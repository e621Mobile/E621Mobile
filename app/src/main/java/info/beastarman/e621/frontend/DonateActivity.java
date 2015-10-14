package info.beastarman.e621.frontend;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.util.ArrayList;

import info.beastarman.e621.R;
import info.beastarman.e621.backend.DonationManager;
import info.beastarman.e621.views.DynamicLinearLayout;
import info.beastarman.e621.views.ObservableScrollView;
import info.beastarman.e621.views.ScrollViewListener;

public class DonateActivity extends BaseActivity implements Runnable, ScrollViewListener
{
	@Override
    protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_donate);

		TextView t = (TextView) findViewById(R.id.buy_me_porn);
		t.setText(Html.fromHtml(this.getString(R.string.buy_me_porn)));
		
		final EditText paypalEmail = (EditText) findViewById(R.id.paypalEmail);
		paypalEmail.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View arg0)
			{
				ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
				
				ClipData clip = ClipData.newPlainText("simple text", paypalEmail.getText());
				
				clipboard.setPrimaryClip(clip);
				
				Toast.makeText(getApplicationContext(), "Address copied to the clipboard", Toast.LENGTH_SHORT).show();
			}
		});

		TabHost tabHost = (TabHost) findViewById(R.id.tabHost);
		tabHost.setup();
		tabHost.addTab(tabHost.newTabSpec("Newest Donators").setIndicator("Newest Donators").setContent(R.id.newest));
		tabHost.addTab(tabHost.newTabSpec("Top Donators").setIndicator("Top Donators").setContent(R.id.top));
		tabHost.addTab(tabHost.newTabSpec("First Donators").setIndicator("First Donators").setContent(R.id.oldest));
		tabHost.setCurrentTab(1);

		for (int i = 0; i < tabHost.getTabWidget().getChildCount(); i++)
		{
			TextView tv = (TextView) tabHost.getTabWidget().getChildAt(i).findViewById(android.R.id.title);
			tv.setTextColor(getResources().getColor(R.color.white));
		}

		new Thread(this).start();

		ObservableScrollView sv = (ObservableScrollView)findViewById(R.id.scrollView1);
		sv.setScrollViewListener(this);
    }
	
	public void paypalDonate(View v)
	{
		Intent i = new Intent(Intent.ACTION_VIEW);
		i.setData(Uri.parse(getString(R.string.paypal_donation_link)));
		startActivity(i);
	}

	@Override
	public void run()
	{
		DonationManager dm = e621.getDonationManager();

		Float totalDonated = dm.getMonthDonations();

		if(totalDonated != null)
		{
			DecimalFormat df = new DecimalFormat();
			df.setMinimumFractionDigits(2);
			df.setMaximumFractionDigits(2);
			final String total = "$"+df.format(totalDonated);

			runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					((TextView)findViewById(R.id.total_donated)).setText(String.format(getString(R.string.total_donated),total));
				}
			});
		}

		final DynamicLinearLayout newest = (DynamicLinearLayout) findViewById(R.id.newest);
		final DynamicLinearLayout top = (DynamicLinearLayout) findViewById(R.id.top);
		final DynamicLinearLayout oldest = (DynamicLinearLayout) findViewById(R.id.oldest);

		final ArrayList<DonationManager.Donator> topDonators = dm.getDonators();
		final ArrayList<DonationManager.Donator> newestDonators = dm.getNewestDonators();
		final ArrayList<DonationManager.Donator> oldestDonators = dm.getOldestDonators();

		if(topDonators != null)
		{
			runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					top.setAdapter(new DonatorAdapter(topDonators));
				}
			});
		}

		if(newestDonators != null)
		{
			runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					newest.setAdapter(new DonatorAdapter(newestDonators));
				}
			});
		}

		if(oldestDonators != null)
		{
			runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					oldest.setAdapter(new DonatorAdapter(oldestDonators));
				}
			});
		}
	}

	@Override
	public void onScrollChanged(ObservableScrollView scrollView, int x, int y, int oldx, int oldy)
	{
		if(scrollView.isAtBottom())
		{
			loadMore(scrollView);
		}
	}

	private void loadMore(final ObservableScrollView scrollView)
	{
		TabHost tabHost = (TabHost) findViewById(R.id.tabHost);

		final boolean test = ((DynamicLinearLayout)tabHost.getCurrentView()).loadMore(10);

		scrollView.post(new Runnable()
		{
			@Override
			public void run()
			{
				if(test && scrollView.isAtBottom())
				{
					loadMore(scrollView);
				}
			}
		});
	}

	class DonatorAdapter extends BaseAdapter implements ListAdapter
	{
		ArrayList<DonationManager.Donator> donators;

		public DonatorAdapter(ArrayList<DonationManager.Donator> donators)
		{
			super();

			this.donators = donators;
		}

		@Override
		public int getCount()
		{
			return donators.size();
		}

		@Override
		public Object getItem(int i)
		{
			return donators.get(i);
		}

		@Override
		public long getItemId(int i)
		{
			return i;
		}

		@Override
		public View getView(int i, View view, ViewGroup viewGroup)
		{
			View v = getLayoutInflater().inflate(R.layout.view_donator,null);
			DonationManager.Donator don = (DonationManager.Donator)getItem(i);

			TextView donatorView = (TextView)v.findViewById(R.id.donator);
			SpannableString ss = new SpannableString(don.name);

			if(don.url != null)
			{
				ss.setSpan(new URLSpan(don.url.toString()),0,ss.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
				donatorView.setMovementMethod(LinkMovementMethod.getInstance());
			}

			donatorView.setText(ss);

			DecimalFormat df = new DecimalFormat();
			df.setMinimumFractionDigits(2);
			df.setMaximumFractionDigits(2);
			((TextView)v.findViewById(R.id.amount)).setText("$"+df.format(don.ammount));

			return v;
		}
	}
}
