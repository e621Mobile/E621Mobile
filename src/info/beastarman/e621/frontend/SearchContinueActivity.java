package info.beastarman.e621.frontend;

import info.beastarman.e621.api.E621Search;
import info.beastarman.e621.backend.Pair;

import java.io.IOException;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class SearchContinueActivity extends SearchActivity
{
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		Pair<String,String> ids = e621.get_continue_ids(search);
		
		if(ids == null)
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
				min_id = (Integer.parseInt(min_id) < Integer.parseInt(ids.left) ? min_id : ids.left);
			}
			else
			{
				min_id = ids.right;
			}
			
			if(max_id != null)
			{
				max_id = (Integer.parseInt(max_id) > Integer.parseInt(ids.right) ? max_id : ids.right);
			}
			else
			{
				max_id = ids.right;
			}
		}
	}
	
	@Override
	protected E621Search get_results()
	{
		try {
			return e621.continue_search(search, page, limit);
		} catch (IOException e) {
			return null;
		}
	}
	
	@Override
	public void prev(View view)
	{
		if (page > 0)
		{
			if(e621Search != null && !e621Search.has_prev_page())
			{
				return;
			}
			Intent intent = new Intent(this, SearchContinueActivity.class);
			intent.putExtra(SearchActivity.SEARCH, search);
			intent.putExtra(SearchActivity.PAGE, page - 1);
			intent.putExtra(SearchActivity.LIMIT, limit);
			intent.putExtra(SearchActivity.MIN_ID, cur_min_id);
			intent.putExtra(SearchActivity.MAX_ID, cur_max_id);
			startActivity(intent);
		}
	}

	@Override
	public void next(View view)
	{
		if(e621Search != null && !e621Search.has_next_page())
		{
			return;
		}
		
		Intent intent = new Intent(this, SearchContinueActivity.class);
		intent.putExtra(SearchActivity.SEARCH, search);
		intent.putExtra(SearchActivity.PAGE, page + 1);
		intent.putExtra(SearchActivity.LIMIT, limit);
		intent.putExtra(SearchActivity.MIN_ID, cur_min_id);
		intent.putExtra(SearchActivity.MAX_ID, cur_max_id);
		startActivity(intent);
	}
}
