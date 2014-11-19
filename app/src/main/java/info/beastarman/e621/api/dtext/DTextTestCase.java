package info.beastarman.e621.api.dtext;

import android.test.InstrumentationTestCase;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;

public class DTextTestCase extends InstrumentationTestCase
{
	private static final Class W = DTextTokenWord.class;
	private static final Class T = DTextTokenTag.class;
	private static final Class S = DTextTokenSpace.class;
	private static final Class N = DTextTokenNewline.class;
	private static final Class L = DTextTokenLink.class;
	private static final Class WK = DTextTokenWiki.class;
	private static final Class ST = DTextTokenSearchTag.class;
	private static final Class PL = DTextTokenPageLink.class;
	private static final Class H = DTextTokenHeader.class;
	private static final Class LI = DTextTokenList.class;
	private static final Class U = DTextTokenUser.class;

	public void inputTest(String input, Class[] classes) throws Exception
	{
		InputStream in = new ByteArrayInputStream(input.getBytes());

		DTextCompiler compiler = new DTextCompiler(in);

		DTextToken token = null;

		ArrayList<Class> realClasses = new ArrayList<Class>();

		while((token = compiler.getToken()) != null)
		{
			realClasses.add(token.getClass());
		}

		assertEquals(Arrays.asList(classes),realClasses);
	}

	public void test() throws Exception
	{
		inputTest("Hello",new Class[]{W});
		inputTest("Hello World!\n",new Class[]{W,S,W,N});
		inputTest("Hello [b]World[/b]",new Class[]{W,S,T,W,T});
		inputTest("http://aol.com",new Class[]{L});
		inputTest("\"e621\":https://e621.net?a[2]",new Class[]{L});
		inputTest("\"e 6 2 1\":https://e621.net?a[2]",new Class[]{L});
		inputTest("[color=red]RED[/color]",new Class[]{T,W,T});
		inputTest("[[bunda]]",new Class[]{WK});
		inputTest("[[bunda | Ass]]",new Class[]{WK});
		inputTest("[[bunda | Ass]]",new Class[]{WK});
		inputTest("{{ gardevoir solo penis }}",new Class[]{ST});
		inputTest("post #2134",new Class[]{PL});
		inputTest("[section,expanded=Custom Title]Lololo[/section]",new Class[]{T,W,T});
		inputTest("h6.Header!\n",new Class[]{H,W,N});
		inputTest("* One\n** T*w*o\n\n*None",new Class[]{LI,W,N,LI,W,N,W});
		inputTest("@beastarman\n",new Class[]{U,N});
		inputTest("h1.@beastarman\n",new Class[]{H,U,N});
		inputTest("\\[b]NOTBOLD\\[/b]",new Class[]{W});
		inputTest("h1.H e a  d 1\n h6.Head6\nh2.h2\nh0.h1.h2.HAHAHA\n\n",new Class[]{H,W,S,W,S,W,S,W,S,W,N,S,H,W,N,H,W,N,W,N});
		inputTest("\"Quote\"\n",new Class[]{W,N});
		inputTest("\"Quotes are soooo soo soooooo kool\"\n",new Class[]{W,S,W,S,W,S,W,S,W,S,W,N});
		inputTest("death’s",new Class[]{W});
	}
}
