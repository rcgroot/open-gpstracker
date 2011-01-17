package nl.sogeti.android.gpstracker.viewer.proxy;

import nl.sogeti.android.gpstracker.viewer.FixedMyLocationOverlay;

import org.andnav.osm.views.OpenStreetMapView;
import org.andnav.osm.views.overlay.OpenStreetMapViewOverlay;

import android.content.Context;
import android.location.Location;
import android.view.View;

import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;


public class MyLocationOverlayProxy implements OverlayProxy
{
   @SuppressWarnings("unused")
   private static final String TAG = "OGT.MyLocationOverlayProxy";

   private MyLocationOverlay googleLocationOverlay;
   private org.andnav.osm.views.overlay.MyLocationOverlay osmLocationOverlay;
   private Context mContext;

   private MapViewProxy mMapViewProxy;
   
   public MyLocationOverlayProxy(Context ctx, MapViewProxy view)
   {
      mContext = ctx;
      mMapViewProxy = view;
   }

   public void enableMyLocation()
   {
      if( googleLocationOverlay != null )
      {
         googleLocationOverlay.enableMyLocation();
      }
      if( osmLocationOverlay != null )
      {
         osmLocationOverlay.enableMyLocation();
      }
   }

   public void disableMyLocation()
   {
      if( googleLocationOverlay != null )
      {
         googleLocationOverlay.disableMyLocation();
      }
      if( osmLocationOverlay != null )
      {
         osmLocationOverlay.disableMyLocation();
         osmLocationOverlay.onLocationChanged( new Location( "STUB" ) );
         mMapViewProxy.invalidate();
      }
   }

   public void enableCompass()
   {
      if( googleLocationOverlay != null )
      {
         googleLocationOverlay.enableCompass();
      }
      if( osmLocationOverlay != null )
      {
         osmLocationOverlay.enableCompass();
      }
   }

   public void disableCompass()
   {
      if( googleLocationOverlay != null )
      {
         googleLocationOverlay.disableCompass();
      }
      if( osmLocationOverlay != null )
      {
         osmLocationOverlay.disableCompass();
         mMapViewProxy.invalidate();
      }
   }

   public Overlay getGoogleOverlay()
   {
      View mapview = mMapViewProxy.getMap();
      googleLocationOverlay = new FixedMyLocationOverlay( mContext, (MapView) mapview );
      return googleLocationOverlay;
   }

   public OpenStreetMapViewOverlay getOSMOverlay()
   {
      View mapview = mMapViewProxy.getMap();
      osmLocationOverlay = new org.andnav.osm.views.overlay.MyLocationOverlay( mContext, (OpenStreetMapView) mapview );
      return osmLocationOverlay;
   }

   public void stopCalculations()
   {
      // noop no calculations going on here
   }
}
