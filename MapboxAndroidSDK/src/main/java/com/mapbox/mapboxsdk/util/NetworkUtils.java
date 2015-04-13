/**
 * @author Brad Leege <bleege@gmail.com>
 * Created on 2/15/14 at 3:26 PM
 */

package com.mapbox.mapboxsdk.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.util.Log;

import com.squareup.okhttp.Cache;
import com.squareup.okhttp.OkHttpClient;

import java.io.File;
import java.io.IOException;

public class NetworkUtils {
    static final String TAG = "MapTileCache";
    public static final String DISK_TILES_CACHE_SUBDIR = "mapbox_tiles_cache";
    public static final String DISK_HTTP_CACHE_SUBDIR = "mapbox_http_cache";
    /**
     * Creates a unique subdirectory of the designated app cache directory. Tries to use external
     * but if not mounted, falls back on internal storage.
     */
    public static File getDiskCacheDir(Context context, String uniqueName) {
        // Check if media is mounted or storage is built-in, if so, try and use external cache dir
        // otherwise use internal cache dir
        final String cachePath =
                context.getExternalCacheDir() != null && (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                        || (!Environment.isExternalStorageRemovable()))
                        ? context.getExternalCacheDir().getPath()
                        : context.getCacheDir().getPath();
//        Log.i(TAG, "cachePath: '" + cachePath + "'");

        return new File(cachePath, uniqueName);
    }

    
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

//    public static Request getHttpRequest(final URL url) {
//        return getHttpRequest(url, null, null);
//    }
//
//    public static Request getHttpRequest(final URL url, final Cache cache) {
//        return getHttpRequest(url, cache, null);
//    }
    
    private static OkHttpClient _httpClient = null;
    public static OkHttpClient getOkHttpClient() {
        if (_httpClient == null) {
            _httpClient = new OkHttpClient();
            _httpClient.interceptors().add(new com.squareup.okhttp.Interceptor() {
                @Override public com.squareup.okhttp.Response intercept(Chain chain) throws IOException {
                    com.squareup.okhttp.Request.Builder builder = chain.request().newBuilder();
                    builder.addHeader("User-Agent", MapboxUtils.getUserAgent());
                    return chain.proceed(builder.build());
                }
              });
        }
        return _httpClient;
    }
    
    public static void createHttpCacheIfNecessary(final Context context) {
        Cache cache = getOkHttpClient().getCache();
        if (cache == null) {
            int cacheSize = 10 * 1024 * 1024; // 10 MiB
            File cacheDir = getDiskCacheDir(context, DISK_HTTP_CACHE_SUBDIR);
            if (cacheDir.exists() || cacheDir.mkdirs()) {
                cache = new Cache(cacheDir, cacheSize);
                _httpClient.setCache(cache);
            } else {
                Log.e(TAG, "can't create cacheDir " + cacheDir);
            }
        }
    }

    public static com.squareup.okhttp.Request getHttpRequest(final String url) {
        return new com.squareup.okhttp.Request.Builder()
            .url(url)
            .build();
    }

    public static Cache getCache(final File cacheDir, final int maxSize) throws IOException {
        return new Cache(cacheDir, maxSize);
    }
}
