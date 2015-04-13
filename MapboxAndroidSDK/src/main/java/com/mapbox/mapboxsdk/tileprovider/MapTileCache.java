// Created by plusminus on 17:58:57 - 25.09.2008
package com.mapbox.mapboxsdk.tileprovider;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.util.Log;

import com.mapbox.mapboxsdk.tileprovider.constants.TileLayerConstants;
import com.mapbox.mapboxsdk.util.BitmapUtils;
import com.mapbox.mapboxsdk.util.NetworkUtils;

import java.io.File;
import java.io.InputStream;

import uk.co.senab.bitmapcache.BitmapLruCache;
import uk.co.senab.bitmapcache.BitmapLruCache.OnDiskCacheSetListener;
import uk.co.senab.bitmapcache.CacheableBitmapDrawable;

/**
 * A wrapper around a BitmapLruCache that stores tiles on disk in order
 * to improve performance and provide offline content.
 */
public class MapTileCache implements TileLayerConstants {

    protected static BitmapLruCache sCachedTiles = null;
    private Context context;
    static final String TAG = "MapTileCache";

    private int mMaximumCacheSize;
    private OnDiskCacheSetListener mDiskCacheListener;

    private boolean mDiskCacheEnabled = true;

    public MapTileCache(final Context aContext, OnDiskCacheSetListener diskCacheListener) {
        this(aContext, CACHE_MAPTILEDISKSIZE_DEFAULT, diskCacheListener);
    }

    public MapTileCache(final Context aContext, int aMaximumCacheSize, OnDiskCacheSetListener diskCacheListener) {
        this.context = aContext;
        this.mDiskCacheListener = diskCacheListener;
        this.mMaximumCacheSize = aMaximumCacheSize;
    }

    /**
     * Get the BitmapLruCache that belongs to this tile cache, creating it first
     * if there isn't one yet.
     *
     * @return BitmapLruCache the cache
     */
    protected BitmapLruCache getCache() {
        if (sCachedTiles == null) {
            File cacheDir = NetworkUtils.getDiskCacheDir(context, NetworkUtils.DISK_TILES_CACHE_SUBDIR);
            if (!cacheDir.exists()) {
                if (cacheDir.mkdirs()) {
                    Log.i(TAG, "created cacheDir " + cacheDir.getAbsolutePath());
                } else {
                    Log.e(TAG, "can't create cacheDir " + cacheDir);
                }
            } else {
                Log.i(TAG, "cacheDir previously created '" + cacheDir.getAbsolutePath() + "'");
            }
            sCachedTiles = (new BitmapLruCache.Builder(context)).setMemoryCacheEnabled(true)
                    .setMemoryCacheMaxSize(BitmapUtils.calculateMemoryCacheSize(context))
                    .setDiskCacheEnabled(mDiskCacheEnabled)
                    .setDiskCacheMaxSize(mMaximumCacheSize)
                    .setDiskCacheLocation(cacheDir)
                    .build();
            sCachedTiles.setDiskCacheListener(mDiskCacheListener);
            Log.i(TAG, "Disk Cache Enabled: '" + sCachedTiles.isDiskCacheEnabled() + "'; Memory Cache Enabled: '" + sCachedTiles.isMemoryCacheEnabled() + "'");
        }
        return sCachedTiles;
    }

    /**
     * Computes a prefixed key for a tile.
     *
     * @param aTile the tile
     * @return the key
     */
    public String getCacheKey(MapTile aTile) {
        return aTile.getCacheKey();
    }

    public CacheableBitmapDrawable getMapTile(final MapTile aTile) {
        String key = getCacheKey(aTile);
        CacheableBitmapDrawable result = getCache().getFromMemoryCache(key);
        if (result == null) {
            result = getCache().getFromDiskCache(key, null);
        }
        return result;
    }

    public CacheableBitmapDrawable getMapTileFromMemory(final MapTile aTile) {
        return getCache().getFromMemoryCache(getCacheKey(aTile));
    }

    public CacheableBitmapDrawable getMapTileFromDisk(final MapTile aTile) {
        return getCache().getFromDiskCache(getCacheKey(aTile), null);
    }

    public CacheableBitmapDrawable putTileStream(final MapTile aTile, final InputStream inputStream,
                                                 final BitmapFactory.Options decodeOpts) {
        return getCache().put(getCacheKey(aTile), inputStream, decodeOpts);
    }

    public CacheableBitmapDrawable putTileBitmap(final MapTile aTile, final Bitmap bitmap) {
        return getCache().put(getCacheKey(aTile), bitmap);
    }

    public CacheableBitmapDrawable putTile(final MapTile aTile, final Drawable aDrawable) {
        if (aDrawable != null && aDrawable instanceof BitmapDrawable) {
            String key = getCacheKey(aTile);
            CacheableBitmapDrawable drawable = null;
            if (!getCache().containsInMemoryCache(key)) {
                drawable = getCache().putInMemoryCache(getCacheKey(aTile),
                        ((BitmapDrawable) aDrawable).getBitmap());
            }
            if (getCache().isDiskCacheEnabled() && !getCache().containsInDiskCache(key)) {
                if (drawable != null) {
                    getCache().putInDiskCache(getCacheKey(aTile), drawable);
                } else {
                    getCache().putInDiskCache(getCacheKey(aTile),
                            ((BitmapDrawable) aDrawable).getBitmap());
                }
            }
            return drawable;
        }
        return null;
    }

    public CacheableBitmapDrawable putTileInMemoryCache(final MapTile aTile, final Bitmap aBitmap) {
        if (aBitmap != null) {
            return getCache().putInMemoryCache(getCacheKey(aTile), aBitmap);
        }
        return null;
    }

    public CacheableBitmapDrawable putTileInMemoryCache(final MapTile aTile,
                                                        final Drawable aDrawable) {
        if (aDrawable != null && aDrawable instanceof BitmapDrawable) {
            String key = getCacheKey(aTile);
            if (aDrawable instanceof CacheableBitmapDrawable) {
                return getCache().putInMemoryCache(key, ((CacheableBitmapDrawable) aDrawable));
            } else {
                return getCache().putInMemoryCache(key, ((BitmapDrawable) aDrawable).getBitmap());
            }
        }
        return null;
    }

    public CacheableBitmapDrawable putTileInDiskCache(final MapTile aTile,
                                                      final Drawable aDrawable) {
        if (aDrawable != null && aDrawable instanceof BitmapDrawable) {
            String key = getCacheKey(aTile);
            if (getCache().isDiskCacheEnabled() && !getCache().containsInDiskCache(key)) {
                return getCache().putInDiskCache(getCacheKey(aTile),
                        ((BitmapDrawable) aDrawable).getBitmap());
            }
        }
        return null;
    }

    public boolean containsTile(final MapTile aTile) {
        return getCache().contains(getCacheKey(aTile));
    }

    public boolean containsTileInDiskCache(final MapTile aTile) {
        return getCache().isDiskCacheEnabled() && getCache().containsInDiskCache(getCacheKey(aTile));
    }

    public void removeTile(final MapTile aTile) {
        getCache().remove(getCacheKey(aTile));
    }

    public void removeTileFromMemory(final MapTile aTile) {
        String key = getCacheKey(aTile);
        getCache().removeFromMemoryCache(key);
    }

    public void purgeMemoryCache() {
        getCache().purgeMemoryCache();
    }

    public void purgeDiskCache() {
        getCache().purgeDiskCache();
        sCachedTiles = null;
    }

    public CacheableBitmapDrawable createCacheableBitmapDrawable(Bitmap bitmap, MapTile aTile) {
        return getCache().createCacheableBitmapDrawable(bitmap, getCacheKey(aTile),
                CacheableBitmapDrawable.SOURCE_UNKNOWN);
    }

    public Bitmap getBitmapFromRemoved(final int width, final int height) {
        return getCache().getBitmapFromRemoved(width, height);
    }

    public Bitmap decodeBitmap(final byte[] data, final BitmapFactory.Options opts) {
        return getCache().decodeBitmap(new BitmapLruCache.ByteArrayInputStreamProvider(data), opts);
    }

    public Bitmap decodeBitmap(final BitmapLruCache.InputStreamProvider ip,
                               final BitmapFactory.Options opts) {
        return getCache().decodeBitmap(ip, opts);
    }

    
    public void setDiskCacheEnabled(final boolean enabled) {
        if (mDiskCacheEnabled != enabled) {
            mDiskCacheEnabled = enabled;
            sCachedTiles = null;
        }
    }

    public boolean isDiskCacheEnabled() {
        return mDiskCacheEnabled;
    }
}
