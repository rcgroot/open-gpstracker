package nl.sogeti.android.gpstracker.logger;

import android.net.Uri;
import android.location.Location;

interface IGPSLoggerServiceRemote {

	int loggingState();
    long startLogging();
    void pauseLogging();
    long resumeLogging();
	void stopLogging();
    boolean isMediaPrepared();
	Uri storeMediaUri(in Uri mediaUri);
    Uri storeMetaData(in String key, in String value);
    Location getLastWaypoint();
    float getTrackedDistance();
}