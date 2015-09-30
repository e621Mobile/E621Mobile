package info.beastarman.e621.middleware;

import java.util.HashMap;

import info.beastarman.e621.R;

/**
 * Created by beastarman on 9/30/2015.
 */
public class Mascot
{
	public int image;
	public int blur;
	public String artistName;
	public String artistUrl;

	public Mascot(int image, int blur, String artistName, String artistUrl)
	{
		this.image = image;
		this.blur = blur;
		this.artistName = artistName;
		this.artistUrl = artistUrl;
	}

	@Override
	public boolean equals(Object that)
	{
		if(that instanceof Mascot)
		{
			return this.image == ((Mascot) that).image;
		}

		return false;
	}

	public static HashMap<String,Mascot> getAllMascots()
	{
		HashMap<String,Mascot> ret = new HashMap<String,Mascot>();
		ret.put("Keishinkae", new Mascot(R.drawable.mascot1,R.drawable.mascot1_blur,"Keishinkae","http://www.furaffinity.net/user/keishinkae"));
		ret.put("Keishinkae2", new Mascot(R.drawable.mascot2,R.drawable.mascot2_blur,"Keishinkae","http://www.furaffinity.net/user/keishinkae"));
		ret.put("darkdoomer", new Mascot(R.drawable.mascot3,R.drawable.mascot3_blur,"darkdoomer","http://nowhereincoming.net/"));
		ret.put("Narse", new Mascot(R.drawable.mascot4,R.drawable.mascot4_blur,"Narse","http://www.furaffinity.net/user/narse"));
		ret.put("chizi", new Mascot(R.drawable.mascot0,R.drawable.mascot0_blur,"chizi","http://www.furaffinity.net/user/chizi"));
		ret.put("wiredhooves", new Mascot(R.drawable.mascot5,R.drawable.mascot5_blur,"wiredhooves","http://www.furaffinity.net/user/wiredhooves"));
		ret.put("ECMajor", new Mascot(R.drawable.mascot6,R.drawable.mascot6_blur,"ECMajor","http://www.horsecore.org/"));
		ret.put("evalion", new Mascot(R.drawable.mascot7,R.drawable.mascot7_blur,"evalion","http://www.furaffinity.net/user/evalion"));

		return ret;
	}
}
