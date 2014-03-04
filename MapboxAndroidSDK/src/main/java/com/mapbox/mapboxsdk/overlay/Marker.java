package com.mapbox.mapboxsdk.overlay;

import android.content.Context;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.views.MapView;

/**
 * A Marker object is a visible representation of a point on a Map that has a geographical place.
 */
public class Marker extends ExtendedOverlayItem {
    private Context context;
    private MapView mapView;

    /**
     * Initialize a new marker object, adding it to a MapView and attaching a tooltip
     * @param mv a mapview
     * @param aTitle the title of the marker, in a potential tooltip
     * @param aDescription the description of the marker, in a tooltip
     * @param aLatLng the location of the marker
     */
    public Marker(MapView mv, String aTitle, String aDescription, LatLng aLatLng) {
        super(aTitle, aDescription, aLatLng);
        if (mv != null) {
            context = mv.getContext();
            mapView = mv;
            setIcon(new Icon(Icon.Size.l, "", "000"));
//            attachTooltip();
        }
    }

    /**
     * Initialize a new marker object
     *
     * @param aTitle the title of the marker, in a potential tooltip
     * @param aDescription the description of the marker, in a tooltip
     * @param aLatLng the location of the marker
     */
    public Marker(String aTitle, String aDescription, LatLng aLatLng) {
        this(null, aTitle, aDescription, aLatLng);
    }

    public Marker addTo(MapView mv) {
        mapView = mv;
        context = mv.getContext();
        setIcon(new Icon(Icon.Size.l, "", "000"));
        return this;
    }

    public Marker setIcon(Icon icon) {
        icon.setMarker(this);
        this.setMarkerHotspot(HotspotPlace.CENTER);
        return this;
    }
}
