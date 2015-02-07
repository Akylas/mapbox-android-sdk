package com.mapbox.mapboxsdk.android.testapp;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import com.mapbox.mapboxsdk.api.ILatLng;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.util.MapboxUtils;
import com.mapbox.mapboxsdk.views.MapView;

public class TapForUTFGridTestFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tapforutfgrid, container, false);

        final MapView mv = (MapView) view.findViewById(R.id.tapForUTFGridMapView);
        mv.setCenter(new LatLng(-33.99541, 18.48885));
        mv.setZoom(12);
        mv.setOnMapClickListener(new MapView.OnMapClickListener() {
            @Override
            public void onMapClick(ILatLng point) {
                String coords = String.format("Zoom = %f, Lat = %f, Lon = %f", mv.getZoomLevel(), point.getLatitude(), point.getLongitude());
                String utfGrid = MapboxUtils.getUTFGridString(point, Float.valueOf(mv.getZoomLevel()).intValue());
                Log.i("TapForUTFGridTestFragment", String.format("coords = '%s', UTFGrid = '%s'", coords, utfGrid));
                Toast.makeText(getActivity(), coords + " == " + utfGrid, Toast.LENGTH_LONG).show();
            }
        });



        return view;
    }

}
