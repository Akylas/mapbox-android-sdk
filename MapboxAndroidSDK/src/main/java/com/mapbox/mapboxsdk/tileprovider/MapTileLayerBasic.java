package com.mapbox.mapboxsdk.tileprovider;

import android.content.Context;
import com.mapbox.mapboxsdk.tileprovider.modules.MapTileDownloader;
import com.mapbox.mapboxsdk.tileprovider.modules.MapTileModuleLayerBase;
import com.mapbox.mapboxsdk.tileprovider.tilesource.ITileLayer;
import com.mapbox.mapboxsdk.tileprovider.util.SimpleRegisterReceiver;
import com.mapbox.mapboxsdk.views.MapView;

/**
 * A base class for tile layers to built, this is a simple collection of tile sources.
 */
public class MapTileLayerBasic extends MapTileLayerArray implements IMapTileProviderCallback {
    Context mContext;

    /**
     * @param pContext
     * @param pTileSource
     * @param mapView
     */
    public MapTileLayerBasic(final Context pContext, final ITileLayer pTileSource,
            MapView mapView) {
        super(pContext, pTileSource, new SimpleRegisterReceiver(pContext), mapView);
        this.mContext = pContext;
        this.mMapView = mapView;

        final MapTileDownloader downloaderProvider =
                new MapTileDownloader(pTileSource, mTileCache, mNetworkAvailabilityCheck, mMapView);

        for (MapTileModuleLayerBase provider : mTileProviderList) {
            if (provider.getClass().isInstance(MapTileDownloader.class)) {
                mTileProviderList.remove(provider);
            }
        }
        addTileSource(pTileSource);
    }

    @Override
    protected void handleAddTileSource(final ITileLayer pTileSource, final int index) {
        final MapTileDownloader downloaderProvider =
                new MapTileDownloader(pTileSource, mTileCache, mNetworkAvailabilityCheck, mMapView);
        if (hasNoSource()) {
            mCacheKey = pTileSource.getCacheKey();
        }
        synchronized (mTileProviderList) {
            if (index < 0 || index > mTileProviderList.size()) {
                mTileProviderList.add(downloaderProvider);
            } else {
                mTileProviderList.add(index, downloaderProvider);
            }
        }
    }


}