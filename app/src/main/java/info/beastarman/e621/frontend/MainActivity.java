package info.beastarman.e621.frontend;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import info.beastarman.e621.R;
import info.beastarman.e621.middleware.E621Middleware.Mascot;

public class MainActivity extends SlideMenuBaseActivity
{
	Mascot[] mascots;
	
	int previous_mascot = -1;
	
	@Override
    protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_main);
		
		if(e621.isFirstRun())
		{
			AlertDialog.Builder confirmFullUpdateBuilder = new AlertDialog.Builder(this);
			confirmFullUpdateBuilder.setTitle("Welcome to E621 Mobile!");
			confirmFullUpdateBuilder.setMessage(getString(R.string.welcome));
			confirmFullUpdateBuilder.setPositiveButton("Ok", new OnClickListener()
			{
				@Override
				public void onClick(DialogInterface dialogInterface, int which)
				{
				}
			});
			
			confirmFullUpdateBuilder.create().show();
		}
	}
	
	protected void onStart()
	{
		super.onStart();
		
		mascots = e621.getMascots();
		
		change_mascot();
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
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
    
    public void visit_mascot_website(View v)
    {
    	visit_mascot_website();
    }
    
    public void visit_mascot_website()
    {
    	Mascot m;
    	
    	if(previous_mascot >=0 && previous_mascot < mascots.length)
    	{
    		m = mascots[previous_mascot];
    	}
    	else
    	{
    		return;
    	}
    	
    	Intent i = new Intent(Intent.ACTION_VIEW);
    	i.setData(Uri.parse(m.artistUrl));
    	startActivity(i);
    }
    
    public void change_mascot(View v)
    {
    	change_mascot();
    }
    
    public void change_mascot()
    {
    	ImageView mascot = (ImageView)findViewById(R.id.mascot);
    	ImageView mascot_blur = (ImageView)findViewById(R.id.mascot_blur);
    	TextView mascot_by = (TextView)findViewById(R.id.mascotBy);
    	
    	if(mascots.length == 0)
    	{
    		mascot.setVisibility(View.INVISIBLE);
    		mascot_blur.setVisibility(View.INVISIBLE);
    		mascot_by.setVisibility(View.INVISIBLE);
    		
    		return;
    	}
    	else
    	{
    		mascot.setVisibility(View.VISIBLE);
    		mascot_blur.setVisibility(View.VISIBLE);
    		mascot_by.setVisibility(View.VISIBLE);
    	}
    	
    	Mascot m;
    	
    	if(mascots.length == 1)
    	{
    		m = mascots[0];
    	}
    	else
    	{
	    	int random_mascot = (int) (Math.random()*(mascots.length-1));
	    	if(random_mascot >= previous_mascot)
	    	{
	    		random_mascot++;
	    	}
	    	
	    	previous_mascot = random_mascot;
    	
	    	m = mascots[random_mascot%mascots.length];
    	}
    	
    	mascot.setImageResource(m.image);
    	mascot_blur.setImageResource(m.blur);
    	mascot_by.setText("Mascot by " + m.artistName);
    }
    
    public void open_settings()
    {
    	Intent intent;
    	intent = new Intent(this, SettingsActivity.class);
		startActivity(intent);
    }
    
    public void search(View view)
    {
		EditText editText = (EditText) findViewById(R.id.searchInput);
    	String search = editText.getText().toString().trim();
    	
		Intent intent = new Intent(this, SearchActivity.class);
		intent.putExtra(SearchActivity.SEARCH,search);
		startActivity(intent);
    }
    
    public void localSearch(View view)
    {
    	EditText editText = (EditText) findViewById(R.id.searchInput);
    	String search = editText.getText().toString().trim();
    	
		Intent intent = new Intent(this, DownloadsActivity.class);
		intent.putExtra(DownloadsActivity.SEARCH,search);
		startActivity(intent);
    }
}
