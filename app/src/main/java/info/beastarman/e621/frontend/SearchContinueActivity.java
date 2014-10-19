package info.beastarman.e621.frontend;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import java.io.IOException;

import info.beastarman.e621.R;
import info.beastarman.e621.api.E621Image;
import info.beastarman.e621.api.E621Search;
import info.beastarman.e621.middleware.E621Middleware.InterruptedSearch;
import info.beastarman.e621.middleware.OnlineContinueImageNavigator;

public class SearchContinueActivity extends SearchActivity
{
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		InterruptedSearch ids = e621.get_continue_ids(search);
		
		if(ids == null || !ids.is_valid())
		{
			Intent intent = new Intent(this, SearchActivity.class);
			intent.putExtra(SearchActivity.SEARCH, search);
			startActivity(intent);
			
			finish();
		}
		else
		{
			if(min_id != null)
			{
				min_id = Math.min(min_id, ids.min_id);
			}
			else
			{
				min_id = ids.min_id;
			}
			
			if(max_id != null)
			{
				max_id = Math.max(max_id,ids.max_id);
			}
			else
			{
				max_id = ids.max_id;
			}
		}
	}
	
	@Override
	protected Integer getSearchResultsPages(String search, int limit)
	{
		return e621.getSearchContinueResultsPages(search,limit);
	}
	
	@Override
	protected E621Search get_results(int page)
	{
		try {
			return e621.continue_search(search, page, limit);
		} catch (IOException e) {
			return null;
		}
	}

	@Override
	protected void goToPage(int newPage)
	{
		Intent intent = new Intent(this, SearchContinueActivity.class);
		intent.putExtra(SearchContinueActivity.SEARCH, search);
		intent.putExtra(SearchContinueActivity.PAGE, newPage);
		intent.putExtra(SearchContinueActivity.LIMIT, limit);
		intent.putExtra(SearchContinueActivity.MIN_ID, cur_min_id);
		intent.putExtra(SearchContinueActivity.MAX_ID, cur_max_id);
		intent.putExtra(SearchContinueActivity.PREVIOUS_PAGE, page);

		if(nextE621Search != null && newPage==page+1)
		{
			intent.putExtra(SearchContinueActivity.PRELOADED_SEARCH, nextE621Search);
		}

		startActivity(intent);
	}

	public void imageClick(View view) {
		Intent intent = new Intent(this, ImageActivity.class);
		intent.putExtra(ImageActivity.NAVIGATOR, new OnlineContinueImageNavigator(
				(E621Image) view.getTag(R.id.imageObject),
				(Integer) view.getTag(R.id.imagePosition),
				search,
				limit,
				e621Search));
		intent.putExtra(ImageActivity.INTENT,getIntent());
		startActivity(intent);
	}
}
