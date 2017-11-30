package net.kwatts.powtools.components;

import android.view.MotionEvent;

import com.github.mikephil.charting.listener.ChartTouchListener;
import com.github.mikephil.charting.listener.OnChartGestureListener;

public class LockScrollbarGestureListener implements OnChartGestureListener {

    private LockableScrollView scrollView;

    public LockScrollbarGestureListener(LockableScrollView scrollView) {
        this.scrollView = scrollView;
    }
    @Override
    public void onChartGestureStart(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {
        scrollView.setScrollingEnabled(false);
    }

    @Override
    public void onChartGestureEnd(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {
        scrollView.setScrollingEnabled(true);
    }
    @Override
    public void onChartLongPressed(MotionEvent me) {

    }

    @Override
    public void onChartDoubleTapped(MotionEvent me) {

    }

    @Override
    public void onChartSingleTapped(MotionEvent me) {

    }

    @Override
    public void onChartFling(MotionEvent me1, MotionEvent me2, float velocityX, float velocityY) {

    }

    @Override
    public void onChartScale(MotionEvent me, float scaleX, float scaleY) {

    }

    @Override
    public void onChartTranslate(MotionEvent me, float dX, float dY) {

    }
}
