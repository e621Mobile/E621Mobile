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

	public void inputTest(String input) throws Exception
	{
		inputTest(input,null);
	}

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

		if(classes == null) return;

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
		inputTest("@ass666",new Class[]{U});
		inputTest("original #213.",new Class[]{PL,W});
		inputTest("#hashtag",new Class[]{W});
		inputTest("#post ...",new Class[]{W,S,W});
		inputTest("#post #NoN",new Class[]{W,S,W});
		inputTest("\"gaaay\"",new Class[]{W});
	}

	public void test_whatever() throws Exception
	{
		for(String s : str)
		{
			inputTest(s);
		}
	}

	String[] str = new String[]{
		"[quote]Darkened_Shadow said: \"Uploader requested removal within 48 hours\"... That isn't a valid FFD reason... Is it?...\n" +
				"\n" +
				"That's what the takedown requests form is for, isn't it?\n" +
				"\n" +
				"Unless you already made the takedown request and then came to flag this one for the sake of clarity?\n" +
				"\n" +
				"Anyway, in that case, the FFD reason should be repost:post# here... Unless my memory's really messing around with me. [/quote]\n" +
				"\n" +
				"Eh, you're half right. Takedowns are only for character owners and artists. He is just the poster.\n" +
				"\n" +
				"This really should be flagged as \"Repost of post #....\" but this one stays due to being higher res than the other",
		"[section= Story] And so the story begins.. It was a long a grueling battle, the Anubians had won and were celebrating victory" +
				" - what a finer place to spend it than at the towns heart and center! Drugs, wines, women, men, and spirits flowed freely" +
				" at the local tavern. The men were each treated like royalty, their own personal servants to cater to their every whim and" +
				" need. Only the best women of the city were selected for such a task to leave not a'one any room disappointment. On their" +
				" return they were greeted with cheers and bellows from the townsfolk that gathered en-masse to witness the return from battle." +
				" The sounds of bellows and horses galloping in was a prominent ring throughout the streets as the bell tolled it's victory." +
				" Echoing the call of the return of the men, words passed ear to ear that celebrations were to begin! The first treat for the men" +
				" was the private baths, Naela took first sight of Anubis and her heart had skipped a beat as she was off to drag the broad chested" +
				" male for what was just the beginning of a luxurious night. Even dappled and dusted with sand, he was only more alluring to lady." +
				" \"Come come.\" She had motioned in his direction, her voice smooth - calm - dripping of sugary sweetness. That licentious sway of" +
				" her hips was easy to coax a lead from the man. He'd notice she was clad only in a simple wrap of silk at her hips, it left nothing" +
				" to the imagination. Her scent of jasmine and melon left it the air to reach any willing muzzle. The little red panda hadn't wasted" +
				" a breath as he was led off out of sight from the others, the woman having immediately assisted with removing the weighted armor." +
				" Clinks audible as each piece was dropped one by one to the floor and was set to the side. Taking every chance to tease the man" +
				" as her breasts occasionally brushed against him as she catered ev er so attentive to his needs. The room was already filled" +
				" with a steaming heated bath, wine ready to be poured, a side meal of breads, fruit, and cheeses for him to enjoy. Nimble digits" +
				" teased along his back, right on down to his rump as he was gently directed into the waiting waters. She had knelt down to" +
				" the side once he was in, beginning to pour him his first glass of wine of the evening. Raising it to settle it within his" +
				" hand as she proceeded to wash him down, lathering up the soaps to work out all the dust, blood, and the like from the battlefield." +
				" That ever so sweet smile never fading from her lips, even as she worked herself around him ? taking special care of his firm chest," +
				" legs, and manhood. Not much time would be spent in the baths, but long enough for him to relax and enjoy what was set before him ?" +
				" and for Naela to question the story of win in battle if he chose to tell it. He'd be towel dried and wrapped in the finest linens," +
				" only to be lead to the tavern for an evening of song, drink, food, and th e display of assorted women. He'd meet up with his" +
				" comrades over drink to boast of the tales, the women catering to each man to ensure they had everything they asked for." +
				" All were dressed in the same fashion, breasts on display with little wraps of silk being all there was to grace their frames." +
				" Each a personal 'dish' to whom they were to be with for the evening. The drinks were flowing, the music upbeat, the women" +
				" ensuring each mug was filled and refilled with never ending spirits, the spread of food for one and all to enjoy. The" +
				" gathering went on into the evening, even as others stumbled off, Naela remained behind with Anubis until he had his fill." +
				" Whether there were still any lingering it wasn't about to matter. It was easy to be aroused with her little rump teasing" +
				" against the thin linen fabric, the only thing between her and his manhood. There was not a moments notice before Anubis" +
				" had the girl bent over the table before him, her lengthy plush tail curled up and over her shoulders as her aroused" +
				" petals were on display before him. As well as that silken rear that had been taunting him all night, his aroused cock" +
				" took no time to find her lower set of lips. Already moist with excitement coupled with arousal as he grabbed hold of" +
				" her hips. Plunging that thick shaft deep into that smooth as silk tunnel, immediately lifting the girl off of her feet" +
				" and further over the tables edge. She let out a pleasured squeal, he too ground teeth emitting rumbles ? being away" +
				" at battle there was a lack of something. And that was something warm to fulfill those needs of a man. Her fingertips" +
				" clawed into the table as hips rocked back and slammed deeper, breasts bouncing forward with each thrust. Her mouth agape" +
				" as each time he drew back the thrusts were even more rapid and intense, the sound of the table screeching across the" +
				" floor, chairs falling over as the table moved with a clank here and there. With an easy reach around Anubis was able" +
				" to take a good hold of the red pandas deep red mane, keeping her positioned on all fours as head was arched and back" +
				" along with it. Her tongue rolling out past lips as warm juices took every withdraw as a sign of escape, dribbling down" +
				" her inner thighs as inner walls fiercely massaged his throbbing aching shaft that only begged for pleasure and release." +
				" The slapping of his hips tapping against her perfectly plump rear as knot threatened against those achingly spread lower" +
				" lips. That arch in her back directing the tip of his cock over her g-spot, taunting and teasing until even the girl" +
				" couldn't take it any further. Naela couldn't hold back any longer, her body shaking as he continued against her sweetest" +
				" of spots. He'd feel that warm rush of her orgasm spreading over his cock as her body trembled beneath his hold, it" +
				" cause her inner walls to grow just that much more sensitive and tighter as Anubis continued to enjoy the little harlot." +
				" His prick being mouthed until he too was brought to his climax, she could feel the quiver of his cock at the release ?" +
				" not to mention the sound a jackal makes. The rush painting her inner walls white with hot cum, her inner thighs and his" +
				" shaft and balls coated with the thick substance. But one orgasm wasn't enough to halt him from enjoying what was left" +
				" of the night. To those lingering it would be quite a show, but one wouldn't be wrong to say they too were beginning to partake in the women as well.",
		"To think that this was almost post # 400000...",
		"\n" +
				"\n" +
				"[quote]MedizinePlus said: More like 25% balls, 15% dick and 60% cum\n" +
				"\n" +
				"[/quote]\n" +
				"\n" +
				"thumb #[222193]\n",
		"\"Talin is a very bad influence on a young boy... especially a young boy like Nir! While he teases young Nir, giving him false sense of security" +
				" of having a fun time with a small raptor, ends up sneaking the young male into a club with a couple of well hung males. After enticing the" +
				" two, the raptor sets up his young friend into having quite an intimate night of fun.... just not with the raptor specifically! The two riled" +
				" up males take the two boys in the back, soon filling such impossibly tight holes with impossibly big tools sinking inside. Not that Talin" +
				" seems to mind, but he definitely shows Nir what it's like to be a real sub in a bar full of real men... not that Nir seems to mind. >w>\""
	};
}
