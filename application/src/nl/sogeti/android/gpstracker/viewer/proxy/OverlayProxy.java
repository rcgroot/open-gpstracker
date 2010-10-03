package nl.sogeti.android.gpstracker.viewer.proxy;

import org.andnav.osm.views.overlay.OpenStreetMapViewOverlay;

import com.google.android.maps.Overlay;

public interface OverlayProxy
{
   public Overlay getGoogleOverlay();
   public OpenStreetMapViewOverlay getOSMOverlay();
   
}
