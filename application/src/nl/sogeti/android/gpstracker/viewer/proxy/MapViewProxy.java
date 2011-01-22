package nl.sogeti.android.gpstracker.viewer.proxy;

import java.util.List;

import nl.sogeti.android.gpstracker.util.Constants;

import org.osmdroid.tileprovider.tilesource.TileSourceFactory;

import android.util.Log;
import android.view.View;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

public class MapViewProxy
{
   private static final String TAG = "OGT.MapViewProxy";
   private MapView mGoogleMapView;
   private MapControllerProxy mMapControllerProxy;
   private ProjectionProxy mProjectionProxy;
   
   private org.osmdroid.views.MapView mOpenStreetMapView;
   private boolean buildinzoom;

   public MapViewProxy()
   {
      mProjectionProxy = new ProjectionProxy();
      mMapControllerProxy = new MapControllerProxy();
   }
   
   public void setMap( View newView )
   {
      if( newView instanceof MapView )
      {
         mGoogleMapView = (MapView) newView;
         mMapControllerProxy.setController( mGoogleMapView.getController() );
         mProjectionProxy.setProjection( mGoogleMapView.getProjection() );
         
         if( mOpenStreetMapView != null )
         {
            GeoPoint mapCenter = convertOSMGeoPoint( mOpenStreetMapView.getMapCenter() );
            int zoomLevel = mOpenStreetMapView.getZoomLevel();
            mMapControllerProxy.setCenter( mapCenter );
            mMapControllerProxy.setZoom( zoomLevel );
         }
         mOpenStreetMapView = null;
      }
      else if( newView instanceof org.osmdroid.views.MapView )
      {
         mOpenStreetMapView = (org.osmdroid.views.MapView) newView;
         mMapControllerProxy.setController( mOpenStreetMapView );
         mProjectionProxy.setProjection( mOpenStreetMapView );
         
         
         if( mGoogleMapView != null )
         {
            GeoPoint mapCenter = mGoogleMapView.getMapCenter();
            int zoomLevel = mGoogleMapView.getZoomLevel();
            mMapControllerProxy.setCenter( mapCenter );
            mMapControllerProxy.setZoom( zoomLevel );
         }
         mGoogleMapView = null;
      }
      else 
      {
         Log.e( TAG, "Unusable map provided: "+ newView);
         throw new IllegalStateException( "Unusable map provided" );
      }
      setBuiltInZoomControls( buildinzoom );
   }
   
   protected View getMap()
   {
      if( mGoogleMapView != null )
      {
         return mGoogleMapView;
      }
      else if( mOpenStreetMapView != null )
      {
         return mOpenStreetMapView;
      }
      else 
      {
         return null;
      }
   }
   
   public void postInvalidate()
   {
      if( mGoogleMapView != null )
      {
         mGoogleMapView.postInvalidate();
      }
      if( mOpenStreetMapView != null )
      {
         mOpenStreetMapView.postInvalidate();
      }
   }
   
   public void invalidate()
   {
      if( mGoogleMapView != null )
      {
         mGoogleMapView.invalidate();
      }
      if( mOpenStreetMapView != null )
      {
         mOpenStreetMapView.invalidate();
      }
   }

   public void clearAnimation()
   {
      if( mGoogleMapView != null )
      {
         mGoogleMapView.clearAnimation();
      }
      if( mOpenStreetMapView != null )
      {
         mOpenStreetMapView.clearAnimation();
      }
   }

   public void clearOverlays()
   {
      if( mGoogleMapView != null )
      {
         for( Overlay overlay : mGoogleMapView.getOverlays() )
         {
            if( overlay instanceof OverlayProxy )
            {
               ((OverlayProxy) overlay).stopCalculations();
            }
         }
         mGoogleMapView.getOverlays().clear();
      }
      if( mOpenStreetMapView != null )
      {
         List<org.osmdroid.views.overlay.Overlay> overlays = mOpenStreetMapView.getOverlays();
         org.osmdroid.views.overlay.Overlay baseLayar = overlays.get(0);
         for( org.osmdroid.views.overlay.Overlay overlay : mOpenStreetMapView.getOverlays() )
         {
            if( overlay instanceof OverlayProxy )
            {
               ((OverlayProxy) overlay).stopCalculations();
            }
         }
         overlays.clear();
         overlays.add(baseLayar);
      }
   }

   public MapControllerProxy getController()
   {
      return mMapControllerProxy;
   }

   public ProjectionProxy getProjection()
   {
      return mProjectionProxy;
   }

   /**
    * To maintain state do not alter this list, use the MapViewProxy methods instead
    *  
    * @return The list of overlays
    */
   public List<?> getOverlays()
   {
      if( mGoogleMapView != null )
      {
         return mGoogleMapView.getOverlays();
      }
      if( mOpenStreetMapView != null )
      {
         return mOpenStreetMapView.getOverlays();
      }
      return null;
   }

   public GeoPoint getMapCenter()
   {
      if( mGoogleMapView != null )
      {
         return mGoogleMapView.getMapCenter();
      }
      if( mOpenStreetMapView != null )
      {
         return convertOSMGeoPoint( mOpenStreetMapView.getMapCenter() );
      }
      return null;
   }

   public int getHeight()
   {
      if( mGoogleMapView != null )
      {
         return mGoogleMapView.getHeight();
      }      
      if( mOpenStreetMapView != null )
      {
         return mOpenStreetMapView.getHeight();
      }
      return 0;
   }

   public int getWidth()
   {
      if( mGoogleMapView != null )
      {
         return mGoogleMapView.getWidth();
      }
      else if( mOpenStreetMapView != null )
      {
         return mOpenStreetMapView.getWidth();
      }
      return 0;
   }

   public int getZoomLevel()
   {
      int zoomlevel = -1;
      if( mGoogleMapView != null )
      {
         zoomlevel = mGoogleMapView.getZoomLevel();
      }
      else if( mOpenStreetMapView != null )
      {
         zoomlevel = mOpenStreetMapView.getZoomLevel();
      }
      return zoomlevel;
   }

   public int getMaxZoomLevel()
   {
      if( mGoogleMapView != null )
      {
         return mGoogleMapView.getMaxZoomLevel();
      }
      else if( mOpenStreetMapView != null )
      {
         return mOpenStreetMapView.getMaxZoomLevel();
      }
      return 0;
   }

   public void addOverlay( OverlayProxy overlay )
   {
      if( mGoogleMapView != null  )
      {
         mGoogleMapView.getOverlays().add( overlay.getGoogleOverlay() );
      }
      else if( mOpenStreetMapView != null )
      {
         mOpenStreetMapView.getOverlays().add( overlay.getOSMOverlay() );
      }
   }

   public void setBuiltInZoomControls( boolean b )
   {
      buildinzoom = b;
      if( mGoogleMapView != null )
      {
         mGoogleMapView.setBuiltInZoomControls( b );
      }
      else if( mOpenStreetMapView != null )
      {
         mOpenStreetMapView.setBuiltInZoomControls( b );
      }
   }

   public void setClickable( boolean b )
   {
      if( mGoogleMapView != null )
      {
         mGoogleMapView.setClickable( b );
      }
      else if( mOpenStreetMapView != null )
      {
         mOpenStreetMapView.setClickable( b );
      }
      
   }

   static GeoPoint convertOSMGeoPoint( org.osmdroid.util.GeoPoint point )
   {
      return new GeoPoint(point.getLatitudeE6(), point.getLongitudeE6() );
   }
   
   static org.osmdroid.util.GeoPoint convertMapGeoPoint( GeoPoint point )
   {
      return new org.osmdroid.util.GeoPoint(point.getLatitudeE6(), point.getLongitudeE6() );
   }

   public boolean isSatellite()
   {
      if( mGoogleMapView != null )
      {
         return mGoogleMapView.isSatellite();
      }
      return false;
   }

   public boolean isTraffic()
   {
      if( mGoogleMapView != null )
      {
         return mGoogleMapView.isTraffic();
      }
      return false;
   }

   public void setTraffic( boolean b )
   {
      if( mGoogleMapView != null )
      {
         mGoogleMapView.setTraffic( b );
      }
   }

   public void setSatellite( boolean b )
   {
      if( mGoogleMapView != null )
      {
         mGoogleMapView.setSatellite( b );
      }
   }
   
   public void setOSMType(int renderer )
   {
      if( mOpenStreetMapView != null )
      {
         switch( renderer )
         {
            case Constants.OSM_CLOUDMADE:
               mOpenStreetMapView.setTileSource(TileSourceFactory.CLOUDMADESTANDARDTILES);
               break;
            case Constants.OSM_MAKNIK:
               mOpenStreetMapView.setTileSource(TileSourceFactory.MAPNIK);
               break;
            case Constants.OSM_CYCLE:
               mOpenStreetMapView.setTileSource(TileSourceFactory.CYCLEMAP);
               break;
            default:
               break;
         }
         
      }
   }
   
   public void executePostponedActions()
   {
      getController().executePostponedActions();
   }
}
