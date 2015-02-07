package com.mapbox.mapboxsdk.android.testapp.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.mapbox.mapboxsdk.android.testapp.R;
import com.mapbox.mapboxsdk.overlay.Marker;
import com.mapbox.mapboxsdk.views.InfoWindow;
import com.mapbox.mapboxsdk.views.MapView;

public class CustomInfoWindow extends InfoWindow {

    protected View createInfoView(final Context context) {
        LayoutInflater inflater =
                (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        return inflater.inflate(R.layout.infowindow_custom, null);
    }
    public CustomInfoWindow(final Context context) {
        super(context);
        
        // Add own OnTouchListener to customize handling InfoWindow touch events
//        setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                if (event.getAction() == MotionEvent.ACTION_UP) {
//                    // Demonstrate custom onTouch() control
//                    Toast.makeText(mView.getContext(), R.string.customInfoWindowOnTouchMessage, Toast.LENGTH_SHORT).show();
//
//                    // Still close the InfoWindow though
//                    close();
//                }
//
//                // Return true as we're done processing this event
//                return true;
//            }
//        });
    }
    @Override
    public void willOpen(Marker marker) {
        String title = marker.getTitle();
        ((TextView) getInfoView().findViewById(R.id.customTooltip_title)).setText(title);
    }
    /**
     * Dynamically set the content in the CustomInfoWindow
     * @param overlayItem The tapped Marker
     */
//    @Override
//    public void onOpen(Marker overlayItem) {
//        String title = overlayItem.getTitle();
//        ((TextView) mView.findViewById(R.id.customTooltip_title)).setText(title);
//    }
}
