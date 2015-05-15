package info.beastarman.e621.api.dtext;

import android.graphics.Typeface;
import android.os.Build;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.StyleSpan;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import info.beastarman.e621.R;
import info.beastarman.e621.views.DTextView;

public class DTextBlockCollection
{
	public static class DTextBlockStartQuote extends DTextBlockStart
	{
		public DTextBlockStartQuote()
		{
			super("quote");
		}

		public static DTextBlockEnd getDTextBlockEnd()
		{
			return new DTextBlockEnd("quote");
		}

		@Override
		public void apply(DTextView dTextView)
		{
			if(dTextView.getContext().getApplicationInfo().targetSdkVersion < Build.VERSION_CODES.JELLY_BEAN)
			{
				dTextView.setBackgroundDrawable(dTextView.getResources().getDrawable(R.drawable.dropshadow));
			}
			else
			{
				dTextView.setBackground(dTextView.getResources().getDrawable(R.drawable.dropshadow));
			}

			int padding = (int) (dTextView.getResources().getDisplayMetrics().xdpi / DisplayMetrics.DENSITY_DEFAULT * 14f);

			dTextView.setPadding(padding, padding, padding, padding);
		}
	}

	public static class DTextBlockStartSection extends DTextBlockStart
	{
		String name;
		boolean expanded;
		TextView tv = null;

		public DTextBlockStartSection(boolean expanded)
		{
			super("quote");

			this.name = "Click to collapse";
			this.expanded = expanded;
		}

		public DTextBlockStartSection(String name, boolean expanded)
		{
			super("section");

			this.name = name;
			this.expanded = expanded;
		}

		public static DTextBlockEnd getDTextBlockEnd()
		{
			return new DTextBlockEnd("section");
		}

		private void updateDTextView(final DTextView dTextView)
		{
			dTextView.post(new Runnable()
			{
				@Override
				public void run()
				{
					LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) dTextView.getLayoutParams();

					if(params == null)
					{
						params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
					}

					int padding = (int) (dTextView.getResources().getDisplayMetrics().xdpi / DisplayMetrics.DENSITY_DEFAULT * 14f);

					if(expanded)
					{
						dTextView.setPadding(padding, padding / 5, padding, padding);

						params.height = RelativeLayout.LayoutParams.WRAP_CONTENT;
					}
					else
					{
						dTextView.setPadding(padding, padding / 5, padding, padding / 5);

						tv.measure(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

						params.height = tv.getMeasuredHeight() + (int) (padding * 2f / 5f);
					}

					dTextView.setLayoutParams(params);
				}
			});
		}

		private Spannable getName(final DTextView dTextView)
		{
			Spannable s;

			if(expanded)
			{
				s = new SpannableString("▼ " + name);
			}
			else
			{
				s = new SpannableString("► " + name);
			}

			s.setSpan(new StyleSpan(Typeface.BOLD), 0, name.length() + 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

			s.setSpan(new ClickableSpan()
			{
				@Override
				public void onClick(View view)
				{
					expanded = !expanded;

					tv.setText(getName(dTextView));

					updateDTextView(dTextView);

					view.invalidate();
				}

				@Override
				public void updateDrawState(TextPaint ds)
				{
				}
			}, 0, name.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

			return s;
		}

		@Override
		public void apply(DTextView dTextView)
		{
			if(dTextView.getContext().getApplicationInfo().targetSdkVersion < Build.VERSION_CODES.JELLY_BEAN)
			{
				dTextView.setBackgroundDrawable(dTextView.getResources().getDrawable(R.drawable.dropshadow));
			}
			else
			{
				dTextView.setBackground(dTextView.getResources().getDrawable(R.drawable.dropshadow));
			}

			int padding = (int) (dTextView.getResources().getDisplayMetrics().xdpi / DisplayMetrics.DENSITY_DEFAULT * 14f);

			tv = new TextView(dTextView.getContext());

			tv.setText(getName(dTextView), TextView.BufferType.SPANNABLE);
			tv.setMovementMethod(LinkMovementMethod.getInstance());
			tv.setPadding(0, 0, 0, padding);

			dTextView.addView(tv);

			updateDTextView(dTextView);
		}
	}
}
