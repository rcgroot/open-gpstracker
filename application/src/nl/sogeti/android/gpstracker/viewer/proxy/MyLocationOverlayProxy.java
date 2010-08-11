package nl.sogeti.android.gpstracker.viewer.proxy;

import nl.sogeti.android.gpstracker.viewer.FixedMyLocationOverlay;

import android.content.Context;
import android.view.View;

import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;


public class MyLocationOverlayProxy implements OverlayProxy
{
   private static final String TAG = "OGT.MyLocationOverlayProxy";

   private MyLocationOverlay googleLocationOverlay;
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

   public Object getOverlay()
   {
      View mapview = mMapViewProxy.getMap();
      if( mapview instanceof MapView )
      {
         googleLocationOverlay = new FixedMyLocationOverlay( mContext, (MapView) mapview );
         return googleLocationOverlay;
      }
      return null;
   }
}
