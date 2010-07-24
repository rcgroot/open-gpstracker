package nl.sogeti.android.gpstracker.viewer.proxy;

import org.andnav.osm.views.OpenStreetMapView;
import org.andnav.osm.views.overlay.OpenStreetMapViewOverlay;

import nl.sogeti.android.gpstracker.viewer.FixedMyLocationOverlay;
import nl.sogeti.android.gpstracker.viewer.LoggerMap;

import android.content.Context;
import android.view.View;

import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;


public class MyLocationOverlayProxy implements OverlayProxy
{

   private MyLocationOverlay googleLocationOverlay;
   private org.andnav.osm.views.overlay.MyLocationOverlay osmLocationOverlay;
   private Context mContext;
   
   public MyLocationOverlayProxy(Context ctx, MapViewProxy view)
   {
      mContext = ctx;
      setMyLocationOverlay( view.getMap() );
   }

   public void setMyLocationOverlay( Object mapview )
   {
      if( mapview instanceof MapView )
      {
         googleLocationOverlay = new FixedMyLocationOverlay( mContext, (MapView) mapview );
         if( osmLocationOverlay != null )
         {
            if( osmLocationOverlay.isMyLocationEnabled() )
            {
               googleLocationOverlay.enableMyLocation();
            }
            else
            {
               googleLocationOverlay.disableMyLocation();
            }
         }
         osmLocationOverlay = null;
      }
      if( mapview instanceof OpenStreetMapView )
      {
         osmLocationOverlay = new org.andnav.osm.views.overlay.MyLocationOverlay( mContext, (OpenStreetMapView) mapview );
         if( googleLocationOverlay != null )
         {
            if( googleLocationOverlay.isMyLocationEnabled() )
            {
               osmLocationOverlay.enableMyLocation();
            }
            else
            {
               osmLocationOverlay.disableMyLocation();
            }
         }
         googleLocationOverlay = null;
      }
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
      if( osmLocationOverlay != null )
      {
         osmLocationOverlay.disableMyLocation();
      }
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

   public void enableCompass()
   {
      if( googleLocationOverlay != null )
      {
         googleLocationOverlay.enableCompass();
      }
   }

   public MyLocationOverlay getGoogleOverlay()
   {
      return googleLocationOverlay;
   }

   public org.andnav.osm.views.overlay.MyLocationOverlay getOsmOverlay()
   {
      return osmLocationOverlay;
   }
}
