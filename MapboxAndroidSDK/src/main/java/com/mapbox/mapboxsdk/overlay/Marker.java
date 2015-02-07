package com.mapbox.mapboxsdk.overlay;

import android.content.Context;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.views.InfoWindow;
import com.mapbox.mapboxsdk.views.MapView;
import com.mapbox.mapboxsdk.views.util.Projection;

/**
 * Immutable class describing a LatLng with a Title and a Description.
 */
public class Marker {
    public static final int ITEM_STATE_FOCUSED_MASK = 4;
    public static final int ITEM_STATE_PRESSED_MASK = 1;
    public static final int ITEM_STATE_SELECTED_MASK = 2;

    private boolean mClustered;

    private final RectF mMyLocationRect = new RectF(0, 0, 0, 0);
    private final RectF mMyLocationPreviousRect = new RectF(0, 0, 0, 0);
    protected final PointF mCurMapCoords = new PointF();

    protected Context context;
    private MapView mapView;
    private Icon icon;
    private Comparable mSortkey = null;
    private boolean isUsingMakiIcon = true;

    protected String mUid;
    protected LatLng mLatLng;
    protected Drawable mMarker;
    protected PointF mAnchor = null;

    private int mIndexForFastSort = 0;
    private String mTitle = "";
    private String mDescription = "";
    private String mSubDescription = "";
    //a third field that can be displayed in the infowindow, on a third line
    private Drawable mImage; //that will be shown in the infowindow.
    //private GeoPoint mGeoPoint //unfortunately, this is not so simple...
    private Object mRelatedObject; //reference to an object (of any kind) linked to this item.
    private boolean bubbleShowing;
    private ItemizedOverlay mParentHolder;

    private boolean mAnimated = true;
    private boolean mDraggable = false;

    private float mMinZoom = 0;
    private float mMaxZoom = 22;

    /**
     * Construct a new Marker, given title, description, and place
     * @param title Marker title
     * @param description Marker description
     * @param latLng Marker position
     */
    public Marker(String title, String description, LatLng latLng) {
        this(null, title, description, latLng);
    }

    /**
     * Initialize a new marker object, adding it to a MapView and attaching a tooltip
     *
     * @param mv a mapview
     * @param aTitle the title of the marker, in a potential tooltip
     * @param aDescription the description of the marker, in a tooltip
     * @param aLatLng the location of the marker
     */
    public Marker(MapView mv, String aTitle, String aDescription, LatLng aLatLng) {
        super();
        this.mapView = mv;
        this.setTitle(aTitle);
        this.setDescription(aDescription);
        this.mLatLng = aLatLng;
        if (mv != null) {
            mAnchor = mv.getDefaultPinAnchor();
        }
        mParentHolder = null;
    }

    /**
     * Attach this marker to a given MapView and that MapView's context
     * @param mv the MapView to add this marker to
     * @return Marker
     */
    public Marker addTo(MapView mv) {
        if (mMarker == null) {
            //if there is an icon it means it's not loaded yet
            //thus change the drawable while waiting
            setMarker(mv.getDefaultPinDrawable());
            isUsingMakiIcon = true;
        }
        mapView = mv;
        context = mv.getContext();
        if (mAnchor == null) {
            mAnchor = mv.getDefaultPinAnchor();
        }
        return this;
    }

    /**
     * Determine if this marker has a title, description, subdescription,
     * or image that could be displayed
     * @return true if the marker has content
     */
    public boolean hasContent() {
        return !TextUtils.isEmpty(this.mTitle) ||
                !TextUtils.isEmpty(this.mDescription) ||
                !TextUtils.isEmpty(this.mSubDescription) ||
                this.mImage != null;
    }

    protected InfoWindow createInfoWindow() {
        return new InfoWindow(context);
    }

    private InfoWindow mInfoWindow;

    /**
     * Get this marker's tooltip, creating it if it doesn't exist yet.
     * @param mv MapView
     * @return InfoWindow
     */
    public InfoWindow getInfoWindow() {
        if (mInfoWindow == null) {
            mInfoWindow = createInfoWindow();
        }
        return mInfoWindow;
    }
    
    public void setInfoWindow(InfoWindow infoWindow) {
        mInfoWindow = infoWindow;
    }

    public void closeInfoWindow() {
        if (mInfoWindow != null && mInfoWindow.equals(mInfoWindow.getMapView().getCurrentInfoWindow())) {
            mInfoWindow.getMapView().closeCurrentInfoWindow();
        }
    }

    public void blur() {
        bubbleShowing = false;
        if (mParentHolder != null) {
            mParentHolder.blurItem(this);
        }
    }

    /**
     * Indicates a hotspot for an area. This is where the origin (0,0)of a point will be located
     * relative to the area. In otherwords this acts as an offset. NONE indicates that no
     * adjustment
     * should be made.
     */
    public enum HotspotPlace {
        NONE, CENTER, BOTTOM_CENTER, TOP_CENTER, RIGHT_CENTER,
        LEFT_CENTER, UPPER_RIGHT_CORNER, LOWER_RIGHT_CORNER,
        UPPER_LEFT_CORNER, LOWER_LEFT_CORNER
    }
    
    public void setIndexForFastSort(final int index) {
        mIndexForFastSort = index;
    }
    
    public int getIndexForFastSort() {
        return mIndexForFastSort;
    }

    public String getUid() {
        return mUid;
    }

    public String getTitle() {
        return mTitle;
    }

    public LatLng getPoint() {
        return mLatLng;
    }

    public void setTitle(String aTitle) {
        mTitle = aTitle;
    }

    public void setDescription(String aDescription) {
        mDescription = aDescription;
    }

    public void setSubDescription(String aSubDescription) {
        mSubDescription = aSubDescription;
    }

    public void setImage(Drawable anImage) {
        mImage = anImage;
    }

    public void setRelatedObject(Object o) {
        mRelatedObject = o;
    }

    /**
     * Set the centerpoint of this marker in geographical coordinates
     * @param point
     */
    public void setPoint(LatLng point) {
        mLatLng = point;
        invalidate();
    }

    /**
     * Set the description attached to this marker
     * @return
     */
    public String getDescription() {
        return mDescription;
    }

    /**
     * Set the sub-description attached to this marker
     * @return
     */
    public String getSubDescription() {
        return mSubDescription;
    }

    /**
     * Set the image attached to this marker
     * @return
     */
    public Drawable getImage() {
        return mImage;
    }

    public Object getRelatedObject() {
        return mRelatedObject;
    }

    public ItemizedOverlay getParentHolder() {
        return mParentHolder;
    }

    public void setParentHolder(ItemizedOverlay o) {
        mParentHolder = o;
    }


    @SuppressWarnings("rawtypes")
    public void setSortkey(Comparable value) {
        mSortkey = value;
    }

    @SuppressWarnings("rawtypes")
    public Comparable getSortkey() {
        return mSortkey;
    }

    /**
     * Gets the custom image (Drawable) used for the Marker's image
     * @param stateBitset State Of Marker (@see #ITEM_STATE_FOCUSED_MASK , @see #ITEM_STATE_PRESSED_MASK, @see #ITEM_STATE_SELECTED_MASK)
     * @return marker drawable corresponding to stateBitset
     */
    public Drawable getMarker(final int stateBitset) {
        // marker not specified
        if (mMarker == null) {
            return null;
        }

        // set marker state appropriately
        setState(mMarker, stateBitset);
        return mMarker;
    }

    /**
     * Set a custom image to be used as the Marker's image
     * @param marker Drawable resource to be used as Marker image
     */
    public void setMarker(final Drawable marker) {
        this.mMarker = marker;
        if (marker != null) {
            marker.setBounds(0, 0, marker.getIntrinsicWidth(), marker.getIntrinsicHeight());
            isUsingMakiIcon = false;
        }
        invalidate();
    }

    /**
     * Sets the marker hotspot
     * @param place Hotspot Location @see #HotspotPlace
     */
    public void setHotspot(HotspotPlace place) {
        if (place == null) {
            place = HotspotPlace.BOTTOM_CENTER; //use same default than in osmdroid.
        }
        switch (place) {
            case NONE:
            case UPPER_LEFT_CORNER:
                mAnchor.set(0, 0);
                break;
            case BOTTOM_CENTER:
                mAnchor.set(0.5f, 1f);
                break;
            case LOWER_LEFT_CORNER:
                mAnchor.set(0, 1);
                break;
            case LOWER_RIGHT_CORNER:
                mAnchor.set(1, 1);
                break;
            case CENTER:
                mAnchor.set(0.5f, 0.5f);
                break;
            case LEFT_CENTER:
                mAnchor.set(0, 0.5f);
                break;
            case RIGHT_CENTER:
                mAnchor.set(1, 0.5f);
                break;
            case TOP_CENTER:
                mAnchor.set(0.5f, 0);
                break;
            case UPPER_RIGHT_CORNER:
                mAnchor.set(1, 0);
                break;
        }
        invalidate();
    }

    public Point getAnchor() {
        if (mAnchor != null) {
            int markerWidth = getWidth(), markerHeight = getRealHeight();
            return new Point((int) (-mAnchor.x * markerWidth), (int) (-mAnchor.y * markerHeight));
        }
        return new Point(0, 0);
    }

//    public Point getAnchor(HotspotPlace place) {
//        int markerWidth = getWidth(), markerHeight = getHeight();
//        return getHotspot(place, markerWidth, markerHeight);
//    }

    public void setAnchor(final PointF anchor) {
        this.mAnchor = anchor;
        invalidate();
    }

    public static void setState(final Drawable drawable, final int stateBitset) {
        final int[] states = new int[3];
        int index = 0;
        if ((stateBitset & ITEM_STATE_PRESSED_MASK) > 0) {
            states[index++] = android.R.attr.state_pressed;
        }
        if ((stateBitset & ITEM_STATE_SELECTED_MASK) > 0) {
            states[index++] = android.R.attr.state_selected;
        }
        if ((stateBitset & ITEM_STATE_FOCUSED_MASK) > 0) {
            states[index++] = android.R.attr.state_focused;
        }

        drawable.setState(states);
    }

    public Drawable getDrawable() {
        return this.mMarker;
    }

    /**
     * Get the width of the marker, based on the width of the image backing it.
     */
    public int getWidth() {
        return this.mMarker.getIntrinsicWidth();
    }

    public int getHeight() {
        int result = getRealHeight();
        if (isUsingMakiIcon) {
            result /= 2;
        }
        return result;
    }
    
    public int getRealHeight() {
        return this.mMarker.getIntrinsicHeight();
    }

    /**
     * Get the current position of the marker in pixels
     * @param projection Projection
     * @param reuse PointF to reuse
     */
    public PointF getPositionOnScreen(final Projection projection, final PointF reuse) {
        return projection.toPixels(mCurMapCoords, reuse);
    }
    
    public PointF getPositionOnScreen(final PointF reuse) {
        if (mapView != null) {
            return getPositionOnScreen(mapView.getProjection(), reuse);
        }
        return null;
    }

    public PointF getDrawingPositionOnScreen(final Projection projection, PointF reuse) {
        reuse = getPositionOnScreen(projection, reuse);
        Point point = getAnchor();
        reuse.offset(point.x, point.y);
        return reuse;
    }
    
    protected RectF getBounds(final Projection projection, final boolean realSize, RectF reuse) {
        if (reuse == null) {
            reuse = new RectF();
        }
        final PointF position = getPositionOnScreen(projection, null);
        final int w = getWidth();
        final int h = realSize?getRealHeight():getHeight();
        final float x = position.x - mAnchor.x * w;
        final float y = position.y - mAnchor.y * h;
        reuse.set(x, y, x + w, y + h);
        return reuse;
    }

    protected RectF getScreenDrawingBounds(final Projection projection, RectF reuse) {
        return getBounds(projection, true, reuse);
    }
    
    protected RectF getHitBounds(final Projection projection, RectF reuse) {
        return getBounds(projection, false, reuse);
    }

    protected RectF internalGetMapDrawingBounds(final Projection projection, RectF reuse) {
        if (reuse == null) {
            reuse = new RectF();
        }
        projection.toMapPixels(mLatLng, mCurMapCoords);
        final int w = getWidth();
        final float x = mCurMapCoords.x - mAnchor.x * w;
        final float y = mCurMapCoords.y - mAnchor.y * getHeight();
        reuse.set(x, y, x + w, y + getRealHeight());
        return reuse;
    }
    
    protected RectF getMapDrawingBounds() {

        return mMyLocationRect;
    }

    public PointF getHotspotScale(HotspotPlace place, PointF reuse) {
        if (reuse == null) {
            reuse = new PointF();
        }
        if (place == null) {
            place = HotspotPlace.BOTTOM_CENTER; //use same default than in osmdroid.
        }
        switch (place) {
            case NONE:
            case UPPER_LEFT_CORNER:
                reuse.set(-0.5f, -0.5f);
                break;
            case BOTTOM_CENTER:
                reuse.set(0, 0.5f);
                break;
            case LOWER_LEFT_CORNER:
                reuse.set(-0.5f, 0.5f);
                break;
            case LOWER_RIGHT_CORNER:
                reuse.set(0.5f, 0.5f);
                break;
            case CENTER:
                reuse.set(0, 0);
                break;
            case LEFT_CENTER:
                reuse.set(-0.5f, 0);
                break;
            case RIGHT_CENTER:
                reuse.set(0.5f, 0);
                break;
            case TOP_CENTER:
                reuse.set(0, -0.5f);
                break;
            case UPPER_RIGHT_CORNER:
                reuse.set(0.5f, -0.5f);
                break;
        }
        return reuse;
    }

    public PointF getMarkerHotspotScale(HotspotPlace place, PointF reuse) {

        if (reuse == null) {
            reuse = new PointF();
        }
        getHotspotScale(place, reuse);
        reuse.offset(0.5f,0.5f);
        return reuse;
    }

    /**
     * From a HotspotPlace and drawable dimensions (width, height), return the hotspot position.
     * Could be a public method of HotspotPlace or OverlayItem...
     */
    public Point getHotspot(HotspotPlace place, int w, int h) {
        PointF scale = getMarkerHotspotScale(place, null);
        return new Point((int) (-w * scale.x), (int) (-h * scale.y));
    }

    /**
     * Populates this infoWindow with all item info:
     * <ul>title and description in any case, </ul>
     * <ul>image and sub-description if any.</ul>
     * and centers the map view on the item if panIntoView is true. <br>
     */
    public void showInfoWindow(InfoWindow infoWindow, MapView aMapView, boolean panIntoView) {
        int markerWidth = getWidth(), markerHeight = getRealHeight();
        PointF infoAnchor = infoWindow.getAnchor();
        if (infoAnchor == null) {
            infoAnchor = aMapView.getDefaultInfoWindowAnchor();
        }
        PointF tooltipH = new PointF((infoAnchor.x + 0.5f - mAnchor.x) * markerWidth, 
                (infoAnchor.y + 0.5f - mAnchor.y) * markerHeight);
        infoWindow.setMapView(aMapView);
        infoWindow.open(this, this.getPoint(), (int) tooltipH.x, (int) tooltipH.y);
        if (panIntoView) {
            aMapView.getController().animateTo(getPoint());
        }

        bubbleShowing = true;
    }

    /**
     * Sets the Icon image that represents this marker on screen.
     */
    public Marker setIcon(Icon aIcon) {
        this.icon = aIcon;
        icon.setMarker(this);
        isUsingMakiIcon = true;
        return this;
    }

    public boolean isUsingMakiIcon() {
        return isUsingMakiIcon;
    }
    public void setUsingMakiIcon(final boolean value) {
        isUsingMakiIcon = value;
    }

    public PointF getPositionOnMap() {
        return mCurMapCoords;
    }
    
    public LatLng getPosition() {
        return this.mLatLng;
    }

    public boolean shouldDraw() {
        if (mapView == null) {
            return false;
        }
        if (bubbleShowing) {
            return true;
        }
        float zoomDelta = (float) (Math.log(mapView.getScale()) / Math.log(2d));
        float zoom = mapView.getZoomLevel(false) + zoomDelta;
        return zoom > mMinZoom && zoom < mMaxZoom;
    }

    public void updateDrawingPosition() {
        if (mapView == null) {
            return; //not on map yet
        }
        internalGetMapDrawingBounds(mapView.getProjection(), mMyLocationRect);
    }

    /**
     * Sets the marker to be redrawn.
     */
    public void invalidate() {
        if (mapView == null) {
            return; //not on map yet
        }
        // Get new drawing bounds
        mMyLocationPreviousRect.set(mMyLocationRect);
        updateDrawingPosition();
        final RectF newRect = new RectF(mMyLocationRect);
        // If we had a previous location, merge in those bounds too
        newRect.union(mMyLocationPreviousRect);
        // Invalidate the bounds
        mapView.post(new Runnable() {
            @Override
            public void run() {
                mapView.invalidateMapCoordinates(newRect);
            }
        });
    }

    public void setMinZoom(final float zoom) {
        mMinZoom = zoom;
        invalidate();
    }

    public void setMaxZoom(final float zoom) {
        mMaxZoom = zoom;
        invalidate();
    }

    public void setAnimated(final boolean animated) {
        mAnimated = animated;
    }
    public boolean isAnimated() {
        return mAnimated;
    }

    public boolean isInfoWindowShown() {
        return bubbleShowing;
    }

    public boolean isDraggable() {
        return !mClustered && mDraggable;
    }
    public void setDraggable(final boolean draggable) {
        mDraggable = draggable;
    }
}
