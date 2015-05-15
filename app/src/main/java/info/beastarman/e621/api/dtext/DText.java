package info.beastarman.e621.api.dtext;

import android.content.Intent;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;

import info.beastarman.e621.frontend.ImageFullScreenActivity;
import info.beastarman.e621.frontend.SearchActivity;
import info.beastarman.e621.middleware.E621Middleware;
import info.beastarman.e621.middleware.NowhereToGoImageNavigator;

public class DText extends DTextObject implements Iterable<DTextObject>, Serializable
{
	ArrayList<DTextObject> objects;

	boolean fail = false;
	String originalText = "";

	public DText(String text)
	{
		objects = new ArrayList<DTextObject>();
		
		parse(text);
	}

	@Override
	public Iterator<DTextObject> iterator()
	{
		return objects.iterator();
	}

	public boolean isEmpty()
	{
		for(DTextObject obj : objects)
		{
			if(obj instanceof DTextString)
			{
				if(((DTextString) obj).text.length() > 0)
				{
					return false;
				}
			}
		}

		return true;
	}

	private void parse(DTextCompiler compiler)
	{
		DTextToken token = null;
		DTextObject lastObj = null;

		try
		{
			while((token = compiler.getToken()) != null)
			{
				if(token instanceof DTextTokenWord)
				{
					if(lastObj == null)
					{
						lastObj = new DTextString(((DTextTokenWord) token).word);

						objects.add(lastObj);
					}
					else
					{
						if(lastObj instanceof DTextString)
						{
							((DTextString) lastObj).text += ((DTextTokenWord) token).word;
						}
						else
						{
							lastObj = new DTextString(((DTextTokenWord) token).word);

							objects.add(lastObj);
						}
					}
				}
				else if(token instanceof DTextTokenSpace)
				{
					if(lastObj != null && !(lastObj instanceof DTextBreakLine))
					{
						if(lastObj instanceof DTextString)
						{
							((DTextString) lastObj).text += " ";
						}
						else
						{
							lastObj = new DTextString(" ");

							objects.add(lastObj);
						}
					}
				}
				else if(token instanceof DTextTokenNewline)
				{
					lastObj = new DTextBreakLine();

					objects.add(lastObj);
				}
				else if(token instanceof DTextTokenLink)
				{
					lastObj = new DTextLink(((DTextTokenLink) token).link, ((DTextTokenLink) token).title);

					objects.add(lastObj);
				}
				else if(token instanceof DTextTokenWiki)
				{
					lastObj = new DTextLink("https://e621.net/wiki/show?title=" + ((DTextTokenWiki) token).wikiPage.trim().replace(" ", "_"), ((DTextTokenWiki) token).title);

					objects.add(lastObj);
				}
				else if(token instanceof DTextTokenSearchTag)
				{
					Intent intent = new Intent();

					intent.putExtra(SearchActivity.SEARCH, ((DTextTokenSearchTag) token).tag);

					lastObj = new DTextIntent(((DTextTokenSearchTag) token).tag, SearchActivity.class, intent);

					objects.add(lastObj);
				}
				else if(token instanceof DTextTokenUser)
				{
					lastObj = new DTextLink("https://e621.net/user?name=" + ((DTextTokenUser) token).userName, "@" + ((DTextTokenUser) token).userName);

					objects.add(lastObj);
				}
				else if(token instanceof DTextTokenList)
				{
					if(lastObj == null || lastObj instanceof DTextBreakLine)
					{
						String[] bullets = {"◦", "•"};

						String bullet = bullets[((DTextTokenList) token).level % bullets.length];

						int i = ((DTextTokenList) token).level;
						String s = "";

						for(; i > 0; i--)
						{
							s += "  ";
						}

						lastObj = new DTextString(s + bullet + " ");

						objects.add(lastObj);
					}
					else
					{
						int i = ((DTextTokenList) token).level;
						String s = "";

						for(; i > 0; i--)
						{
							s += "*";
						}

						compiler.tokenStack.add(0, new DTextTokenWord(s + " "));
					}
				}
				else if(token instanceof DTextTokenHeader)
				{
					if(lastObj == null || lastObj instanceof DTextBreakLine)
					{
						lastObj = new DTextRuleCollection.DTextRuleStartHeader(((DTextTokenHeader) token).level);

						objects.add(lastObj);
					}
					else
					{
						String s = "h" + ((DTextTokenHeader) token).level + ".";

						compiler.tokenStack.add(0, new DTextTokenWord(s + " "));
					}
				}
				else if(token instanceof DTextTokenTag)
				{
					if(((DTextTokenTag) token).tag.equals("b"))
					{
						if(((DTextTokenTag) token).opening)
						{
							lastObj = new DTextRuleCollection.DTextRuleStartBold();
						}
						else
						{
							lastObj = DTextRuleCollection.DTextRuleStartBold.getRuleEnd();
						}

						objects.add(lastObj);
					}
					else if(((DTextTokenTag) token).tag.equals("i"))
					{
						if(((DTextTokenTag) token).opening)
						{
							lastObj = new DTextRuleCollection.DTextRuleStartItalic();
						}
						else
						{
							lastObj = DTextRuleCollection.DTextRuleStartItalic.getRuleEnd();
						}

						objects.add(lastObj);
					}
					else if(((DTextTokenTag) token).tag.equals("u"))
					{
						if(((DTextTokenTag) token).opening)
						{
							lastObj = new DTextRuleCollection.DTextRuleStartUnderlined();
						}
						else
						{
							lastObj = DTextRuleCollection.DTextRuleStartUnderlined.getRuleEnd();
						}

						objects.add(lastObj);
					}
					else if(((DTextTokenTag) token).tag.equals("s"))
					{
						if(((DTextTokenTag) token).opening)
						{
							lastObj = new DTextRuleCollection.DTextRuleStartStruck();
						}
						else
						{
							lastObj = DTextRuleCollection.DTextRuleStartStruck.getRuleEnd();
						}

						objects.add(lastObj);
					}
					else if(((DTextTokenTag) token).tag.equals("sub"))
					{
						if(((DTextTokenTag) token).opening)
						{
							lastObj = new DTextRuleCollection.DTextRuleStartSubscript();
						}
						else
						{
							lastObj = DTextRuleCollection.DTextRuleStartSubscript.getRuleEnd();
						}

						objects.add(lastObj);
					}
					else if(((DTextTokenTag) token).tag.equals("sup"))
					{
						if(((DTextTokenTag) token).opening)
						{
							lastObj = new DTextRuleCollection.DTextRuleStartSuperscript();
						}
						else
						{
							lastObj = DTextRuleCollection.DTextRuleStartSuperscript.getRuleEnd();
						}

						objects.add(lastObj);
					}
					else if(((DTextTokenTag) token).tag.equals("color") && !(((DTextTokenTag) token).opening && ((DTextTokenTag) token).extraValue == null))
					{
						if(((DTextTokenTag) token).opening)
						{
							lastObj = new DTextRuleCollection.DTextRuleStartColor(((DTextTokenTag) token).extraValue);
						}
						else
						{
							lastObj = DTextRuleCollection.DTextRuleStartColor.getRuleEnd();
						}

						objects.add(lastObj);
					}
					else if(((DTextTokenTag) token).tag.equals("o"))
					{
						continue;
					}
					else if(((DTextTokenTag) token).tag.equals("spoiler"))
					{
						if(((DTextTokenTag) token).opening)
						{
							lastObj = new DTextRuleCollection.DTextRuleStartSpoiler();
						}
						else
						{
							lastObj = DTextRuleCollection.DTextRuleStartSpoiler.getRuleEnd();
						}

						objects.add(lastObj);
					}
					else if(((DTextTokenTag) token).tag.equals("quote"))
					{
						if(((DTextTokenTag) token).opening)
						{
							lastObj = new DTextBlockCollection.DTextBlockStartQuote();
						}
						else
						{
							lastObj = DTextBlockCollection.DTextBlockStartQuote.getDTextBlockEnd();
						}

						objects.add(lastObj);
					}
					else if(((DTextTokenTag) token).tag.equals("section"))
					{
						if(((DTextTokenTag) token).opening)
						{
							if(((DTextTokenTag) token).extraValue == null)
							{
								lastObj = new DTextBlockCollection.DTextBlockStartSection(false);
							}
							else
							{
								lastObj = new DTextBlockCollection.DTextBlockStartSection(((DTextTokenTag) token).extraValue, false);
							}
						}
						else
						{
							lastObj = DTextBlockCollection.DTextBlockStartSection.getDTextBlockEnd();
						}

						objects.add(lastObj);
					}
					else if(((DTextTokenTag) token).tag.equals("section,expanded"))
					{
						if(((DTextTokenTag) token).opening)
						{
							if(((DTextTokenTag) token).extraValue == null)
							{
								lastObj = new DTextBlockCollection.DTextBlockStartSection(true);
							}
							else
							{
								lastObj = new DTextBlockCollection.DTextBlockStartSection(((DTextTokenTag) token).extraValue, true);
							}
						}
						else
						{
							lastObj = DTextBlockCollection.DTextBlockStartSection.getDTextBlockEnd();
						}

						objects.add(lastObj);
					}
					else
					{
						String tag = ((DTextTokenTag) token).tag;

						if(!((DTextTokenTag) token).opening)
						{
							tag = "/" + tag;
						}

						if(((DTextTokenTag) token).extraValue != null)
						{
							tag += "=" + ((DTextTokenTag) token).extraValue;
						}

						lastObj = new DTextString("[" + tag + "]");

						objects.add(lastObj);
					}
				}
				else if(token instanceof DTextTokenPageLink)
				{
					if(((DTextTokenPageLink) token).page.equals("post"))
					{
						Intent intent = new Intent();

						intent.putExtra(ImageFullScreenActivity.NAVIGATOR, new NowhereToGoImageNavigator(((DTextTokenPageLink) token).number));

						lastObj = new DTextIntent(((DTextTokenPageLink) token).page + " #" + ((DTextTokenPageLink) token).number, ImageFullScreenActivity.class, intent);

						objects.add(lastObj);
					}
					else if(((DTextTokenPageLink) token).page.equals("thumb"))
					{
						lastObj = new DTextThumb(((DTextTokenPageLink) token).number);

						objects.add(lastObj);
					}
					else if(((DTextTokenPageLink) token).page.equals("forum"))
					{
						lastObj = new DTextLink("https://e621.net/forum/show/" + ((DTextTokenPageLink) token).number,
													   ((DTextTokenPageLink) token).page + " #" + ((DTextTokenPageLink) token).number);

						objects.add(lastObj);
					}
					else if(((DTextTokenPageLink) token).page.equals("category"))
					{
						lastObj = new DTextLink("https://e621.net/forum?category=" + ((DTextTokenPageLink) token).number,
													   ((DTextTokenPageLink) token).page + " #" + ((DTextTokenPageLink) token).number);

						objects.add(lastObj);
					}
					else if(((DTextTokenPageLink) token).page.equals("comment"))
					{
						lastObj = new DTextLink("https://e621.net/comment/show/" + ((DTextTokenPageLink) token).number,
													   ((DTextTokenPageLink) token).page + " #" + ((DTextTokenPageLink) token).number);

						objects.add(lastObj);
					}
					else if(((DTextTokenPageLink) token).page.equals("pool"))
					{
						Intent intent = new Intent();

						intent.putExtra(SearchActivity.SEARCH, "order:id pool:" + ((DTextTokenPageLink) token).number);

						lastObj = new DTextIntent(((DTextTokenPageLink) token).page + " #" + ((DTextTokenPageLink) token).number, SearchActivity.class, intent);

						objects.add(lastObj);
					}
					else if(((DTextTokenPageLink) token).page.equals("set"))
					{
						Intent intent = new Intent();

						intent.putExtra(SearchActivity.SEARCH, "order:id set:" + ((DTextTokenPageLink) token).number);

						lastObj = new DTextIntent(((DTextTokenPageLink) token).page + " #" + ((DTextTokenPageLink) token).number, SearchActivity.class, intent);

						objects.add(lastObj);
					}
					else if(((DTextTokenPageLink) token).page.equals("blip"))
					{
						lastObj = new DTextLink("https://e621.net/blip/show/" + ((DTextTokenPageLink) token).number,
													   ((DTextTokenPageLink) token).page + " #" + ((DTextTokenPageLink) token).number);

						objects.add(lastObj);
					}
					else if(((DTextTokenPageLink) token).page.equals("takedown"))
					{
						lastObj = new DTextLink("https://e621.net/takedown/show/" + ((DTextTokenPageLink) token).number,
													   ((DTextTokenPageLink) token).page + " #" + ((DTextTokenPageLink) token).number);

						objects.add(lastObj);
					}
					else if(((DTextTokenPageLink) token).page.equals("ticket"))
					{
						lastObj = new DTextLink("https://e621.net/ticket/show/" + ((DTextTokenPageLink) token).number,
													   ((DTextTokenPageLink) token).page + " #" + ((DTextTokenPageLink) token).number);

						objects.add(lastObj);
					}
					else if(((DTextTokenPageLink) token).page.equals("record"))
					{
						lastObj = new DTextLink("https://e621.net/user_record/show/" + ((DTextTokenPageLink) token).number,
													   ((DTextTokenPageLink) token).page + " #" + ((DTextTokenPageLink) token).number);

						objects.add(lastObj);
					}
					else
					{
						lastObj = new DTextString(((DTextTokenPageLink) token).page + " #" + ((DTextTokenPageLink) token).number);

						objects.add(lastObj);
					}
				}
			}
		}
		catch(java.lang.Throwable e)
		{
			e.printStackTrace();

			fail = true;
		}
		finally
		{
			if(fail)
			{
				E621Middleware.getInstance().sendReport("DText Error parsing:\n\n" + originalText, false);

				objects.clear();

				objects.add(new DTextString(originalText));
			}
		}
	}

	public void parse(String text)
	{
		objects.clear();

		originalText = text;

		InputStream in = new ByteArrayInputStream((text).getBytes());
		
		DTextCompiler compiler = new DTextCompiler(in);

		parse(compiler);
	}
}
