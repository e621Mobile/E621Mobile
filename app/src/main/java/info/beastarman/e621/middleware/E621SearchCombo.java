package info.beastarman.e621.middleware;

import java.io.IOException;

import info.beastarman.e621.api.E621SearchInterface;

/**
 * Created by douglas on 27/01/15.
 */
public class E621SearchCombo implements E621SearchInterface
{
	E621SearchGeneratorCombo comboGenerator;
	int page;
	int limit;

	@Override
	public boolean has_prev_page()
	{
		return current_page(1) > 1;
	}

	@Override
	public boolean has_next_page()
	{
		return current_page(1) < total_pages();
	}

	@Override
	public int current_page()
	{
		return current_page(0);
	}

	@Override
	public int current_page(int diff)
	{
		return page + diff;
	}

	@Override
	public int total_pages()
	{
		try
		{
			return (int) Math.floor(((double) comboGenerator.getCount()) / limit);
		}
		catch(IOException e)
		{
			return 1;
		}
	}
}
