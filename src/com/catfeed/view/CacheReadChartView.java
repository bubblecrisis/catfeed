package com.catfeed.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.catfeed.model.Subscription;

/**
 * View for displaying a pie chart on the main screen.
 */
public class CacheReadChartView extends View 
{        
    private static Paint paint;  
    private static int READ_CACHE_SIZE = 6;    
    private RectF cacheBound = new RectF();
    private RectF readBound = new RectF();
    
    private Subscription subscription;
    
    public int color = Color.BLUE;
    
    public CacheReadChartView(Context context, AttributeSet attrs) 
    {
        super(context, attrs); 
        
        // Set up the paint object.
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);
    }
    
    public void setSubscription(Subscription subscription) {
    	this.subscription = subscription;
    	randomizeColourScheme(subscription.title.hashCode());
    }
    
    /**
     * Set a randomized colour scheme
     * @param code
     */
    public void randomizeColourScheme(int code) {
		int hash = Math.abs(code);
		int r = (hash >> 2) % 200; 
		int b = (hash >> 4) % 200;
		int g = (hash >> 8) % 150;
		color = Color.rgb(r, g, b); 
    }
    
    protected void onDraw(Canvas canvas) 
    {
        super.onDraw(canvas);
		cacheBound.set(0, 0, getWidth(), getHeight());
		if (subscription == null) return;
		
		// Outer Circle - cache
        paint.setColor( color );
        paint.setAlpha(100);
        float cached = (subscription.getTotalArticles() == 0)? 0: 
        				(float) subscription.getNoOfCachedArticles() / (float) subscription.getTotalArticles();
        
        canvas.drawArc( cacheBound, -90, cached * 360, true, paint );

        // Inner Circle - unread
        float read_left = getWidth() / READ_CACHE_SIZE;
        float read_top = getHeight() / READ_CACHE_SIZE;
        float read_right = getWidth() - (getWidth() / READ_CACHE_SIZE);
        float read_bottom = getHeight() - (getHeight() / READ_CACHE_SIZE);
        float read =  (subscription.getTotalArticles() == 0)? 0:
        			  (float) (subscription.getTotalArticles() - subscription.getUnreadCount()) / (float) subscription.getTotalArticles();
        
        readBound.set(read_left, read_top, read_right, read_bottom);        
        paint.setAlpha(180);
        canvas.drawArc( readBound, -90, read * 360, true, paint );
   }
}