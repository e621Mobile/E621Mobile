package info.beastarman.e621.frontend;

import java.io.InputStream;
import java.util.ArrayList;

import info.beastarman.e621.R;
import info.beastarman.e621.middleware.E621DownloadedImage;
import info.beastarman.e621.middleware.E621Middleware;
import info.beastarman.e621.middleware.E621Middleware.InterruptedSearch;
import info.beastarman.e621.middleware.ImageViewHandler;
import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.view.animation.Transformation;

public class SlideMenuBaseActivity extends BaseActivity
{
	private static final int SWIPE_MIN_DISTANCE = 120;
    private static final int SWIPE_MAX_OFF_PATH = 250;
    private static final int SWIPE_THRESHOLD_VELOCITY = 200;
    
    public ArrayList<InterruptedSearch> saved_searches;
	
	private GestureDetector gestureDetector;
    View.OnTouchListener gestureListener;
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        RelativeLayout fullLayout = (RelativeLayout) getLayoutInflater().inflate(R.layout.activity_base, null);
        super.setContentView(fullLayout);
        
        getActionBar().setDisplayHomeAsUpEnabled(true);
        
        gestureDetector = new GestureDetector(this, new MyGestureDetector());
        gestureListener = new View.OnTouchListener()
        {
            public boolean onTouch(View v, MotionEvent event)
            {
                return gestureDetector.onTouchEvent(event);
            }
        };
        
        fullLayout.setOnTouchListener(gestureListener);
    }
	
	protected void onStart()
	{
		super.onStart();
		
		getWindow().getDecorView().post(new Runnable()
        {
        	@Override
			public void run()
        	{
        		update_sidebar();
			}
        });
	}
	
	protected void onPause()
	{
		super.onPause();
		
		if(sidemenu_is_open)
		{
			close_sidemenu(0);
		}
	}
	
	private void update_sidebar()
	{
		FrameLayout wrapper = (FrameLayout) findViewById(R.id.sidemenu_wrapper);
		
		if(wrapper == null) return;
		
        final LinearLayout saved_search_container = (LinearLayout) findViewById(R.id.savedSearchContainer);
        saved_search_container.removeAllViews();
        
        new Thread(new Runnable()
        {
        	public void run()
        	{
        		saved_searches = e621.getAllSearches();
        		
        		runOnUiThread(new Runnable()
                {
                	public void run()
                	{
                		for(final InterruptedSearch search : saved_searches)
		                {
		                	View row = getSearchItemView(search);
		                	saved_search_container.addView(row);
		                	
		                	View hr = getLayoutInflater().inflate(R.layout.hr, saved_search_container, false);
		                	saved_search_container.addView(hr);
		                	
		                	row.setTag(R.id.hr, hr);
		                }
                	}
                });
                
                if(saved_searches.size() == 0)
                {
                	final TextView continue_search_label = (TextView) findViewById(R.id.continue_search_label);
                	
                	runOnUiThread(new Runnable()
                	{
                		public void run()
                		{
                			continue_search_label.setTextColor(getResources().getColor(R.color.gray));
                		}
                	});
                }
        	}
        }).start();
        
        loginout_front();
	}
	
	public void sync(View v)
	{
		new Thread(new Runnable()
		{
			public void run()
			{
				e621.sync();
				
				runOnUiThread(new Runnable()
				{
					public void run()
					{
						update_sidebar();
					}
				});
			}
		}).start();
	}
	
	private View getSearchItemView(final InterruptedSearch search)
	{
		RelativeLayout row = new RelativeLayout(getApplicationContext());
		row.setPadding(dpToPx(10), 0, 0, 0);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				dpToPx(36+16));
		row.setLayoutParams(params);
		
		final ImageView img = new ImageView(getApplicationContext());
		img.setBackgroundResource(android.R.drawable.ic_menu_gallery);
		RelativeLayout.LayoutParams rparams = new RelativeLayout.LayoutParams(
				dpToPx(36+16),
				dpToPx(36+16));
		img.setPadding(0, dpToPx(8), 0, dpToPx(8));
		img.setLayoutParams(rparams);
		
		Bitmap bmp = e621.getContinueSearchThumbnail(search.search);
		if(bmp != null)
		{
			drawInputStreamToImageView(bmp,img);
		}
		
		TextView text = new TextView(getApplicationContext());
		text.setText(search.search);
		text.setPadding(dpToPx(36+12+10), 0, 0, 0);
		text.setTextAppearance(getApplicationContext(), android.R.attr.textAppearanceSmall);
		text.setTextColor(getResources().getColor(R.color.white));
		rparams = new RelativeLayout.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.MATCH_PARENT);
		text.setLayoutParams(rparams);
		text.setGravity(Gravity.CENTER_VERTICAL);
		
		if(search.new_images > 0)
		{
			text.setPadding(text.getPaddingLeft(), 0, 0, dpToPx(8));
			
			TextView new_count = new TextView(getApplicationContext());
			new_count.setText(String.valueOf(search.new_images) + " new");
			new_count.setPadding(0,0,dpToPx(8),0);
			new_count.setTextAppearance(getApplicationContext(), android.R.attr.textAppearanceSmall);
			new_count.setTextSize(getResources().getDimension(R.dimen.new_images_text_size));
			new_count.setTextColor(getResources().getColor(R.color.white));
			rparams = new RelativeLayout.LayoutParams(
					ViewGroup.LayoutParams.WRAP_CONTENT,
					ViewGroup.LayoutParams.WRAP_CONTENT);
			rparams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
			rparams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
			new_count.setLayoutParams(rparams);
			row.addView(new_count);
		}
		
		row.addView(img);
		row.addView(text);
		
		row.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View arg0)
			{
				Intent intent = new Intent(getApplication(), SearchContinueActivity.class);
				intent.putExtra(DownloadsActivity.SEARCH,search.search);
				startActivity(intent);
			}
		});
		
		row.setOnLongClickListener(new OnLongClickListener()
		{
			@Override
			public boolean onLongClick(final View v) {
				final SlideMenuBaseActivity activity = SlideMenuBaseActivity.this;
				
				E621ConfirmDialogFragment fragment = new E621ConfirmDialogFragment();
				fragment.setTitle("Do you want to remove this search?");
				fragment.setConfirmLabel("Yes");
				fragment.setCancelLabel("No");
				
				fragment.setConfirmRunnable(new Runnable()
				{
					public void run()
					{
						e621.removeSearch(search.search);
						activity.removeSearch(v);
					}
				});
				
				fragment.show(getFragmentManager(), "RemoveSearchDialog");
				
				return true;
			}
		});
		
		return row;
	}
	
	public void removeSearch(View v)
	{
		View hr = (View)v.getTag(R.id.hr);
		
		((ViewGroup)v.getParent()).removeView(v);
		((ViewGroup)hr.getParent()).removeView(hr);
	}
	
	@Override
	public void setContentView(final int layoutResID)
	{
		RelativeLayout view_container= (RelativeLayout) findViewById(R.id.view_container);
        getLayoutInflater().inflate(layoutResID, view_container, true);
        
        view_container.post(new Runnable()
        {
        	public void run()
        	{
        		View arrow = findViewById(R.id.sidebar_arrow);
        		
        		if(e621.isFirstRun() && arrow!=null)
        		{
        			arrow.setVisibility(View.VISIBLE);
        			
        			Animation animation = AnimationUtils.loadAnimation(SlideMenuBaseActivity.this, R.anim.alpha_glow);
        			arrow.startAnimation(animation);
        		}
        	}
        });
	}
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case android.R.id.home:
            	toggle_sidebar();
            	
            	return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
	
	public void sendErrorReport(View v)
	{
		uncaughtException();
	}
	
	public void open_favs(View v)
	{
		if(e621.isLoggedIn())
		{
			Intent intent = new Intent(this, SearchActivity.class);
			intent.putExtra(SearchActivity.SEARCH,"fav:"+e621.getLoggedUser());
			startActivity(intent);
		}
	}
	
	boolean user_is_open = false;
	
	public void userClick(View v)
	{
		if(!user_is_open)
		{
			open_user();
		}
		else
		{
			close_user();
		}
	}
	
	public void open_user()
	{
		FrameLayout wrapper = (FrameLayout) findViewById(R.id.userOptionsWrapper);
		ImageView arrow = (ImageView) findViewById(R.id.continue_arrow_user);
		
		open_sidemenu_submenu(wrapper,arrow);
		
		user_is_open = true;
	}
	
	public void close_user()
	{
		FrameLayout wrapper = (FrameLayout) findViewById(R.id.userOptionsWrapper);
		ImageView arrow = (ImageView) findViewById(R.id.continue_arrow_user);
		
		close_sidemenu_submenu(wrapper,arrow);
		
		user_is_open = false;
	}
	
	boolean continue_is_open = false;
	
	public void toggleContinueSearch(View v)
	{
		toggleContinueSearch();
	}
	
	public void toggleContinueSearch()
	{
		if(saved_searches.size() == 0)
		{
			Toast.makeText(getApplicationContext(), "No saved searches yet. Add some on the options menu in any online search.", Toast.LENGTH_SHORT).show();
			
			return;
		}
		
		if(!continue_is_open)
		{
			open_continue();
		}
		else
		{
			close_continue();
		}
	}
	
	private void open_sidemenu_submenu(final FrameLayout wrapper, final ImageView arrow)
	{
		wrapper.measure(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
	    final int targetHeight = wrapper.getMeasuredHeight();
		
		Animation a = new Animation()
		{
		    @Override
		    protected void applyTransformation(float interpolatedTime, Transformation t)
		    {
		    	ViewGroup.LayoutParams drawerParams = (ViewGroup.LayoutParams) wrapper.getLayoutParams();
		    	
		        drawerParams.height = (int) (interpolatedTime * targetHeight);
		        wrapper.setLayoutParams(drawerParams);
		        
		        arrow.setRotation(270f + (90f*interpolatedTime));
		    }
		};
		
		a.setDuration(300);
		wrapper.startAnimation(a);
		((View)wrapper.getParent()).invalidate();
	}
	
	private void close_sidemenu_submenu(final FrameLayout wrapper, final ImageView arrow)
	{
		wrapper.measure(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
	    final int targetHeight = wrapper.getMeasuredHeight();
		
		Animation a = new Animation()
		{
		    @Override
		    protected void applyTransformation(float interpolatedTime, Transformation t)
		    {
		    	interpolatedTime = 1f - interpolatedTime;
		    	
		    	ViewGroup.LayoutParams drawerParams = (ViewGroup.LayoutParams) wrapper.getLayoutParams();
		    	
		        drawerParams.height = (int) (interpolatedTime * targetHeight);
		        wrapper.setLayoutParams(drawerParams);
		        
		        arrow.setRotation(270f + (90f*interpolatedTime));
		    }
		};

		a.setDuration(300);
		wrapper.startAnimation(a);
		((View)wrapper.getParent()).invalidate();
	}
	
	public void open_continue()
	{
		FrameLayout wrapper = (FrameLayout) findViewById(R.id.savedSearchWrapper);
		ImageView arrow = (ImageView) findViewById(R.id.continue_arrow);
		
		open_sidemenu_submenu(wrapper,arrow);
		
		continue_is_open = true;
	}
	
	public void close_continue()
	{
		FrameLayout wrapper = (FrameLayout) findViewById(R.id.savedSearchWrapper);
		ImageView arrow = (ImageView) findViewById(R.id.continue_arrow);
		
		close_sidemenu_submenu(wrapper,arrow);
		
		continue_is_open = false;
	}
	
	boolean sidemenu_is_open = false;
	
	public void toggle_sidebar()
	{
		if(!sidemenu_is_open)
		{
			open_sidemenu();
		}
		else
		{
			close_sidemenu();
		}
	}
	
	public void open_sidemenu()
	{
		open_sidemenu(300);
	}
	
	public void open_sidemenu(int delta)
	{
		View arrow = findViewById(R.id.sidebar_arrow);
		arrow.clearAnimation();
		arrow.setVisibility(View.GONE);
		
		final FrameLayout wrapper = (FrameLayout) findViewById(R.id.sidemenu_wrapper);
		final LinearLayout sidemenu = (LinearLayout) findViewById(R.id.sidemenu);
		final FrameLayout close_sidemenu_area = (FrameLayout) findViewById(R.id.close_sidemenu_area);
		
		Animation a = new Animation()
		{
		    @Override
		    protected void applyTransformation(float interpolatedTime, Transformation t)
		    {
		    	RelativeLayout.LayoutParams drawerParams = (RelativeLayout.LayoutParams) wrapper.getLayoutParams();
		    	
		    	int width = sidemenu.getWidth();
		    	
		        drawerParams.width = (int) (interpolatedTime * width);
		        wrapper.setLayoutParams(drawerParams);
		        
		        if(interpolatedTime < 0.001f)
		        {
		        	close_sidemenu_area.setVisibility(FrameLayout.GONE);
		        }
		        else
		        {
		        	close_sidemenu_area.setVisibility(FrameLayout.VISIBLE);
		        }
		        
		        close_sidemenu_area.setAlpha(interpolatedTime/2f);
		    }
		};

		a.setDuration(delta);
		wrapper.startAnimation(a);
		
		sidemenu_is_open = true;
	}
	
	public void close_sidemenu(View v)
	{
		close_sidemenu();
	}
	
	public void close_sidemenu()
	{
		close_sidemenu(300);
	}
	
	public void close_sidemenu(int delta)
	{
		final FrameLayout wrapper = (FrameLayout) findViewById(R.id.sidemenu_wrapper);
		final LinearLayout sidemenu = (LinearLayout) findViewById(R.id.sidemenu);
		final FrameLayout close_sidemenu_area = (FrameLayout) findViewById(R.id.close_sidemenu_area);
		
		Animation a = new Animation()
		{
		    @Override
		    protected void applyTransformation(float interpolatedTime, Transformation t)
		    {
		    	interpolatedTime = 1f - interpolatedTime;
		    	
		    	RelativeLayout.LayoutParams drawerParams = (RelativeLayout.LayoutParams) wrapper.getLayoutParams();
		    	
		    	int width = sidemenu.getWidth();
		    	
		        drawerParams.width = (int) (interpolatedTime * width);
		        wrapper.setLayoutParams(drawerParams);
		        
		        if(interpolatedTime < 0.001f)
		        {
		        	close_sidemenu_area.setVisibility(FrameLayout.GONE);
		        }
		        else
		        {
		        	close_sidemenu_area.setVisibility(FrameLayout.VISIBLE);
		        }
		        
		        close_sidemenu_area.setAlpha(interpolatedTime/2f);
		    }
		};

		a.setDuration(delta);
		wrapper.startAnimation(a);
		
		sidemenu_is_open = false;
	}
	
	class MyGestureDetector extends SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            try {
                if(
                	(Math.abs(e1.getY() - e2.getY()) <= SWIPE_MAX_OFF_PATH) &&
                	(e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE) &&
                	(Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY)
                )
                {
                    toggle_sidebar();
                }
            } catch (Exception e) {
                // nothing
            }
            return false;
        }

        @Override
        public boolean onDown(MotionEvent e)
        {
        	return true;
        }
    }
	 
	public void login(View v)
	{
		LoginDialogFragment fragment = new LoginDialogFragment();
		fragment.show(getFragmentManager(), "LoginDialog");
	}
	
	public void logout()
	{
		e621.logout();
		
		loginout_front();
	}
	
	public void logout(View v)
	{
		final SlideMenuBaseActivity activity = this;
		
		E621ConfirmDialogFragment fragment = new E621ConfirmDialogFragment();
		fragment.setTitle("Do you want to logout?");
		fragment.setConfirmLabel("Yes");
		fragment.setCancelLabel("No");
		
		fragment.setConfirmRunnable(new Runnable()
		{
			public void run()
			{
				activity.logout();
			}
		});
		
		fragment.show(getFragmentManager(), "LogoutDialog");
	}
	
	public void dummy(View v)
	{
	}
	
	@Override
	public void onBackPressed()
	{
		if(sidemenu_is_open)
		{
			close_sidemenu();
		}
		else
		{
			super.onBackPressed();
		}
	}
	
	public void loginout_front()
	{
		if(e621.getLoggedUser() == null)
		{
			runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					logout_front();
				}
			});
		}
		else
		{
			runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					login_front();
				}
			});
		}
	}
	
	public void login_front()
	{
		View usernameArea = findViewById(R.id.usernameArea);
		View signInArea = findViewById(R.id.signInArea);
		
		usernameArea.setVisibility(View.VISIBLE);
		signInArea.setVisibility(View.GONE);
		
		TextView username = (TextView) findViewById(R.id.usernameText);
		
		username.setText(e621.getLoggedUser());
	}
	
	public void logout_front()
	{

		View usernameArea = findViewById(R.id.usernameArea);
		View signInArea = findViewById(R.id.signInArea);
		
		signInArea.setVisibility(View.VISIBLE);
		usernameArea.setVisibility(View.GONE);
	}
	
	public void confirmSignUp(View v, final String username, final String password, final boolean remember)
	{
		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				if(!e621.login(username, password,remember))
				{
					runOnUiThread(new Runnable()
					{
						@Override
						public void run()
						{
							Toast.makeText(getApplicationContext(), "Could not login. Please try again.", Toast.LENGTH_SHORT).show();
						}
					});
				}
				
				loginout_front();
			}
		}).start();
	}
	
	public void cancelSignUp(View v)
	{
	}
	
	public static class LoginDialogFragment extends DialogFragment
	{
		@Override
	    public Dialog onCreateDialog(Bundle savedInstanceState)
		{
			LayoutInflater inflater = getActivity().getLayoutInflater();
			
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			
			final View view = inflater.inflate(R.layout.sign_up_dialog,null);
			
			view.post(new Runnable()
			{
				@Override
				public void run()
				{
					final SlideMenuBaseActivity activity = ((SlideMenuBaseActivity)getActivity());
					Button confirm = (Button) view.findViewById(R.id.confirmSignUp);
					
					confirm.setOnClickListener(new OnClickListener()
					{
						@Override
						public void onClick(View v)
						{
							EditText username = (EditText) view.findViewById(R.id.username);
							EditText password = (EditText) view.findViewById(R.id.password);
							CheckBox remember = (CheckBox) view.findViewById(R.id.rememberCheckBox);
							
							activity.confirmSignUp(v,username.getText().toString(),password.getText().toString(),remember.isChecked());
							dismiss();
						}
					});
					
					Button cancel = (Button) view.findViewById(R.id.cancelSignUp);
					
					cancel.setOnClickListener(new OnClickListener()
					{
						@Override
						public void onClick(View v)
						{
							activity.cancelSignUp(v);
							dismiss();
						}
					});
				}
			});
			
			builder.setView(view);
			
	        return builder.create();
	    }
	}
}
