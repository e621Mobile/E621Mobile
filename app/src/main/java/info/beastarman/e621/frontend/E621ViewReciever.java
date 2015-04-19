package info.beastarman.e621.frontend;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.util.List;

import info.beastarman.e621.middleware.E621Middleware;
import info.beastarman.e621.middleware.NowhereToGoImageNavigator;

public class E621ViewReciever extends Activity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Intent i = null;

        try {
            final Uri data = getIntent().getData();
            List<String> path = data.getPathSegments();

            if (path.size() >= 3 &&
                path.get(0).toLowerCase().equals("post") &&
                path.get(1).toLowerCase().equals("show") &&
                path.get(2).matches("^\\d*$"))
            {
                i = new Intent(getApplicationContext(), ImageFullScreenActivity.class);
                i.putExtra(ImageFullScreenActivity.NAVIGATOR, new NowhereToGoImageNavigator(Integer.parseInt(path.get(2))));
            }
            else if(path.size() >= 1 &&
                    path.get(0).toLowerCase().equals("post"))
            {
                if(path.size() == 1)
                {
                    i = new Intent(getApplicationContext(), SearchActivity.class);

                    if(data.getQueryParameter("page") != null &&
                            data.getQueryParameter("page").matches("^\\d*$"))
                    {
                        i.putExtra(SearchActivity.PAGE,Integer.parseInt(data.getQueryParameter("page"))-1);
                    }

                    if(data.getQueryParameter("tags") != null)
                    {
                        i.putExtra(SearchActivity.SEARCH,data.getQueryParameter("tags"));
                    }

                    if(data.getQueryParameter("limit") != null &&
                            data.getQueryParameter("limit").matches("^\\d*$"))
                    {
                        i.putExtra(SearchActivity.LIMIT,Integer.parseInt(data.getQueryParameter("limit")));
                    }
                }
                else if(path.size() >= 2 &&
                        (path.get(1).toLowerCase().equals("index") || path.get(1).toLowerCase().equals("search")))
                {
                    i = new Intent(getApplicationContext(), SearchActivity.class);

                    if(path.size() >= 3 && path.get(2).matches("^\\d*$"))
                    {
                        i.putExtra(SearchActivity.PAGE,Integer.parseInt(path.get(2))-1);
                    }
                    else if(data.getQueryParameter("page") != null &&
                            data.getQueryParameter("page").matches("^\\d*$"))
                    {
                        i.putExtra(SearchActivity.PAGE,Integer.parseInt(data.getQueryParameter("page"))-1);
                    }

                    if(path.size() >= 4)
                    {
                        i.putExtra(SearchActivity.SEARCH,path.get(3));
                    }
                    else if(data.getQueryParameter("tags") != null)
                    {
                        i.putExtra(SearchActivity.SEARCH,data.getQueryParameter("tags"));
                    }

                    if(data.getQueryParameter("limit") != null &&
                            data.getQueryParameter("limit").matches("^\\d*$"))
                    {
                        i.putExtra(SearchActivity.LIMIT,Integer.parseInt(data.getQueryParameter("limit")));
                    }
                }
            }
        }
        catch (Throwable t)
        {
            t.printStackTrace();
        }

        if(i == null)
        {
            i = new Intent(getApplicationContext(), MainActivity.class);
        }

        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
        finish();
    }
}
