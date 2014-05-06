package info.beastarman.e621;

import android.os.Build;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

public class MainActivity extends Activity
{
	E621Middleware e621;
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        e621 = new E621Middleware(getApplicationContext());
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
}
