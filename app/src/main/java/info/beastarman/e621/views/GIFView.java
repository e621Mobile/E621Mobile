package info.beastarman.e621.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Movie;
import android.graphics.Paint;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.View;

import java.io.InputStream;

import info.beastarman.e621.R;

public class GIFView extends View {
    
    private Movie mMovie;
    private long movieStart;
    private int gifId;

    public void setGIFResource(int resId) {
        this.gifId = resId;
    }

    public int getGIFResource() {
        return this.gifId;
    }
    
    public GIFView(Context context) {
        super(context);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        initializeView();
    }

    public GIFView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        setAttrs(attrs);
        initializeView();
    }

    public GIFView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        setAttrs(attrs);
        initializeView();
    }
    
    private void setAttrs(AttributeSet attrs)
    {
    	if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.GIFView, 0, 0);
            String gifSource = a.getString(R.styleable.GIFView_src);
            //little workaround here. Who knows better approach on how to easily get resource id - please share
            String sourceName = Uri.parse(gifSource).getLastPathSegment().replace(".gif", "");
            setGIFResource(getResources().getIdentifier(sourceName, "drawable", getContext().getPackageName()));
            a.recycle();
        }
    }
    
    public void initializeView()
    {
    	if (gifId != 0)
        {
            InputStream is = getContext().getResources().openRawResource(gifId);
            initializeView(is);
        }
    }

    public void initializeView(InputStream is)
    {
    	mMovie = Movie.decodeStream(is);
        movieStart = 0;
        this.invalidate();
    }
    
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        int desiredWidth = (mMovie != null? mMovie.width() : 0);
        int desiredHeight = (mMovie != null? mMovie.height() : 0);

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int width;
        int height;

        if (widthMode == MeasureSpec.EXACTLY) {
            width = widthSize;
        } else if (widthMode == MeasureSpec.AT_MOST) {
            width = Math.min(desiredWidth, widthSize);
        } else {
            width = desiredWidth;
        }

        if (heightMode == MeasureSpec.EXACTLY) {
            height = heightSize;
        } else if (heightMode == MeasureSpec.AT_MOST) {
            height = Math.min(desiredHeight, heightSize);
        } else {
            height = desiredHeight;
        }

        setMeasuredDimension(width, height);
    }

	private boolean playing = true;

	public void play()
	{
		playing = true;
		this.invalidate();
	}

	public void pause()
	{
		playing = false;
	}

	public void toggle()
	{
		if(playing)
		{
			pause();
		}
		else
		{
			play();
		}
	}

	private Paint getTextPaint()
	{
		Paint textPaint = new Paint();
		textPaint.setARGB(200, 254, 0, 0);
		textPaint.setTextAlign(Paint.Align.LEFT);
		textPaint.setTextSize(32);

		return textPaint;
	}

    @Override
    protected void onDraw(Canvas canvas)
    {
		canvas.drawColor(Color.TRANSPARENT);
        super.onDraw(canvas);
        long now = android.os.SystemClock.uptimeMillis();
        
        if (movieStart == 0)
        {
            movieStart = now;
        }
        
        if (mMovie != null)
        {
            if(playing)
			{
				int relTime = (int) ((now - movieStart) % (mMovie.duration()==0?1:mMovie.duration()));
				mMovie.setTime(relTime);
			}
            
            double scalex = (double) this.getWidth() / (double) mMovie.width();
            double scaley = (double) this.getHeight() / (double) mMovie.height();
			canvas.save();
			canvas.scale((float) scalex, (float) scaley);
			mMovie.draw(canvas, (float) scalex, (float) scaley);
			canvas.restore();
            
            if(playing)
			{
				this.invalidate();
			}
			else
			{
				Paint t = getTextPaint();
				canvas.drawText("Hold to play", 0, 32, t);
			}
        }
    }
}
