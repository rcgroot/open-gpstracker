package nl.sogeti.android.gpstracker.settings;

import android.content.SharedPreferences;

/**
 * Created by rene on 15-11-15.
 */
public class Helper {

    public static final String LEGACY_BROADCAST_STREAM = "STREAM_ENABLED";
    public static final String LEGACY_PRECISION_PREFERENCE = "precision";
    public static final String LEGACY_CUSTOMPRECISIONTIME_PREFERENCE = "customprecisiontime";
    public static final String LEGACY_CUSTOMPRECISIONDISTANCE_PREFERENCE = "customprecisiondistance";
    public static final String LEGACY_STREAMBROADCAST_PREFERENCE = "streambroadcast_distance";

    public static final String BROADCAST_STREAM = "APP_SETTING_BROADCAST_STREAM";
    public static final String BROADCAST_STREAM_DISTANCE = "streambroadcast_distance";
    public static final String BROADCAST_STREAM_DISTANCE_METER = "streambroadcast_distance_meter";
    public static final String CUSTOM_UPLOAD_BACKLOG = "APP_SETTING_CUSTOM_UPLOAD_BACKLOG";
    public static final String PRECISION = "APP_SETTING_PRECISION";
    public static final String CUSTOM_PRECISION_TIME = "APP_SETTING_CUSTOM_PRECISION_TIME";
    public static final String CUSTOM_DISTANCE_TIME = "APP_SETTING_CUSTOM_DISTANCE_TIME";


    public static boolean getBoolean(SharedPreferences sharedPreferences, String legacyKey, String key, boolean defaultValue) {
        return sharedPreferences.getBoolean(key, sharedPreferences.getBoolean(legacyKey, defaultValue));
    }
}
