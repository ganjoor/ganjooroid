/**
 * 
 */
package com.android.ganjoor;

import android.view.MotionEvent;
import android.view.View;

/**
 * http://stackoverflow.com/questions/937313/android-basic-gesture-detection
 *
 */


public class ActivitySwipeDetector implements View.OnTouchListener {

    private SwipeInterface activity;
    static final int MIN_DISTANCE = 25;
    private float downX, upX;

    public ActivitySwipeDetector(SwipeInterface activity){
        this.activity = activity;
    }

    public void onRightToLeftSwipe(View v){
        activity.onSwipeRightToLeft(v);
    }

    public void onLeftToRightSwipe(View v){
        activity.onSwipeLeftToRight(v);
    }

    public boolean onTouch(View v, MotionEvent event) {
        switch(event.getAction()){
        case MotionEvent.ACTION_DOWN: {
            downX = event.getX();
            
            return true;
        }
        case MotionEvent.ACTION_UP: {
            upX = event.getX();
            

            float deltaX = downX - upX;
            // swipe horizontal?
            if(Math.abs(deltaX) > MIN_DISTANCE){
                // left or right
                if(deltaX < 0) { this.onLeftToRightSwipe(v); return true; }
                if(deltaX > 0) { this.onRightToLeftSwipe(v); return true; }
            }
            else {
                //Log.i(logTag, "Swipe was only " + Math.abs(deltaX) + " long, need at least " + MIN_DISTANCE);
            }

        }
        }
        return true;
    }

}
