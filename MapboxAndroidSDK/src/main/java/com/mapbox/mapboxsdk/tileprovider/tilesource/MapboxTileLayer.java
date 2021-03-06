package com.mapbox.mapboxsdk.tileprovider.tilesource;

import android.text.TextUtils;
import com.mapbox.mapboxsdk.constants.MapboxConstants;
import com.mapbox.mapboxsdk.tileprovider.MapTile;
import com.mapbox.mapboxsdk.util.MapboxUtils;
import com.mapbox.mapboxsdk.views.util.constants.MapViewConstants;
import java.util.Locale;

/**
 * A convenience class to initialize tile layers that use Mapbox services.
 * Underneath, this initializes a WebSourceTileLayer, but provides conveniences
 * for retina tiles, initialization by ID, and loading over SSL.
 */
public class MapboxTileLayer extends TileJsonTileLayer implements MapViewConstants, MapboxConstants {
    private static final String TAG = "MapboxTileLayer";
    private final String mId;
    private final String mToken;

    /**
     * Initialize a new tile layer, directed at a hosted Mapbox tilesource.
     *
     * @param mapId a valid mapid, of the form account.map
     */
    public MapboxTileLayer(String mapId) {
        this(mapId, true);
    }

    public MapboxTileLayer(String mapId, boolean enableSSL) {
        this(mapId, MapboxUtils.getAccessToken(), enableSSL);
    }
    
    public MapboxTileLayer(String mapId, String token, boolean enableSSL) {
        super(mapId, mapId, enableSSL, false);
        mId = mapId;
        mToken = token;
        initialize(mId, mId, enableSSL);
    }

    @Override
    public TileLayer setURL(final String aUrl) {
        if (!TextUtils.isEmpty(aUrl) && !aUrl.toLowerCase(Locale.US).contains("http://") && !aUrl.toLowerCase(Locale.US).contains("https://")) {
            super.setURL(MAPBOX_BASE_URL_V4 + aUrl + "/{z}/{x}/{y}{2x}.png?access_token=" + mToken);
        } else {
            super.setURL(aUrl);
        }
        return this;
    }

    @Override
    protected String getBrandedJSONURL() {
        String url = String.format(MAPBOX_BRANDED_JSON_URL_V4, mId, mToken);
        if (!mEnableSSL) {
            url = url.replace("https://", "http://");
            url = url.replace("&secure=1", "");
        }

        return url;
    }

    @Override
    public String getTileURL(final MapTile aTile, boolean hdpi) {
        //disable hdpi for mapbox
        return super.getTileURL(aTile, false);
    }

    public String getCacheKey() {
        return mId;
    }
}
