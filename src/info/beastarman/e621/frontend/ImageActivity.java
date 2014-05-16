package info.beastarman.e621.frontend;

import java.io.IOException;

import info.beastarman.e621.R;
import info.beastarman.e621.api.E621Image;
import info.beastarman.e621.middleware.E621Middleware;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.content.Intent;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;

public class ImageActivity extends Activity
{
	public static String ID = "id";
	
	public String id= "";
	
	E621Image e621Image = null;
	
	E621Middleware e621;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_image);
		
		e621 = E621Middleware.getInstance();
		
		id= getIntent().getExtras().getString(ImageActivity.ID);
		if(id == null)
		{
			id = "";
		}
		
		final Handler handler = new ImageHandler(this);
		
		new Thread(new Runnable() {
	        public void run() {
	        	Message msg = handler.obtainMessage();
	        	try {
					msg.obj = e621.post__show(id);
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
		super.onStart();
		
		if(e621Image != null)
		{
			update_result();
		}
	}
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_settings:
                open_settings();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    public void open_settings()
    {
    	Intent intent;
    	intent = new Intent(this, SettingsActivityNew.class);
		startActivity(intent);
    }
	
	public void update_result()
	{
		View mainView = getLayoutInflater().inflate(R.layout.activity_image_loaded, null);
		
		mainView.post(new Runnable() 
	    {
	        @Override
	        public void run() 
	        {
	        	if(e621.isSaved(e621Image))
	        	{
	        		ImageButton button = (ImageButton)findViewById(R.id.downloadButton);
	        		button.setImageResource(android.R.drawable.ic_menu_delete);
	        	}
	        	
	        	DisplayMetrics dm = new DisplayMetrics();
	    		getWindowManager().getDefaultDisplay().getMetrics(dm);
	    		dm.widthPixels = findViewById(R.id.content_wrapper).getWidth();
	    		
	    		ImageViewHandler handler = new ImageViewHandler(
	    			(ImageView)findViewById(R.id.imageWrapper),
	    			dm,
	    			findViewById(R.id.progressBarLoader));
	    		
	    		new Thread(new ImageLoadRunnable(handler,e621Image,e621,e621.getFileDownloadSize())).start();
	        }
	    });
		
		setContentView(mainView);
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
	
	public void save_delete(View view)
	{
		if(e621.isSaved(e621Image))
		{
			delete(view);
		}
		else
		{
			save(view);
		}
	}
	
	public void delete(View view)
	{
		final ImageButton button = (ImageButton)view;
		
		e621.deleteImage(e621Image);
		
		button.setImageResource(android.R.drawable.ic_menu_save);
		
		button.setOnClickListener(new View.OnClickListener() {
	        @Override
	        public void onClick(View v) {
	        	save(v);
	        }
	    });
	}
	
	public void save(View view)
	{
		final ImageButton button = (ImageButton)view;
		button.setImageResource(android.R.drawable.stat_sys_download);
		
		e621.saveImageAsync(e621Image, this, new Runnable()
		{
			@Override
			public void run() {
				
				runOnUiThread(new Runnable()
				{
					@Override
					public void run() {
						button.setImageResource(android.R.drawable.ic_menu_delete);
						
						button.setOnClickListener(new View.OnClickListener() {
					        @Override
					        public void onClick(View v) {
					        	delete(v);
					        }
					    });
					}
				});
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.image, menu);
		return true;
	}

	private static class ImageHandler extends Handler
	{
		ImageActivity activity;
		
		public ImageHandler(ImageActivity activity)
		{
			this.activity = activity;
		}
		
		@Override
		public void handleMessage(Message msg)
		{
			E621Image result = (E621Image)msg.obj;
			activity.e621Image = result;
			activity.update_result();
		}
	};
}
