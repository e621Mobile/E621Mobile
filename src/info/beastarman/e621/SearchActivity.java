package info.beastarman.e621;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import info.beastarman.e621.api.E621;
import info.beastarman.e621.api.E621Image;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class SearchActivity extends Activity {
	public static String SEARCH = "search";
	public static String PAGE = "page";
	public static String LIMIT = "limit";
	
	public String search = "";
	public int page = 0;
	public int limit = 20;
	
	private ArrayList<E621Image> e621Images = null;
	private ArrayList<ImageView> imageViews = new ArrayList<ImageView>();
	
	E621 e621 = new E621();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_search);
		
		search = getIntent().getExtras().getString(SearchActivity.SEARCH,"");
		page = getIntent().getExtras().getInt(SearchActivity.PAGE,0);
		limit = getIntent().getExtras().getInt(SearchActivity.LIMIT,20);
		
		((EditText)findViewById(R.id.searchInput)).setText(search);
		
		Resources res = getResources();
		String text = String.format(res.getString(R.string.page_counter), page+1);
		
		TextView page_counter = (TextView) findViewById(R.id.page_counter);
		page_counter.setText(text);
		
		final Handler handler = new SearchHandler(this);
		
		new Thread(new Runnable() {
	        public void run() {
	        	Message msg = handler.obtainMessage();
	        	try {
					msg.obj = e621.post__index(search, page, limit);
				} catch (IOException e) {
					msg.obj = null;
				}
	        	handler.sendMessage(msg);
	        }
	    }).start();
	}
	
	@Override
	public void onStart()
	{
		Log.d("ASD", "onStart");
		
		super.onStart();
		
		if(e621Images != null)
		{
			update_results();
		}
	}
	
	@Override
	public void onStop()
	{
		Log.d("ASD", "onStop");
		
		super.onStop();
		
		for(ImageView img : imageViews)
		{
			Log.d("Msg", "Unloading image");
			
			Drawable drawable = img.getDrawable();
			if (drawable instanceof BitmapDrawable) {
			    BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
			    Bitmap bitmap = bitmapDrawable.getBitmap();
			    bitmap.recycle();
			}
		}
		
		imageViews.clear();
		
		LinearLayout layout = (LinearLayout) findViewById(R.id.content_wrapper);
		layout.removeAllViews();
	}
	
	public void update_results()
	{
		LinearLayout layout = (LinearLayout) findViewById(R.id.content_wrapper);
		layout.removeAllViews();
		
		if(e621Images == null)
		{
			TextView t = new TextView(getApplicationContext());
			t.setText(R.string.no_internet_no_results);
			t.setGravity(Gravity.CENTER_HORIZONTAL);
			t.setPadding(0, 24, 0, 0);
			
			layout.addView(t);
			
			return;
		}
		else if(e621Images.size() == 0)
		{
			TextView t = new TextView(getApplicationContext());
			t.setText(R.string.no_results);
			t.setGravity(Gravity.CENTER_HORIZONTAL);
			t.setPadding(0, 24, 0, 0);
			
			layout.addView(t);
			
			return;
		}
		
		DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		
		for(final E621Image img : e621Images)
		{
			ImageView imgView = new ImageView(getApplicationContext());
			RelativeLayout rel = new RelativeLayout(getApplicationContext());
			ProgressBar bar = new ProgressBar(getApplicationContext());
			
			imageViews.add(imgView);
			
			rel.addView(bar);
			rel.addView(imgView);
			layout.addView(rel);
			
			RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams)bar.getLayoutParams();
			layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
			bar.setLayoutParams(layoutParams);
			
			ImageViewHandler handler = new ImageViewHandler(imgView,dm,bar);
			
			new Thread(new ImageLoadRunnable(handler,img)).start();
			
			BitmapFactory.Options bmOptions;
	        bmOptions = new BitmapFactory.Options();
	        bmOptions.inSampleSize = 1;
		}
	}
	
	public void search(View view)
    {
    	EditText editText = (EditText) findViewById(R.id.searchInput);
    	String search = editText.getText().toString().trim();
    	
    	if(search.length() > 0)
    	{
    		Intent intent = new Intent(this, SearchActivity.class);
    		intent.putExtra(SearchActivity.SEARCH,search);
    		startActivity(intent);
    	}
    }
	
	public void prev(View view)
	{
		if(page > 0)
		{
			Intent intent = new Intent(this, SearchActivity.class);
			intent.putExtra(SearchActivity.SEARCH,search);
			intent.putExtra(SearchActivity.PAGE,page-1);
			intent.putExtra(SearchActivity.LIMIT,limit);
			startActivity(intent);
		}
	}
	
	public void next(View view)
	{
		Intent intent = new Intent(this, SearchActivity.class);
		intent.putExtra(SearchActivity.SEARCH,search);
		intent.putExtra(SearchActivity.PAGE,page+1);
		intent.putExtra(SearchActivity.LIMIT,limit);
		startActivity(intent);
	}
	
	private static class SearchHandler extends Handler
	{
		SearchActivity activity;
		
		public SearchHandler(SearchActivity activity)
		{
			this.activity = activity;
		}
		
		@Override
		public void handleMessage(Message msg)
		{
			ArrayList<E621Image> result = (ArrayList<E621Image>)msg.obj;
			activity.e621Images = result;
			activity.update_results();
		}
	};
	
	private class ImageLoadRunnable implements Runnable
	{
		ImageViewHandler handler;
		E621Image img;
		
		public ImageLoadRunnable(ImageViewHandler handler, E621Image img)
		{
			this.handler = handler;
			this.img = img;
		}
		
		@Override
		public void run() {
			InputStream in = e621.getImage(img, new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES),"cache/"), E621Image.PREVIEW);
        	Message msg = handler.obtainMessage();
        	msg.obj = in;
        	handler.sendMessage(msg);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.search, menu);
		return true;
	}

}
