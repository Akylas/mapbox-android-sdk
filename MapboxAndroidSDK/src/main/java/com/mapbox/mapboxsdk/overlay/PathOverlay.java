package com.mapbox.mapboxsdk.overlay;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;

import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.views.MapView;
import com.mapbox.mapboxsdk.views.util.Projection;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Viesturs Zarins
 * @author Martin Pearman
 *         <p/>
 *         This class draws a path line in given color.
 */
public class PathOverlay extends Overlay {

    private static final String TAG = "PathOverlay";
    /**
     * Stores points, converted to the map projection.
     */
    private ArrayList<PointF> mPoints = new ArrayList<PointF>();

    /**
     * Number of points that have precomputed values.
     */
    private int mPointsPrecomputed;


    private boolean mOptimizePath = true;

    /**
     * Paint settings.
     */
    protected Paint mPaint = new Paint();
    private final Path mPath = new Path();

    private final PointF mTempPoint1 = new PointF();
    private final PointF mTempPoint2 = new PointF();

    // bounding rectangle for the current line segment.
    private final RectF mLineBounds = new RectF();

    public PathOverlay() {
        super();
        this.mPaint.setColor(Color.BLUE);
        this.mPaint.setAntiAlias(true);
        this.mPaint.setStrokeWidth(10.0f);
        this.mPaint.setStyle(Paint.Style.STROKE);
        this.clearPath();
        setOverlayIndex(1);
    }

    public PathOverlay(final int color, final float width) {
        super();
        this.mPaint.setColor(color);
        this.mPaint.setStrokeWidth(width);
        this.mPaint.setStyle(Paint.Style.STROKE);

        this.clearPath();
        setOverlayIndex(PATHOVERLAY_INDEX);
    }

    public Paint getPaint() {
        return mPaint;
    }

    public PathOverlay setPaint(final Paint pPaint) {
        mPaint = pPaint;
        return this;
    }

    public void clearPath() {
        synchronized (this.mPoints) {
            this.mPointsPrecomputed = 0;
            this.mPoints = new ArrayList<PointF>();
        }
    }

    public void addPoint(final LatLng aPoint) {
        addPoint(aPoint.getLatitude(), aPoint.getLongitude());
    }

    public void addPoint(final double aLatitude, final double aLongitude) {
        mPoints.add(new PointF((float) aLatitude, (float) aLongitude));
    }

    public void addPoints(final LatLng... aPoints) {
        for (final LatLng point : aPoints) {
            addPoint(point);
        }
    }

    public void addPoints(final List<LatLng> aPoints) {
        for (final LatLng point : aPoints) {
            addPoint(point);
        }
    }

    public void removeAllPoints() {
        mPoints.clear();
    }

    public int getNumberOfPoints() {
        return this.mPoints.size();
    }

    /**
     * This method draws the line. Note - highly optimized to handle long paths, proceed with care.
     * Should be fine up to 10K points.
     */
    @Override
    protected void draw(final Canvas canvas, final MapView mapView) {

        int size = 0;
        synchronized (this.mPoints) {
            size = this.mPoints.size();
        }
        // nothing to paint
        if (mSize < 2) {
            return;
        }

        final Projection pj = mapView.getProjection();
        boolean needsDrawing = false;
        // precompute new points to the intermediate projection.
        synchronized (this.mPoints) {
            for (; this.mPointsPrecomputed < size; this.mPointsPrecomputed++) {
                final PointF pt = this.mPoints.get(this.mPointsPrecomputed);
                pj.toMapPixelsProjected((double) pt.x, (double) pt.y, pt);
            }

            PointF screenPoint0 = null; // points on screen
            PointF screenPoint1;
            PointF projectedPoint0; // points from the points list
            PointF projectedPoint1;

            // clipping rectangle in the intermediate projection, to avoid performing projection.
//            final Rect clipBounds = pj.fromPixelsToProjected(pj.getScreenRect());
            final RectF clipBounds = pj.fromPixelsToProjected(pj.getTransformScreenRect());

            mPath.rewind();
            needsDrawing = !mOptimizePath;
            projectedPoint0 = this.mPoints.get(size - 1);
            mLineBounds.set((int) projectedPoint0.x, (int) projectedPoint0.y, (int) projectedPoint0.x,
                    (int) projectedPoint0.y);

            for (int i = size - 2; i >= 0; i--) {
                // compute next points
                projectedPoint1 = this.mPoints.get(i);

                //mLineBounds needs to be computed
                mLineBounds.union((int) projectedPoint1.x, (int) projectedPoint1.y);

                if (mOptimizePath && !RectF.intersects(clipBounds, mLineBounds)) {
                    // skip this line, move to next point
                    projectedPoint0 = projectedPoint1;
                    mLineBounds.set((int) projectedPoint0.x, (int) projectedPoint0.y, (int) projectedPoint0.x,
                            (int) projectedPoint0.y);
                    screenPoint0 = null;
                    continue;
                }


                // the starting point may be not calculated, because previous segment was out of clip
                // bounds
                if (screenPoint0 == null) {
//                    GeometryUtils.getIntersectionPoint(projectedPoint0, projectedPoint1, clipBounds, this.mTempPoint1);
//                    projectedPoint0 = this.mTempPoint1;
                    screenPoint0 = pj.toMapPixelsTranslated(projectedPoint0, this.mTempPoint1);
                    mPath.moveTo(screenPoint0.x, screenPoint0.y);
                }
                screenPoint1 = pj.toMapPixelsTranslated(projectedPoint1, this.mTempPoint2);


                // skip this point, too close to previous point
                if (Math.abs(screenPoint1.x - screenPoint0.x) + Math.abs(
                        screenPoint1.y - screenPoint0.y) <= 1) {
                    continue;
                }

                mPath.lineTo(screenPoint1.x, screenPoint1.y);
                // update starting point to next position
                projectedPoint0 = projectedPoint1;
                screenPoint0.x = screenPoint1.x;
                screenPoint0.y = screenPoint1.y;
                if (mOptimizePath) {
                    needsDrawing = true;
                    mLineBounds.set((int) projectedPoint0.x, (int) projectedPoint0.y, (int) projectedPoint0.x,
                            (int) projectedPoint0.y);
                }
            }
            if (!mOptimizePath) {
                needsDrawing = RectF.intersects(clipBounds, mLineBounds);
            }
        }

        if (needsDrawing) {
            final float realWidth = this.mPaint.getStrokeWidth();
            this.mPaint.setStrokeWidth(realWidth / mapView.getScale());
            canvas.drawPath(mPath, this.mPaint);
            this.mPaint.setStrokeWidth(realWidth);
        }
    }

    /**
     * if true the path will be optimised. True by default. But be aware that the optimize method
     * does not work for filled path.
     */
    public void setOptimizePath(final boolean value) {
        mOptimizePath = value;
    }
}