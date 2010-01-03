package nl.sogeti.android.gpstracker.logger;

interface IGPSLoggerServiceRemote {

    boolean isAlive();
	boolean isLogging();
	void stopLogging();
	long startLogging();
}