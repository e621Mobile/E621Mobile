package info.beastarman.e621.frontend;

import android.app.AlertDialog;
import android.content.Context;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.RelativeLayout;

import java.util.ArrayList;

public class PageSelectorDialog extends AlertDialog
{
	protected PageSelectorDialog(Context context, int maxPages)
	{
		super(context);
		build(context,maxPages);
	}

	protected PageSelectorDialog(Context context, int theme, int maxPages)
	{
		super(context, theme);
		build(context,maxPages);
	}

	protected PageSelectorDialog(Context context, boolean cancelable, OnCancelListener cancelListener, int maxPages)
	{
		super(context, cancelable, cancelListener);
		build(context,maxPages);
	}

	private ArrayList<NumberPicker> pickers = new ArrayList<NumberPicker>();
	private int maxPages;

	public int getValue()
	{
		int num = 0;

		for(NumberPicker p : pickers)
		{
			num = num*10 + p.getValue();
		}

		return num;
	}

	private void fixValues()
	{
		int num = getValue();

		if(num == 0)
		{
			pickers.get(pickers.size()-1).setValue(1);
		}
		else if(num > this.maxPages)
		{
			int maxPages = this.maxPages;

			for(NumberPicker p : pickers)
			{
				if(maxPages == 0)
				{
					p.setValue(0);
					continue;
				}

				int digits = (int)(Math.floor(Math.log10(maxPages))+1);
				int firstDigit = (int)(maxPages/Math.pow(10,digits-1));

				p.setValue(firstDigit);

				maxPages -= (firstDigit * Math.pow(10,digits-1));
			}
		}
	}

	private void build(Context context, int maxPages)
	{
		maxPages = Math.max(1,maxPages);
		this.maxPages = maxPages;

		LinearLayout v = new LinearLayout(context);
		RelativeLayout.LayoutParams rparams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,RelativeLayout.LayoutParams.WRAP_CONTENT);
		rparams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
		v.setLayoutParams(rparams);
		v.setOrientation(LinearLayout.HORIZONTAL);

		int digits = (int)(Math.floor(Math.log10(maxPages))+1);
		int firstDigit = (int)(maxPages/Math.pow(10,digits-1));
		int min = 0;

		if(digits == 1)
		{
			min = 1;
		}

		for(;digits>0;digits--,firstDigit=9)
		{
			NumberPicker picker = new NumberPicker(context);
			picker.setMinValue(min);
			picker.setMaxValue(firstDigit);

			picker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener()
			{
				@Override
				public void onValueChange(NumberPicker numberPicker, int i, int i2)
				{
					fixValues();
				}
			});

			pickers.add(picker);
			v.addView(picker);
		}

		fixValues();

		setTitle("Jump to page...");

		RelativeLayout r = new RelativeLayout(context);
		r.addView(v);

		setView(r);
	}
}
