package info.beastarman.e621.views;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

import info.beastarman.e621.R;
import info.beastarman.e621.api.E621Image;
import info.beastarman.e621.api.dtext.DTextBlockEnd;
import info.beastarman.e621.api.dtext.DTextBlockStart;
import info.beastarman.e621.api.dtext.DTextBreakLine;
import info.beastarman.e621.api.dtext.DTextIntent;
import info.beastarman.e621.api.dtext.DTextLink;
import info.beastarman.e621.api.dtext.DTextObject;
import info.beastarman.e621.api.dtext.DTextRuleEnd;
import info.beastarman.e621.api.dtext.DTextRuleStart;
import info.beastarman.e621.api.dtext.DTextString;
import info.beastarman.e621.api.dtext.DTextThumb;
import info.beastarman.e621.frontend.ImageFullScreenActivity;
import info.beastarman.e621.middleware.NowhereToGoImageNavigator;

public class DTextView extends LinearLayout
{
	private static class DTextRules
	{
		HashMap<String,DTextRuleStart> rules = new HashMap<String,DTextRuleStart>();

		TextView tv;
		DTextRules(TextView tv)
		{
			this.tv = tv;
		}

		public void addRule(DTextRuleStart rule)
		{
			rules.put(rule.name,rule);
		}

		public void removeRule(String name)
		{
			rules.remove(name);
		}

		public void apply(Spannable s)
		{
			for(DTextRuleStart r : rules.values())
			{
				r.apply(s,tv);
			}
		}
	}

	private static class SubDTextView extends DTextView
	{
		private SubDTextView(Context context)
		{
			super(context);
		}

		private SubDTextView(Context context, AttributeSet attrs)
		{
			super(context, attrs);
		}

		private SubDTextView(Context context, AttributeSet attrs, int defStyle)
		{
			super(context, attrs, defStyle);
		}

		String endBlock;

		public void setEndBlock(String endBlock)
		{
			this.endBlock = endBlock;
		}

		@Override
		public boolean shouldBreak(DTextObject dObj)
		{
			if(dObj instanceof DTextBlockEnd)
			{
				return ((DTextBlockEnd) dObj).name.equals(endBlock);
			}

			return false;
		}
	}

	public DTextView(Context context)
	{
		super(context);

		setOrientation(VERTICAL);
	}

	public DTextView(Context context, AttributeSet attrs)
	{
		super(context, attrs);

		setOrientation(VERTICAL);
	}

	public DTextView(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);

		setOrientation(VERTICAL);
	}

	private ClickableSpan getClickableSpanLink(final String word) {
		return new ClickableSpan() {
			final String mWord;
			{
				mWord = word;
			}

			@Override
			public void onClick(View widget)
			{
				Intent i = new Intent(Intent.ACTION_VIEW);
				i.setData(Uri.parse(word));
				i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				getContext().startActivity(i);
			}

			public void updateDrawState(TextPaint ds) {
				super.updateDrawState(ds);
			}
		};
	}

	private ClickableSpan getClickableSpanLink(final Intent i) {
		return new ClickableSpan() {
			final Intent intent;
			{
				intent = i;
			}

			@Override
			public void onClick(View widget)
			{
				getContext().startActivity(intent);
			}

			public void updateDrawState(TextPaint ds) {
				super.updateDrawState(ds);
			}
		};
	}

	public void putRule(DTextRuleStart rule, TextView tv)
	{
		if(tv.getTag(R.id.DTextRules) instanceof DTextRules)
		{
			((DTextRules) tv.getTag(R.id.DTextRules)).addRule(rule);
		}
	}

	public void removeRule(DTextRuleEnd rule, TextView tv)
	{
		if(tv.getTag(R.id.DTextRules) instanceof DTextRules)
		{
			((DTextRules) tv.getTag(R.id.DTextRules)).removeRule(rule.name);
		}
	}

	public void putContent(DTextString dStr, TextView tv)
	{
		Spannable s = new SpannableString(dStr.text);

		if(tv.getTag(R.id.DTextRules) instanceof DTextRules)
		{
			((DTextRules) tv.getTag(R.id.DTextRules)).apply(s);
		}

		tv.setText(TextUtils.concat(tv.getText(), s), TextView.BufferType.SPANNABLE);
	}

	public void putContent(DTextLink dLink, TextView tv)
	{
		Spannable s = new SpannableString(dLink.title);
		s.setSpan(getClickableSpanLink(dLink.link),0,s.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

		if(tv.getTag(R.id.DTextRules) instanceof DTextRules)
		{
			((DTextRules) tv.getTag(R.id.DTextRules)).apply(s);
		}

		tv.setText(TextUtils.concat(tv.getText(), s), TextView.BufferType.SPANNABLE);
	}

	public void putContent(DTextIntent dIntent, TextView tv)
	{
		Spannable s = new SpannableString(dIntent.title);

		Intent i = dIntent.intent;
		i.setClass(getContext(),dIntent.c);

		s.setSpan(getClickableSpanLink(i),0,s.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

		if(tv.getTag(R.id.DTextRules) instanceof DTextRules)
		{
			((DTextRules) tv.getTag(R.id.DTextRules)).apply(s);
		}

		tv.setText(TextUtils.concat(tv.getText(), s), TextView.BufferType.SPANNABLE);
	}

	public ImageView imageView(final DTextThumb thumb)
	{
		final ImageView iv = new ImageView(getContext());

		iv.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				Intent i = new Intent(getContext(), ImageFullScreenActivity.class);
				i.putExtra(ImageFullScreenActivity.NAVIGATOR,new NowhereToGoImageNavigator(thumb.id));
				i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				getContext().startActivity(i);
			}
		});

		post(new Runnable()
		{
			@Override
			public void run()
			{
				final int totalWidth = getResources().getDisplayMetrics().widthPixels;

				new Thread(new Runnable()
				{
					@Override
					public void run()
					{
						try
						{
							E621Image img = thumb.getImage();

							final int height;
							final int width;

							int scale = Math.max(1, Math.min(img.preview_width / (totalWidth / 3), img.preview_height / (totalWidth / 3)));

							width = img.preview_width / scale;
							height = img.preview_height / scale;

							final Bitmap bmp = thumb.getBitmap(width, height);

							if (bmp == null)
							{
								return;
							}

							iv.post(new Runnable()
							{
								@Override
								public void run()
								{
									iv.setImageBitmap(bmp);
								}
							});
						} catch (IOException e)
						{
							return;
						}
					}
				}).start();
			}
		});

		return iv;
	}

	public TextView textView()
	{
		TextView tv = new TextView(getContext());

		tv.setMovementMethod(LinkMovementMethod.getInstance());
		tv.setTag(R.id.DTextRules,new DTextRules(tv));

		tv.setText(new SpannableString(""),TextView.BufferType.SPANNABLE);

		return tv;
	}

	public void setDText(Iterable<DTextObject> dText)
	{
		removeAllViews();

		setDText(dText.iterator());
	}

	public boolean shouldBreak(DTextObject dObj)
	{
		return false;
	}

	public SubDTextView startBlock(DTextBlockStart dObj,Iterator<DTextObject> it)
	{
		SubDTextView subView = new SubDTextView(getContext());

		dObj.apply(subView);
		subView.setEndBlock(dObj.name);
		subView.setDText(it);

		return subView;
	}

	public void setDText(Iterator<DTextObject> it)
	{
		View v = null;

		while(it.hasNext())
		{
			DTextObject dObj = it.next();

			if(shouldBreak(dObj)) break;

			if(dObj instanceof DTextString)
			{
				if(v == null)
				{
					TextView tv = textView();

					putContent((DTextString)dObj,tv);

					v = tv;
					addView(tv);
				}
				else
				{
					if(v instanceof TextView)
					{
						TextView tv = (TextView) v;

						putContent((DTextString) dObj, tv);
					}
					else
					{
						TextView tv = textView();

						putContent((DTextString) dObj, tv);

						v = tv;
						addView(tv);
					}
				}
			}
			else if(dObj instanceof DTextLink)
			{
				if(v == null)
				{
					TextView tv = textView();

					putContent(((DTextLink) dObj), tv);

					v = tv;
					addView(tv);
				}
				else
				{
					if(v instanceof TextView)
					{
						TextView tv = (TextView) v;

						putContent(((DTextLink) dObj), tv);
					}
					else
					{
						TextView tv = textView();

						putContent(((DTextLink) dObj), tv);

						v = tv;
						addView(tv);
					}
				}
			}
			else if(dObj instanceof DTextIntent)
			{
				if(v == null)
				{
					TextView tv = textView();

					putContent(((DTextIntent) dObj), tv);

					v = tv;
					addView(tv);
				}
				else
				{
					if(v instanceof TextView)
					{
						TextView tv = (TextView) v;

						putContent(((DTextIntent) dObj), tv);
					}
					else
					{
						TextView tv = textView();

						putContent(((DTextIntent) dObj), tv);

						v = tv;
						addView(tv);
					}
				}
			}
			else if(dObj instanceof DTextRuleStart)
			{
				if(v instanceof TextView)
				{
					TextView tv = (TextView) v;

					putRule((DTextRuleStart) dObj, tv);
				}
				else
				{
					TextView tv = textView();

					putRule((DTextRuleStart)dObj,tv);

					v = tv;
					addView(tv);
				}
			}
			else if(dObj instanceof DTextRuleEnd)
			{
				if(v instanceof TextView)
				{
					TextView tv = (TextView) v;

					removeRule((DTextRuleEnd) dObj, tv);
				}
				else
				{
					continue;
				}
			}
			else if(dObj instanceof DTextBlockStart)
			{
				SubDTextView d = startBlock((DTextBlockStart)dObj,it);

				v = d;
				addView(d);
			}
			else if(dObj instanceof DTextThumb)
			{
				ImageView iv = imageView((DTextThumb)dObj);

				v = iv;
				addView(iv);
			}
			else if(dObj instanceof DTextBreakLine)
			{
				v = null;
			}
		}
	}
}
