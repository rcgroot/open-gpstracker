package nl.sogeti.android.gpstracker.viewer.proxy;

import nl.sogeti.android.gpstracker.viewer.FixedMyLocationOverlay;

import org.andnav.osm.views.OpenStreetMapView;

import android.content.Context;
import android.view.View;

import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;


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

   public Object getOverlay()
   {
      View mapview = mMapViewProxy.getMap();
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
         return googleLocationOverlay;
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
         return osmLocationOverlay;
      }
      return null;
   }
}
