package utils;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import com.catfeed.R;

public class AnimatingItem implements Progress {
	
	private MenuItem item;
	private int countdown = 0;
	
	public AnimatingItem(MenuItem item) {
		this.item = item;
		this.countdown = 1;
	}

	public AnimatingItem(MenuItem item, int countdown) {
		this.item = item;
		this.countdown = countdown;
	}

	@Override
	public void setCountDown(int countdown) {
		this.countdown = countdown;
	}

	@Override
	public void countup() {
		synchronized (this) {
			 this.countdown++;
		}
	}
	
	@Override
	public void countdown() {
		synchronized (this) {
			this.countdown--;
		}
		if (countdown == 0) stop();
	}
	
	public void rotate(Activity activity, int animatingLayoutImageView) {
		LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);		
		ImageView iv = (ImageView) inflater.inflate(animatingLayoutImageView, null);

		Animation rotation = AnimationUtils.loadAnimation(activity, R.anim.rotation);
		rotation.setRepeatCount(Animation.INFINITE);
		iv.startAnimation(rotation);
		item.setActionView(iv);		
	}
	
	@Override
	public void stop() {
		try {
			item.getActionView().clearAnimation();
			item.setActionView(null);				
		}
		catch(Exception e) { /* ignore, if stop gets called more than once it throws */ }
	}

	@Override
	public void progress(int progress, int of) { }
}
