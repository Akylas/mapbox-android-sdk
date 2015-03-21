package com.mapbox.mapboxsdk.android.testapp;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import com.mapbox.mapboxsdk.api.ILatLng;
import com.mapbox.mapboxsdk.geometry.BoundingBox;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.overlay.Icon;
import com.mapbox.mapboxsdk.overlay.Marker;
import com.mapbox.mapboxsdk.views.MapView;
import com.mapbox.mapboxsdk.views.util.Projection;

public class RotatedMapTestFragment extends Fragment {

    private static final String TAG = "RotatedMapTest";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_rotatedmap, container, false);

        final MapView mv = (MapView) view.findViewById(R.id.rotatedMapView);
        mv.setScrollableAreaLimit(new BoundingBox(new LatLng(45.49311, 9.14612), new LatLng(45.46115, 9.09041)));
        mv.setCenter(new LatLng(45.47820, 9.12400));
        mv.setZoom(14);
        Log.d(TAG, String.format("Is MapRotation Enabled? '%s'", mv.isMapRotationEnabled()));
        mv.setMapOrientation(90.0f);
        Log.d(TAG, String.format("Is MapRotation Enabled Post Set? '%s'", mv.isMapRotationEnabled()));

        Marker cap = new Marker(mv, "San Siro", "Stadio Giuseppe Meazza", new LatLng(45.47820, 9.12400));
        cap.setIcon(new Icon(getActivity(), Icon.Size.LARGE, "soccer", "FF0000"));
        mv.addMarker(cap);
        mv.setOnMapClickListener(new MapView.OnMapClickListener() {
            @Override
            public void onMapClick(ILatLng pPosition) {
                String coords = String.format("Original Lat = %f, Lon = %f", pPosition.getLatitude(), pPosition.getLongitude());
                float[] rc = {(float)pPosition.getLatitude(), (float)pPosition.getLongitude()};
                Projection p = mv.getProjection();
                p.rotatePoints(rc);
                ILatLng rotLatLon = p.fromPixels(rc[0], rc[1]);
                String rotCoords = String.format("Rotated Lat = %f, Lon = %f", rotLatLon.getLatitude(), rotLatLon.getLongitude());
                Log.i("TapForUTFGridTestFragment", String.format("coords = '%s', rotated coords = '%s'", coords, rotCoords));
                Toast.makeText(getActivity(), coords + " ~ " + rotCoords, Toast.LENGTH_LONG).show();
            }
        });

        return view;
    }
}
