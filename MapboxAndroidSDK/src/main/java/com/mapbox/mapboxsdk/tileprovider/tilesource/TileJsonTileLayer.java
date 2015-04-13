package com.mapbox.mapboxsdk.tileprovider.tilesource;

import android.util.Log;

import com.mapbox.mapboxsdk.geometry.BoundingBox;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.util.NetworkUtils;
import com.mapbox.mapboxsdk.util.constants.UtilConstants;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * A type of tile layer that loads tiles from the internet and metadata about itself
 * with the <a href='https://github.com/mapbox/tilejson-spec'>TileJSON</a> standard.
 */
public class TileJsonTileLayer extends WebSourceTileLayer {

    private static final String TAG = "TileJsonTileLayer";

    private JSONObject tileJSON;

    public TileJsonTileLayer(final String pId, final String url, final boolean enableSSL) {
        super(pId, url, enableSSL);
    }
    
    public TileJsonTileLayer(final String pId, final String url, final boolean enableSSL, final boolean shouldInit) {
        super(pId, url, enableSSL, shouldInit);
    }
    
    protected void initialize(String pId, String aUrl, boolean enableSSL) {
        super.initialize(pId, aUrl, enableSSL);
        String jsonURL = this.getBrandedJSONURL();
        if (jsonURL != null) {
            NetworkUtils.getOkHttpClient()
                    .newCall(NetworkUtils.getHttpRequest(jsonURL))
                    .enqueue(new Callback() {
                        @Override
                        public void onFailure(Request request, IOException e) {
                            e.printStackTrace();
                        }

                        @Override
                        public void onResponse(Response response)
                                throws IOException {
                            if (!response.isSuccessful())
                                throw new IOException("Unexpected code "
                                        + response);
                            try {
                                initWithTileJSON(new JSONObject(response.body()
                                        .string()));
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }

                    });
        }
    }

    private void initWithTileJSON(JSONObject aTileJSON) {
        this.setTileJSON((aTileJSON != null) ? aTileJSON : new JSONObject());
        if (aTileJSON != null) {
            if (this.tileJSON.has("tiles")) {
                try {
                    setURL(this.tileJSON.getJSONArray("tiles").getString(0).replace(".png", "{2x}.png"));
                } catch (JSONException e) {
                    Log.e(TAG, "Couldn't set tile url", e);
                }
            }
            mMinimumZoomLevel = getJSONFloat(this.tileJSON, "minzoom");
            mMaximumZoomLevel = getJSONFloat(this.tileJSON, "maxzoom");
            mName = this.tileJSON.optString("name");
            mDescription = this.tileJSON.optString("description");
            mAttribution = this.tileJSON.optString("attribution");
            mLegend = this.tileJSON.optString("legend");

            double[] center = getJSONDoubleArray(this.tileJSON, "center", 3);
            if (center != null) {
                mCenter = new LatLng(center[0], center[1], center[2]);
            }
            double[] bounds = getJSONDoubleArray(this.tileJSON, "bounds", 4);
            if (bounds != null) {
                mBoundingBox = new BoundingBox(bounds[3], bounds[2], bounds[1], bounds[0]);
            }
        }
        if (UtilConstants.DEBUGMODE) {
            Log.d(TAG, "TileJSON " + this.tileJSON.toString());
        }
    }

    public JSONObject getTileJSON() {
        return tileJSON;
    }

    public void setTileJSON(JSONObject aTileJSON) {
        this.tileJSON = aTileJSON;
    }

    private float getJSONFloat(JSONObject JSON, String key) {
        float defaultValue = 0;
        if (JSON.has(key)) {
            try {
                return (float) JSON.getDouble(key);
            } catch (JSONException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private double[] getJSONDoubleArray(JSONObject JSON, String key, int length) {
        double[] defaultValue = null;
        if (JSON.has(key)) {
            try {
                boolean valid = false;
                double[] result = new double[length];
                Object value = JSON.get(key);
                if (value instanceof JSONArray) {
                    JSONArray array = ((JSONArray) value);
                    if (array.length() == length) {
                        for (int i = 0; i < array.length(); i++) {
                            result[i] = array.getDouble(i);
                        }
                        valid = true;
                    }
                } else {
                    String[] array = JSON.getString(key).split(",");
                    if (array.length == length) {
                        for (int i = 0; i < array.length; i++) {
                            result[i] = Double.parseDouble(array[i]);
                        }
                        valid = true;
                    }
                }
                if (valid) {
                    return result;
                }
            } catch (JSONException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    byte[] readFully(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        for (int count; (count = in.read(buffer)) != -1;) {
            out.write(buffer, 0, count);
        }
        return out.toByteArray();
    }

    protected String getBrandedJSONURL() {
        return null;
    }

}
