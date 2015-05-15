package info.beastarman.e621.api.dtext;

import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.SubscriptSpan;
import android.text.style.SuperscriptSpan;
import android.text.style.UnderlineSpan;
import android.view.View;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Locale;

public class DTextRuleCollection
{
	public static class DTextRuleStartHeader extends DTextRuleStart
	{
		public int level;

		protected DTextRuleStartHeader(int level)
		{
			super("h");
			this.level = level;
		}

		@Override
		public void apply(Spannable s, TextView tv)
		{
			s.setSpan(new StyleSpan(Typeface.BOLD),0,s.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			s.setSpan(new RelativeSizeSpan(1f + ((6f-level)/5f)), 0, s.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
	}

	public static class DTextRuleStartBold extends DTextRuleStart
	{
		protected DTextRuleStartBold()
		{
			super("b");
		}

		@Override
		public void apply(Spannable s, TextView tv)
		{
			s.setSpan(new StyleSpan(Typeface.BOLD),0,s.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		}

		public static DTextRuleEnd getRuleEnd()
		{
			return new DTextRuleEnd("b");
		}
	}

	public static class DTextRuleStartItalic extends DTextRuleStart
	{
		protected DTextRuleStartItalic()
		{
			super("i");
		}

		@Override
		public void apply(Spannable s, TextView tv)
		{
			s.setSpan(new StyleSpan(Typeface.ITALIC), 0, s.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		}

		public static DTextRuleEnd getRuleEnd()
		{
			return new DTextRuleEnd("i");
		}
	}

	public static class DTextRuleStartUnderlined extends DTextRuleStart
	{
		protected DTextRuleStartUnderlined()
		{
			super("u");
		}

		@Override
		public void apply(Spannable s, TextView tv)
		{
			s.setSpan(new UnderlineSpan(), 0, s.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		}

		public static DTextRuleEnd getRuleEnd()
		{
			return new DTextRuleEnd("u");
		}
	}

	public static class DTextRuleStartStruck extends DTextRuleStart
	{
		protected DTextRuleStartStruck()
		{
			super("s");
		}

		@Override
		public void apply(Spannable s, TextView tv)
		{
			s.setSpan(new StrikethroughSpan(), 0, s.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		}

		public static DTextRuleEnd getRuleEnd()
		{
			return new DTextRuleEnd("s");
		}
	}

	public static class DTextRuleStartSubscript extends DTextRuleStart
	{
		protected DTextRuleStartSubscript()
		{
			super("sub");
		}

		@Override
		public void apply(Spannable s, TextView tv)
		{
			s.setSpan(new SubscriptSpan(), 0, s.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			s.setSpan(new RelativeSizeSpan(0.5f), 0, s.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}

		public static DTextRuleEnd getRuleEnd()
		{
			return new DTextRuleEnd("sub");
		}
	}

	public static class DTextRuleStartSuperscript extends DTextRuleStart
	{
		protected DTextRuleStartSuperscript()
		{
			super("sup");
		}

		@Override
		public void apply(Spannable s, TextView tv)
		{
			s.setSpan(new SuperscriptSpan(),0,s.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			s.setSpan(new RelativeSizeSpan(0.5f), 0, s.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}

		public static DTextRuleEnd getRuleEnd()
		{
			return new DTextRuleEnd("sup");
		}
	}

	public static class DTextRuleStartColor extends DTextRuleStart
	{
		Integer color = null;

		private static HashMap<String,Integer> colors = null;

		public static synchronized HashMap<String,Integer> getColors()
		{
			if(colors == null)
			{
				colors = new HashMap<String, Integer>();

				colors.put("white",Color.parseColor("#FFFFFF"));
				colors.put("ivory",Color.parseColor("#FFFFF0"));
				colors.put("lightyellow",Color.parseColor("#FFFFE0"));
				colors.put("yellow",Color.parseColor("#FFFF00"));
				colors.put("snow",Color.parseColor("#FFFAFA"));
				colors.put("floralwhite",Color.parseColor("#FFFAF0"));
				colors.put("lemonchiffon",Color.parseColor("#FFFACD"));
				colors.put("cornsilk",Color.parseColor("#FFF8DC"));
				colors.put("seashell",Color.parseColor("#FFF5EE"));
				colors.put("lavenderblush",Color.parseColor("#FFF0F5"));
				colors.put("papayawhip",Color.parseColor("#FFEFD5"));
				colors.put("blanchedalmond",Color.parseColor("#FFEBCD"));
				colors.put("mistyrose",Color.parseColor("#FFE4E1"));
				colors.put("bisque",Color.parseColor("#FFE4C4"));
				colors.put("moccasin",Color.parseColor("#FFE4B5"));
				colors.put("navajowhite",Color.parseColor("#FFDEAD"));
				colors.put("peachpuff",Color.parseColor("#FFDAB9"));
				colors.put("gold",Color.parseColor("#FFD700"));
				colors.put("pink",Color.parseColor("#FFC0CB"));
				colors.put("lightpink",Color.parseColor("#FFB6C1"));
				colors.put("orange",Color.parseColor("#FFA500"));
				colors.put("lightsalmon",Color.parseColor("#FFA07A"));
				colors.put("darkorange",Color.parseColor("#FF8C00"));
				colors.put("coral",Color.parseColor("#FF7F50"));
				colors.put("hotpink",Color.parseColor("#FF69B4"));
				colors.put("tomato",Color.parseColor("#FF6347"));
				colors.put("orangered",Color.parseColor("#FF4500"));
				colors.put("deeppink",Color.parseColor("#FF1493"));
				colors.put("fuchsia",Color.parseColor("#FF00FF"));
				colors.put("magenta",Color.parseColor("#FF00FF"));
				colors.put("red",Color.parseColor("#FF0000"));
				colors.put("oldlace",Color.parseColor("#FDF5E6"));
				colors.put("lightgoldenrodyellow",Color.parseColor("#FAFAD2"));
				colors.put("linen",Color.parseColor("#FAF0E6"));
				colors.put("antiquewhite",Color.parseColor("#FAEBD7"));
				colors.put("salmon",Color.parseColor("#FA8072"));
				colors.put("ghostwhite",Color.parseColor("#F8F8FF"));
				colors.put("mintcream",Color.parseColor("#F5FFFA"));
				colors.put("whitesmoke",Color.parseColor("#F5F5F5"));
				colors.put("beige",Color.parseColor("#F5F5DC"));
				colors.put("wheat",Color.parseColor("#F5DEB3"));
				colors.put("sandybrown",Color.parseColor("#F4A460"));
				colors.put("azure",Color.parseColor("#F0FFFF"));
				colors.put("honeydew",Color.parseColor("#F0FFF0"));
				colors.put("aliceblue",Color.parseColor("#F0F8FF"));
				colors.put("khaki",Color.parseColor("#F0E68C"));
				colors.put("lightcoral",Color.parseColor("#F08080"));
				colors.put("palegoldenrod",Color.parseColor("#EEE8AA"));
				colors.put("violet",Color.parseColor("#EE82EE"));
				colors.put("darksalmon",Color.parseColor("#E9967A"));
				colors.put("lavender",Color.parseColor("#E6E6FA"));
				colors.put("lightcyan",Color.parseColor("#E0FFFF"));
				colors.put("burlywood",Color.parseColor("#DEB887"));
				colors.put("plum",Color.parseColor("#DDA0DD"));
				colors.put("gainsboro",Color.parseColor("#DCDCDC"));
				colors.put("crimson",Color.parseColor("#DC143C"));
				colors.put("palevioletred",Color.parseColor("#DB7093"));
				colors.put("goldenrod",Color.parseColor("#DAA520"));
				colors.put("orchid",Color.parseColor("#DA70D6"));
				colors.put("thistle",Color.parseColor("#D8BFD8"));
				colors.put("lightgrey",Color.parseColor("#D3D3D3"));
				colors.put("tan",Color.parseColor("#D2B48C"));
				colors.put("chocolate",Color.parseColor("#D2691E"));
				colors.put("peru",Color.parseColor("#CD853F"));
				colors.put("indianred",Color.parseColor("#CD5C5C"));
				colors.put("mediumvioletred",Color.parseColor("#C71585"));
				colors.put("silver",Color.parseColor("#C0C0C0"));
				colors.put("darkkhaki",Color.parseColor("#BDB76B"));
				colors.put("rosybrown",Color.parseColor("#BC8F8F"));
				colors.put("mediumorchid",Color.parseColor("#BA55D3"));
				colors.put("darkgoldenrod",Color.parseColor("#B8860B"));
				colors.put("firebrick",Color.parseColor("#B22222"));
				colors.put("powderblue",Color.parseColor("#B0E0E6"));
				colors.put("lightsteelblue",Color.parseColor("#B0C4DE"));
				colors.put("paleturquoise",Color.parseColor("#AFEEEE"));
				colors.put("greenyellow",Color.parseColor("#ADFF2F"));
				colors.put("lightblue",Color.parseColor("#ADD8E6"));
				colors.put("darkgray",Color.parseColor("#A9A9A9"));
				colors.put("brown",Color.parseColor("#A52A2A"));
				colors.put("sienna",Color.parseColor("#A0522D"));
				colors.put("yellowgreen",Color.parseColor("#9ACD32"));
				colors.put("darkorchid",Color.parseColor("#9932CC"));
				colors.put("palegreen",Color.parseColor("#98FB98"));
				colors.put("darkviolet",Color.parseColor("#9400D3"));
				colors.put("mediumpurple",Color.parseColor("#9370DB"));
				colors.put("lightgreen",Color.parseColor("#90EE90"));
				colors.put("darkseagreen",Color.parseColor("#8FBC8F"));
				colors.put("saddlebrown",Color.parseColor("#8B4513"));
				colors.put("darkmagenta",Color.parseColor("#8B008B"));
				colors.put("darkred",Color.parseColor("#8B0000"));
				colors.put("blueviolet",Color.parseColor("#8A2BE2"));
				colors.put("lightskyblue",Color.parseColor("#87CEFA"));
				colors.put("skyblue",Color.parseColor("#87CEEB"));
				colors.put("gray",Color.parseColor("#808080"));
				colors.put("olive",Color.parseColor("#808000"));
				colors.put("purple",Color.parseColor("#800080"));
				colors.put("maroon",Color.parseColor("#800000"));
				colors.put("aquamarine",Color.parseColor("#7FFFD4"));
				colors.put("chartreuse",Color.parseColor("#7FFF00"));
				colors.put("lawngreen",Color.parseColor("#7CFC00"));
				colors.put("mediumslateblue",Color.parseColor("#7B68EE"));
				colors.put("lightslategray",Color.parseColor("#778899"));
				colors.put("slategray",Color.parseColor("#708090"));
				colors.put("olivedrab",Color.parseColor("#6B8E23"));
				colors.put("slateblue",Color.parseColor("#6A5ACD"));
				colors.put("dimgray",Color.parseColor("#696969"));
				colors.put("mediumaquamarine",Color.parseColor("#66CDAA"));
				colors.put("cornflowerblue",Color.parseColor("#6495ED"));
				colors.put("cadetblue",Color.parseColor("#5F9EA0"));
				colors.put("darkolivegreen",Color.parseColor("#556B2F"));
				colors.put("indigo",Color.parseColor("#4B0082"));
				colors.put("mediumturquoise",Color.parseColor("#48D1CC"));
				colors.put("darkslateblue",Color.parseColor("#483D8B"));
				colors.put("steelblue",Color.parseColor("#4682B4"));
				colors.put("royalblue",Color.parseColor("#4169E1"));
				colors.put("turquoise",Color.parseColor("#40E0D0"));
				colors.put("mediumseagreen",Color.parseColor("#3CB371"));
				colors.put("limegreen",Color.parseColor("#32CD32"));
				colors.put("darkslategray",Color.parseColor("#2F4F4F"));
				colors.put("seagreen",Color.parseColor("#2E8B57"));
				colors.put("forestgreen",Color.parseColor("#228B22"));
				colors.put("lightseagreen",Color.parseColor("#20B2AA"));
				colors.put("dodgerblue",Color.parseColor("#1E90FF"));
				colors.put("midnightblue",Color.parseColor("#191970"));
				colors.put("aqua",Color.parseColor("#00FFFF"));
				colors.put("cyan",Color.parseColor("#00FFFF"));
				colors.put("springgreen",Color.parseColor("#00FF7F"));
				colors.put("lime",Color.parseColor("#00FF00"));
				colors.put("mediumspringgreen",Color.parseColor("#00FA9A"));
				colors.put("darkturquoise",Color.parseColor("#00CED1"));
				colors.put("deepskyblue",Color.parseColor("#00BFFF"));
				colors.put("darkcyan",Color.parseColor("#008B8B"));
				colors.put("teal",Color.parseColor("#008080"));
				colors.put("green",Color.parseColor("#008000"));
				colors.put("darkgreen",Color.parseColor("#006400"));
				colors.put("blue",Color.parseColor("#0000FF"));
				colors.put("mediumblue",Color.parseColor("#0000CD"));
				colors.put("darkblue",Color.parseColor("#00008B"));
				colors.put("navy",Color.parseColor("#000080"));
				colors.put("black",Color.parseColor("#000000"));
			}

			return colors;
		}

		protected DTextRuleStartColor(String color)
		{
			super("color");

			if(color == null)
			{
				return;
			}

			color = color.trim().toLowerCase(Locale.US);

			if(getColors().containsKey(color))
			{
				this.color = getColors().get(color);
			}
			else
			{
				if(color.matches("#([0-9a-f]){3}"))
				{
					color = color.substring(0,1) + color.substring(1,2) + color.substring(1,2) + color.substring(2,3) + color.substring(2,3) + color.substring(3,4) + color.substring(3,4);
				}

				try
				{
					this.color = Color.parseColor(color);
				}
				catch (IllegalArgumentException e)
				{
				}
			}
		}

		protected DTextRuleStartColor(int color)
		{
			super("color");
			this.color = color;
		}

		@Override
		public void apply(Spannable s, TextView tv)
		{
			if(color == null) return;

			s.setSpan(new ForegroundColorSpan(color),0,s.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		}

		public static DTextRuleEnd getRuleEnd()
		{
			return new DTextRuleEnd("color");
		}
	}

	public static class DTextRuleStartSpoiler extends DTextRuleStart
	{
		protected DTextRuleStartSpoiler()
		{
			super("spoiler");
		}

		boolean hidden = true;

		@Override
		public void apply(Spannable s, TextView tv)
		{
			s.setSpan(getClickableSpanLink(),0,s.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		}

		public static DTextRuleEnd getRuleEnd()
		{
			return new DTextRuleEnd("spoiler");
		}


		SpoilerSpan clickableSpan = null;
		private synchronized SpoilerSpan getClickableSpanLink()
		{
			return new SpoilerSpan();
		}

		private class SpoilerSpan extends ClickableSpan
		{
			public void setShown(boolean shown)
			{
				hidden = !shown;
			}

			public boolean getShown()
			{
				return !hidden;
			}

			@Override
			public void onClick(View widget)
			{
				setShown(!getShown());

				widget.invalidate();
			}

			@Override
			public void updateDrawState(TextPaint ds)
			{
				if(getShown())
				{
					ds.setColor(Color.WHITE);
					ds.bgColor = 0xFF0A1238;
				}
				else
				{
					ds.setColor(0xFF0A1238);
					ds.bgColor = 0xFF0A1238;
				}
			}
		}
	}
}
