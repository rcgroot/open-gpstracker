package nl.sogeti.android.gpstracker.viewer.proxy;


public interface OverlayProxy
{
   public com.google.android.maps.Overlay getGoogleOverlay();
   public org.osmdroid.views.overlay.Overlay getOSMOverlay();
   public void stopCalculations(); 
   
}
