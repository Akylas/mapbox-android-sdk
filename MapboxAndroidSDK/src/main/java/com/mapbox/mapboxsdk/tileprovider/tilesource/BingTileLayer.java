package com.mapbox.mapboxsdk.tileprovider.tilesource;

import com.mapbox.mapboxsdk.tileprovider.MapTile;
import com.mapbox.mapboxsdk.util.NetworkUtils;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.Reader;

public class BingTileLayer extends WebSourceTileLayer {

    public static String TAG = "BingTileLayer";

    public static final String IMAGERYSET_AERIAL = "Aerial";
    public static final String IMAGERYSET_AERIALWITHLABELS = "AerialWithLabels";
    public static final String IMAGERYSET_ROAD = "Road";

    private static final String BASE_URL_PATTERN = "http://dev.virtualearth.net/REST/V1/Imagery/Metadata/%s?mapVersion=v1&output=json&key=%s";

    private String mBingMapKey = "";

    private String mStyle = IMAGERYSET_ROAD;

    private boolean mHasMetadata = false;

    public BingTileLayer(String key) {
        super("Bing Tile Layer", BASE_URL_PATTERN, false);

        setBingMapKey(key);

        // Default Bing Maps zoom levels.
        this.setMinimumZoomLevel(1);
        this.setMaximumZoomLevel(22);

        getMetadata();
    }

    @Override
    public String getTileURL(final MapTile aTile, boolean hdpi) {
        if (!mHasMetadata) {
            getMetadata();
        }

        return mUrl.replace("{quadkey}", quadTree(aTile));
    }

    @Override
    public String getCacheKey() {
        return "Bing " + getStyle();
    }

    public String getBingMapKey() {
        return mBingMapKey;
    }

    private void setBingMapKey(String key) {
        mBingMapKey = key;
    }

    public String getStyle() {
        return mStyle;
    }

    public TileLayer setStyle(String style) {
        if (!style.equals(mStyle)) {
            synchronized (mStyle) {
                mStyle = style;
                mHasMetadata = false;
            }
        }
        mStyle = style;
        return this;
    }

    private void getMetadata() {
        synchronized (this) {
            if (mHasMetadata) {
                return;
            }
            String url = String.format(BASE_URL_PATTERN, mStyle, mBingMapKey);
            NetworkUtils.getOkHttpClient()
                    .newCall(NetworkUtils.getHttpRequest(url))
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
                                String metadataUrl = getInstanceFromJSON(
                                        response.body().string()).replace(
                                        "{culture}", "en");

                                mUrl = metadataUrl;
                                mHasMetadata = Boolean.TRUE;
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                    });
        }
    }

    private String getInstanceFromJSON(final String jsonContent) throws Exception {
        if (jsonContent == null) {
            throw new Exception("JSON to parse is null");
        }

        final JSONObject json = new JSONObject(jsonContent);
        final int statusCode = json.getInt("statusCode");
        if (statusCode != 200) {
            throw new Exception("Status code = " + statusCode);
        }

        if ("ValidCredentials".compareToIgnoreCase(json.getString("authenticationResultCode")) != 0) {
            throw new Exception("authentication result code = " + json.getString("authenticationResultCode"));
        }

        final JSONArray resultsSet = json.getJSONArray("resourceSets");
        if (resultsSet == null || resultsSet.length() < 1) {
            throw new Exception("No results set found in json response");
        }

        if (resultsSet.getJSONObject(0).getInt("estimatedTotal") <= 0) {
            throw new Exception("No resource found in json response");
        }

        final JSONObject resource = resultsSet.getJSONObject(0).getJSONArray("resources").getJSONObject(0);

        if (resource.has("ZoomMin")) {
            super.mMinimumZoomLevel = (float) resource.getInt("ZoomMin");
        }
        if (resource.has("ZoomMax")) {
            super.mMaximumZoomLevel = (float) resource.getInt("ZoomMax");
        }

        String imageBaseUrl = resource.getString("imageUrl");
        String imageUrl = imageBaseUrl.replace("{subdomain}", resource.getJSONArray("imageUrlSubdomains").getString(0));

        return imageUrl;
    }

    private static String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }

    private String quadTree(final MapTile tile) {
        final StringBuilder quadKey = new StringBuilder();
        for (int i = tile.getZ(); i > 0; i--) {
            int digit = 0;
            final int mask = 1 << (i - 1);
            if ((tile.getX() & mask) != 0) {
                digit += 1;
            }
            if ((tile.getY() & mask) != 0) {
                digit += 2;
            }
            quadKey.append("" + digit);
        }
        return quadKey.toString();
    }
}
