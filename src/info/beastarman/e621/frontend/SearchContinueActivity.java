package info.beastarman.e621.frontend;

import info.beastarman.e621.api.E621Search;

import java.io.IOException;

import android.os.Bundle;

public class SearchContinueActivity extends SearchActivity
{
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
	}
	
	protected E621Search get_results()
	{
		try {
			return e621.continue_search(search, page, limit);
		} catch (IOException e) {
			return null;
		}
	}
}
