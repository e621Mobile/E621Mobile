package info.beastarman.e621.frontend;

import info.beastarman.e621.R;
import info.beastarman.e621.middleware.E621Middleware;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends Activity
{
	E621Middleware e621;
	Mascot[] mascots = new Mascot[]{
			new Mascot(R.drawable.mascot1,R.drawable.mascot1_blur,"Keishinkae","http://www.furaffinity.net/user/keishinkae"),
			new Mascot(R.drawable.mascot2,R.drawable.mascot2_blur,"Keishinkae","http://www.furaffinity.net/user/keishinkae"),
			new Mascot(R.drawable.mascot3,R.drawable.mascot3_blur,"darkdoomer","http://nowhereincoming.net/"),
			new Mascot(R.drawable.mascot4,R.drawable.mascot4_blur,"Narse","http://www.furaffinity.net/user/narse"),
			new Mascot(R.drawable.mascot0,R.drawable.mascot0_blur,"chizi","http://www.furaffinity.net/user/chizi"),
			new Mascot(R.drawable.mascot5,R.drawable.mascot5_blur,"wiredhooves","http://www.furaffinity.net/user/wiredhooves"),
			new Mascot(R.drawable.mascot6,R.drawable.mascot6_blur,"ECMajor","http://www.horsecore.org/"),
			new Mascot(R.drawable.mascot7,R.drawable.mascot7_blur,"evalion","http://www.furaffinity.net/user/evalion"),
	};
	
	int previous_mascot = -1;
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        e621 = new E621Middleware(getApplicationContext());
    }
	
	protected void onStart()
	{
		super.onStart();
		
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
    	
    	int random_mascot = (int) (Math.random()*(mascots.length-1));
    	if(random_mascot >= previous_mascot)
    	{
    		random_mascot++;
    	}
    	
    	previous_mascot = random_mascot;
    	
    	Mascot m = mascots[random_mascot];
    	
    	mascot.setImageResource(m.image);
    	mascot_blur.setImageResource(m.blur);
    	mascot_by.setText("Mascot by " + m.artistName);
    }
    
    public void open_settings()
    {
    	Intent intent;
    	if(Build.VERSION.SDK_INT < 11)
    	{
    		intent = new Intent(this, SettingsActivityOld.class);
    	}
    	else
    	{
    		intent = new Intent(this, SettingsActivityNew.class);
    	}
		startActivity(intent);
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
    
    private class Mascot
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
    }
}
