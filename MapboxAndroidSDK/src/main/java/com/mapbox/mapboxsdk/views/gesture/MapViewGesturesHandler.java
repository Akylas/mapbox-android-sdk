package com.mapbox.mapboxsdk.views.gesture;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.Scroller;

import com.almeros.android.multitouch.RotateGestureDetector;
import com.almeros.android.multitouch.ShoveGestureDetector;
import com.mapbox.mapboxsdk.api.ILatLng;
import com.mapbox.mapboxsdk.util.constants.UtilConstants;
import com.mapbox.mapboxsdk.views.MapView;

public class MapViewGesturesHandler {

    private static final boolean KITKAT_OR_GREATER = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT);
    
    private final GestureDetector mGestureDetector;
    protected final ScaleGestureDetector mScaleGestureDetector;
    protected final RotateGestureDetector mRotateGestureDetector;
    protected final ShoveGestureDetector mShoveGestureDetector;
    private final MapView mMapView;
    protected final Scroller mScroller;


    private boolean mRotationEnabled = true;
    private boolean mScaleEnabled = true;
    private boolean mShoveEnabled = false;

    protected boolean mIsFlinging = false;
    private boolean mIsScaling = false;
    private boolean mIsRotating = false;
    private boolean mIsShoving = false;

    private boolean mSimultaneousRotationScale = true;

    private final int ACTION_DOWN = 0;
    private final int ACTION_FLING = 1;
    private final int ACTION_LONG_PRESS = 2;
    private final int ACTION_SCROLL = 3;
    private final int ACTION_TAP = 4;
    private final int ACTION_DOUBLE_TAP = 5;
    private final int ACTION_SHOW_PRESS = 6;
    private final int ACTION_SCALE = 7;
    private final int ACTION_ROTATE = 8;
    private final int ACTION_SHOVE = 9;

    private boolean isAnimating() {
        return this.mMapView.isAnimating();
    }

    private boolean shouldIgnoreAction(final int action) {
        return shouldIgnoreAction(action, true);
    }

    private boolean shouldIgnoreAction(final int action, final boolean checkAnimated) {
        if (checkAnimated && isAnimating()) return true;
        switch (action) {
            case ACTION_DOWN:
            {
                return false;
            }
            case ACTION_FLING:
            case ACTION_LONG_PRESS:
            case ACTION_SCROLL:
            case ACTION_TAP:
            case ACTION_DOUBLE_TAP:
            case ACTION_SHOW_PRESS:
            {
                return mIsScaling || mIsRotating || mIsShoving;
            }
            case ACTION_SCALE:
            {
                return mIsShoving || (!mSimultaneousRotationScale && mIsRotating);
            }
            case ACTION_ROTATE:
            {
                return mIsShoving || (!mSimultaneousRotationScale && mIsScaling);
            }
            case ACTION_SHOVE:
            {
                return mIsScaling || mIsRotating;
            }
            default:
                return false;
        }
    }


    public MapViewGesturesHandler(final Context context, final MapView mapView, final Scroller scroller) {
        this.mMapView = mapView;
        this.mScroller = scroller;
        this.mGestureDetector = new GestureDetector(context,
                new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onDown(final MotionEvent e) {
                        if (shouldIgnoreAction(ACTION_DOWN)) {
                            return true;
                        }
                        // Stop scrolling if we are in the middle of a fling!
                        if (mIsFlinging) {
                            mScroller.abortAnimation();
                            mIsFlinging = false;
                        }
                        mapView.getOverlayManager().onDown(e, mapView);
                        return true;
                    }

                    @Override
                    public boolean onFling(final MotionEvent e1, final MotionEvent e2, final float velocityX,
                                           final float velocityY) {
                        if (shouldIgnoreAction(ACTION_FLING) ||  mapView.getOverlayManager()
                                .onFling(e1, e2, velocityX, velocityY, mapView)) {
                            return true;
                        }

                        final int worldSize = mapView.getProjection().mapSize(mapView.getZoomLevel(false));
                        mIsFlinging = true;
                        mScroller.fling(mapView.getScrollX(), mapView.getScrollY(),
                                (int) -velocityX, (int) -velocityY, -worldSize, worldSize, -worldSize, worldSize);
                        return true;
                    }

                    @Override
                    public void onLongPress(final MotionEvent e) {
                        if (shouldIgnoreAction(ACTION_LONG_PRESS)) {
                            return;
                        }
                        if (mapView.getOverlayManager().onLongPress(e, mapView)) {
                            return;
                        }

                        //in debug we zoom out on longpress (emulator)
                        if (UtilConstants.DEBUGMODE) {
                            final ILatLng center = mapView.getProjection().fromPixels(e.getX(), e.getY());
                            mapView.zoomOutFixing(e.getX(), e.getY(), false);
                        }
                    }

                    @Override
                    public boolean onScroll(final MotionEvent e1, final MotionEvent e2, final float distanceX,
                                            final float distanceY) {
                        if (shouldIgnoreAction(ACTION_SCROLL) || mapView.getOverlayManager()
                                .onScroll(e1, e2, distanceX, distanceY, mapView)) {
                            return true;
                        }
                        mapView.getController().panBy((int) distanceX, (int) distanceY, true);
                        return true;
                    }

                    @Override
                    public void onShowPress(final MotionEvent e) {
                        if (shouldIgnoreAction(ACTION_SHOW_PRESS)) {
                            return;
                        }
                        mapView.getOverlayManager().onShowPress(e, mapView);
                    }

                    @Override
                    public boolean onSingleTapUp(final MotionEvent e) {
                        return false;
                    }

                    @Override
                    public boolean onSingleTapConfirmed(final MotionEvent e) {
                        if (shouldIgnoreAction(ACTION_TAP)) {
                            return true;
                        }
                        return mapView.getOverlayManager().onSingleTapConfirmed(e, mapView);
                    }

                    @Override
                    public boolean onDoubleTap(final MotionEvent e) {
                        if (shouldIgnoreAction(ACTION_DOUBLE_TAP)) {
                            return true;
                        }
                        if (mapView.getOverlayManager().onDoubleTap(e, mapView)) {
                            return true;
                        }
                        return mapView.zoomInFixing(e.getX(), e.getY(), false);
                    }
                });

        this.mScaleGestureDetector =
                new ScaleGestureDetector(context, new ScaleGestureDetector.OnScaleGestureListener() {
                    private float lastFocusX;
                    private float lastFocusY;

                    private float firstFocusX;
                    private float firstFocusY;
                    private float firstSpan;
                    private float currentScale;
                    @Override
                    public boolean onScaleBegin(ScaleGestureDetector detector) {
                        firstFocusX = lastFocusX = detector.getFocusX();
                        firstFocusY = lastFocusY = detector.getFocusY();
                        firstSpan = detector.getCurrentSpan();
                        currentScale = 1.0f;

                        if (shouldIgnoreAction(ACTION_SCALE)) {
                            return true;
                        }

                        return true;
                    }

                    @Override
                    public boolean onScale(ScaleGestureDetector detector) {
                        if (shouldIgnoreAction(ACTION_SCALE, false)) {
                            return mIsScaling;
                        }
                        float delta = detector.getCurrentSpan() - firstSpan;
                        if (!mIsScaling && Math.abs(delta) > 0.3f) {
                            mapView.getController().aboutToStartAnimation(firstFocusX, firstFocusY);
                            mIsScaling = true;
                        }
                        currentScale = detector.getCurrentSpan() / firstSpan;
                        float focusX = detector.getFocusX();
                        float focusY = detector.getFocusY();
                        if (mIsScaling) {
                            currentScale = detector.getCurrentSpan() / firstSpan;
                            mapView.setScale(currentScale);
                            mapView.getController()
                                    .offsetDeltaScroll(lastFocusX - focusX, lastFocusY - focusY);
                            mapView.getController()
                                    .panBy(lastFocusX - focusX, lastFocusY - focusY, true);
                        }


                        lastFocusX = focusX;
                        lastFocusY = focusY;
                        return true;
                    }

                    @Override
                    public void onScaleEnd(ScaleGestureDetector detector) {
                        if (!mIsScaling) {
                            return;
                        }
                        if (shouldIgnoreAction(ACTION_SCALE, false)) {
                            mIsScaling = false;
                            return;
                        }

                        // android scale gesture is recognized a lot too easily which makes
                        // double taps recognized as scales. So here we check if the scale is too small.
                        // if is we ignore it
                        final float preZoom = mapView.getZoomLevel(false);
                        final float newZoom = (float) (Math.log(currentScale) / Math.log(2d) + preZoom);
                        if (Math.abs(newZoom - preZoom) < 0.1f) {
                            mIsScaling = false;
                            mapView.hasStoppedAnimating();
                            return;
                        }
                        //delaying the "end" will prevent some crazy scroll events when finishing
                        //scaling by getting 2 fingers very close to each other
                        final Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                mapView.getController().onAnimationEnd(newZoom);
                                mIsScaling = false;
                            }
                        }, 100);

                    }
                });
        if (KITKAT_OR_GREATER) {
            this.mScaleGestureDetector.setQuickScaleEnabled(false);
        }
        this.mRotateGestureDetector =
                new RotateGestureDetector(context, new RotateGestureDetector.OnRotateGestureListener() {
                    private float firstAngle; //starting angle
                    private float currentDelta;
                    @Override
                    public boolean onRotate(RotateGestureDetector detector) {
                        float delta = detector.getRotationDegreesDelta();
                        currentDelta += delta;
                        if (shouldIgnoreAction(ACTION_ROTATE, false)) {
                            return true;
                        }

                        if (!mIsRotating && Math.abs(currentDelta) > (mIsScaling?40:15)) {
                            mIsRotating = true;
                        }
                        if (mIsRotating) {
                            float newAngle = firstAngle - currentDelta;
                            mapView.setMapOrientation(newAngle);
                        }

                        return true;
                    }

                    @Override
                    public boolean onRotateBegin(RotateGestureDetector detector) {
                        if (shouldIgnoreAction(ACTION_ROTATE)) {
                            return true;
                        }
                        firstAngle = mapView.getMapOrientation();
                        currentDelta = 0;
                        return true;
                    }

                    @Override
                    public void onRotateEnd(RotateGestureDetector detector) {
                        mIsRotating = false;
                    }
                });
        this.mShoveGestureDetector = new ShoveGestureDetector(context, new ShoveGestureDetector.OnShoveGestureListener() {
            private float currentDelta; //starting delta

            @Override
            public boolean onShove(ShoveGestureDetector detector) {

                float delta = detector.getShovePixelsDelta();
                if (shouldIgnoreAction(ACTION_SHOVE, false)) {
                    return true;
                }

                currentDelta += -delta/4;
                if (!mIsShoving && Math.abs(currentDelta) > 3) {
                    currentDelta += mapView.getMapSkew();
                    mIsShoving = true;
                }
                if (mIsShoving) {
//                    float newAngle = firstAngle - currentDelta;

//                    float realValue = Math.min(30, Math.max(0, currentDelta));
//                    mapView.setMapSkew(realValue);
                }

                return true;
            }

            @Override
            public boolean onShoveBegin(ShoveGestureDetector detector) {
                if (shouldIgnoreAction(ACTION_SHOVE)) {
                    return mIsShoving;
                }
                currentDelta = 0;
                return true;
            }

            @Override
            public void onShoveEnd(ShoveGestureDetector detector) {
                mIsShoving = false;
            }
        });
    }

    public boolean onTouch(final MotionEvent event, final MotionEvent rotatedEvent) {
        mGestureDetector.onTouchEvent(rotatedEvent);
        if (mRotationEnabled) {
            // can't use the scale detector's onTouchEvent() result as it always
            // returns true (Android issue #42591
            if (rotatedEvent.getPointerCount() > 1) {
                mRotateGestureDetector.onTouchEvent(event);
            }
        }
        if (mScaleEnabled) {
            mScaleGestureDetector.onTouchEvent(rotatedEvent);
        }
        if (mShoveEnabled) {
            mShoveGestureDetector.onTouchEvent(rotatedEvent);
        }
        canTapTwoFingers = canTapTwoFingers & !isInteracting();
        handleTwoFingersTap(rotatedEvent);
        return true;
    }


    public boolean isScaling() {return mIsScaling;}
    public boolean isRotating() {return mIsRotating;}
    public boolean isFlinging() {return mIsFlinging;}
    public boolean isShoving() {return mIsShoving;}
    public boolean isInteracting() {return mIsScaling || mIsRotating || mIsFlinging || mIsShoving;}

    public boolean isScaleEnabled() {return mScaleEnabled;}
    public boolean isRotationEnabled() {return mRotationEnabled;}
    public boolean isShoveEnabled() {return mShoveEnabled;}

    public void setRotationEnabled(final boolean enabled) {
        mRotationEnabled = enabled;
    }
    public void setScaleEnabled(final boolean enabled) {
        mScaleEnabled = enabled;
    }
    public void setShoveEnabled(final boolean enabled) {
        mShoveEnabled = enabled;
    }


    public void flingeHasStopped() {
        mIsFlinging = false;
    }


    public void flingeHasStarted() {
        mIsFlinging = true;
    }


    private boolean canTapTwoFingers = false;
    private int multiTouchDownCount = 0;

    private boolean handleTwoFingersTap(MotionEvent event) {
        int pointerCount = event.getPointerCount();
        for (int i = 0; i < pointerCount; i++) {
            int action = event.getActionMasked();
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    multiTouchDownCount = 0;
                    break;
                case MotionEvent.ACTION_UP:
                    if (!isAnimating() && canTapTwoFingers) {
                        mMapView.zoomOutFixing(event.getX(), event.getY(), false);
                        canTapTwoFingers = false;
                        multiTouchDownCount = 0;
                        return true;
                    }
                    canTapTwoFingers = false;
                    multiTouchDownCount = 0;
                    break;
                case MotionEvent.ACTION_POINTER_DOWN:
                    multiTouchDownCount++;
                    canTapTwoFingers = multiTouchDownCount > 1;
                    break;
                case MotionEvent.ACTION_POINTER_UP:
                    multiTouchDownCount--;
                    break;
                default:
            }
        }
        return false;
    }

}
