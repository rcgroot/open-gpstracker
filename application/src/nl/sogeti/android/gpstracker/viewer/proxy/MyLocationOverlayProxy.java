package nl.sogeti.android.gpstracker.viewer.proxy;

import org.andnav.osm.views.overlay.OpenStreetMapViewOverlay;

import nl.sogeti.android.gpstracker.viewer.FixedMyLocationOverlay;

import android.content.Context;

import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;


public class MyLocationOverlayProxy implements OverlayProxy
{

   private MyLocationOverlay googleLocationOverlay;

   public MyLocationOverlayProxy( Context context, MapViewProxy mMapView)
   {
      googleLocationOverlay = new FixedMyLocationOverlay( context, mMapView.getGoogleMapView() );
   }

   public void disableCompass()
   {
      if( googleLocationOverlay != null )
      {
         googleLocationOverlay.disableCompass();
      }
   }

   public void disableMyLocation()
   {
      if( googleLocationOverlay != null )
      {
         googleLocationOverlay.disableMyLocation();
      }
   }

   public void enableMyLocation()
   {
      if( googleLocationOverlay != null )
      {
         googleLocationOverlay.enableMyLocation();
      }
   }

   public void enableCompass()
   {
      if( googleLocationOverlay != null )
      {
         googleLocationOverlay.enableCompass();
      }
   }

   public Overlay getGoogleOverlay()
   {
      return googleLocationOverlay;
   }

   public OpenStreetMapViewOverlay getOsmOverlay()
   {
      // TODO Auto-generated method stub
      return null;
   }


}
