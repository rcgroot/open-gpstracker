package nl.sogeti.android.gpstracker.logger;

interface IGPSLoggerServiceRemote {

	boolean isLogging();
	void stopLogging();
	long startLogging();
}