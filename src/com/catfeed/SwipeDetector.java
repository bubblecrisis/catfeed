package com.catfeed;

import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class SwipeDetector implements View.OnTouchListener {

	public static enum Action {
		LR, // Left to Right
		RL, // Right to Left
		TB, // Top to bottom
		BT, // Bottom to Top
		None // when no action was detected
	}

	private static final String logTag = "SwipeDetector";
	private static final int MIN_DISTANCE = 100;
	private float downX, downY, upX, upY;
	private Action mSwipeDetected = Action.None;

	public boolean swipeDetected() {
		return mSwipeDetected != Action.None;
	}

	public Action getAction() {
		return mSwipeDetected;
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			downX = event.getX();
			downY = event.getY();
			mSwipeDetected = Action.None;
			return false; // allow other events like Click to be processed
		case MotionEvent.ACTION_UP:
			upX = event.getX();
			upY = event.getY();

			float deltaX = downX - upX;
			float deltaY = downY - upY;

			// horizontal swipe detection
			if (Math.abs(deltaX) > MIN_DISTANCE) {
				// left or right
				if (deltaX < 0) {
					Log.i(Constants.LOGTAG, "Swipe Left to Right");
					mSwipeDetected = Action.LR;					
					return swipeRight(v, event);
				}
				if (deltaX > 0) {
					Log.i(Constants.LOGTAG, "Swipe Right to Left");
					mSwipeDetected = Action.RL;
					return swipeLeft(v, event);
				}
			} else if (Math.abs(deltaY) > MIN_DISTANCE) { // vertical swipe
															// detection
				// top or down
				if (deltaY < 0) {
					Log.i(Constants.LOGTAG, "Swipe Top to Bottom");
					mSwipeDetected = Action.TB;
					return swipeDown(v, event);
				}
				if (deltaY > 0) {
					Log.i(Constants.LOGTAG, "Swipe Bottom to Top");
					mSwipeDetected = Action.BT;
					return swipeUp(v, event);
				}
			}
			return false;
		}
		return false;
	}

	public boolean swipeRight(View v, MotionEvent event) {
		return false;
	}
	public boolean swipeLeft(View v, MotionEvent event)  {
		return false;
	}
	public boolean swipeUp(View v, MotionEvent event) {
		return false;
	}
	public boolean swipeDown(View v, MotionEvent event) {
		return false;
	}

}
