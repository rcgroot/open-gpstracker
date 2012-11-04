/*------------------------------------------------------------------------------
 **     Ident: Delivery Center Java
 **    Author: rene
 ** Copyright: (c) Nov 4, 2012 Sogeti Nederland B.V. All Rights Reserved.
 **------------------------------------------------------------------------------
 ** Sogeti Nederland B.V.            |  No part of this file may be reproduced  
 ** Distributed Software Engineering |  or transmitted in any form or by any        
 ** Lange Dreef 17                   |  means, electronic or mechanical, for the      
 ** 4131 NJ Vianen                   |  purpose, without the express written    
 ** The Netherlands                  |  permission of the copyright holder.
 *------------------------------------------------------------------------------
 */
package nl.sogeti.android.gpstracker.fragment;

import nl.sogeti.android.gpstracker.R;
import nl.sogeti.android.gpstracker.googlemapfragment.ActivityHostFragment;
import nl.sogeti.android.gpstracker.googlemapfragment.MyGoogleMapActivity;
import nl.sogeti.android.gpstracker.googlemapfragment.MyMapquestMapActivity;
import nl.sogeti.android.gpstracker.util.SlidingIndicatorView;
import nl.sogeti.android.gpstracker.viewer.map.LoggerMapHelper;
import nl.sogeti.android.gpstracker.viewer.map.overlay.OverlayProvider;
import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.google.android.maps.GeoPoint;
import com.mapquest.android.maps.MapView;
import com.mapquest.android.maps.MyLocationOverlay;

/**
 * ????
 * 
 * @version $Id:$
 * @author rene (c) Nov 4, 2012, Sogeti B.V.
 */
public class MapQuestFragment extends ActivityHostFragment 
{
   private MapView mMapView;
   private MyLocationOverlay mMylocation; 

   /**
    * Constructor: create a new MapQuestFragment.
    */
   public MapQuestFragment()
   {
   }
   
   @Override
   protected Class< ? extends Activity> getActivityClass()
   {
      return MyMapquestMapActivity.class;
   }

   @Override
   public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
   {
      View v = super.onCreateView(inflater, container, savedInstanceState);
      mMapView = (MapView) v.findViewById(R.id.myMapView);
      mMapView.setBuiltInZoomControls(true);
      mMylocation = new MyLocationOverlay(getActivity(), mMapView);
      super.didCreateView(v, savedInstanceState);
      
      return v;
   };

   @Override
   public void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);
      setHasOptionsMenu(true);
      
   }   
   
   @Override
   public void onDestroyView()
   {
      mMapView = null;
      super.onDestroyView();
   }

   /******************************/
   /** Loggermap methods        **/ 
   /******************************/
   
   @Override
   public void updateOverlays()
   {
   }
   
   @Override
   public void setDrawingCacheEnabled(boolean b)
   {
      mMapView.getRootView().setDrawingCacheEnabled(true);
   }
   
   @Override
   public void onLayerCheckedChanged(int checkedId, boolean isChecked)
   {
   }

   @Override
   public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
   {
   }

   @Override
   public Bitmap getDrawingCache()
   {
      return mMapView.getRootView().getDrawingCache();
   }

   @Override
   public void showMediaDialog(BaseAdapter mediaAdapter)
   {
      mHelper.showMediaDialog(mediaAdapter);
   }

   public void onDateOverlayChanged()
   {
      mMapView.postInvalidate();
   }
   
   @Override
   public String getDataSourceId()
   {
      return LoggerMapHelper.MAPQUEST_PROVIDER;
   }

   @Override
   public boolean isOutsideScreen(GeoPoint lastPoint)
   {
      Point out = new Point();
      this.mMapView.getProjection().toPixels(convertGeoPoint(lastPoint), out);
      int height = this.mMapView.getHeight();
      int width = this.mMapView.getWidth();
      return (out.x < 0 || out.y < 0 || out.y > height || out.x > width);
   }

   @Override
   public boolean isNearScreenEdge(GeoPoint lastPoint)
   {
      Point out = new Point();
      this.mMapView.getProjection().toPixels(convertGeoPoint(lastPoint), out);
      int height = this.mMapView.getHeight();
      int width = this.mMapView.getWidth();
      return (out.x < width / 4 || out.y < height / 4 || out.x > (width / 4) * 3 || out.y > (height / 4) * 3);
   }

   @Override
   public void executePostponedActions()
   {
   }
   
   @Override
   public void enableCompass()
   {
      mMylocation.enableCompass();
   }

   @Override
   public void enableMyLocation()
   {
      mMylocation.enableMyLocation(); 
   }
   @Override
   public void disableMyLocation()
   {
      mMylocation.disableMyLocation();
   }

   @Override
   public void disableCompass()
   {
      mMylocation.disableCompass();
   }

   @Override
   public void setZoom(int zoom)
   {
      mMapView.getController().setZoom(zoom);
   }

   @Override
   public void animateTo(GeoPoint storedPoint)
   {
      mMapView.getController().animateTo(convertGeoPoint(storedPoint));
   }

   @Override
   public int getZoomLevel()
   {
      return mMapView.getZoomLevel();
   }

   @Override
   public GeoPoint getMapCenter()
   {
      return convertMapQuestGeoPoint(mMapView.getMapCenter());
   }

   @Override
   public boolean zoomOut()
   {
      return mMapView.getController().zoomOut();
   }

   @Override
   public boolean zoomIn()
   {
      return  mMapView.getController().zoomIn();
   }

   @Override
   public void postInvalidate()
   {
      mMapView.postInvalidate();
   }

   @Override
   public void addOverlay(OverlayProvider overlay)
   {
      mMapView.getOverlays().add(overlay.getMapQuestOverlay());
   }

   @Override
   public void clearAnimation()
   {
      mMapView.clearAnimation();
   }

   @Override
   public void setCenter(GeoPoint lastPoint)
   {
      mMapView.getController().setCenter( convertGeoPoint(lastPoint));
   }

   @Override
   public int getMaxZoomLevel()
   {
      return mMapView.getMaxZoomLevel();
   }

   @Override
   public GeoPoint fromPixels(int x, int y)
   {
      com.mapquest.android.maps.GeoPoint mqGeopoint = mMapView.getProjection().fromPixels(x, y);
      return convertMapQuestGeoPoint(mqGeopoint);
   }

   @Override
   public boolean hasProjection()
   {
      return mMapView.getProjection() != null;
   }

   @Override
   public float metersToEquatorPixels(float float1)
   {
      return mMapView.getProjection().metersToEquatorPixels(float1);
   }

   @Override
   public void toPixels(GeoPoint geoPoint, Point screenPoint)
   {
      com.mapquest.android.maps.GeoPoint mqGeopoint = convertGeoPoint(geoPoint);
      mMapView.getProjection().toPixels( mqGeopoint, screenPoint);
   }
   
   static com.mapquest.android.maps.GeoPoint convertGeoPoint( GeoPoint point )
   {
      return new com.mapquest.android.maps.GeoPoint(point.getLatitudeE6(), point.getLongitudeE6() );
   }
   
   static GeoPoint convertMapQuestGeoPoint( com.mapquest.android.maps.GeoPoint mqPoint )
   {
      return new GeoPoint(mqPoint.getLatitudeE6(), mqPoint.getLongitudeE6() );
   }

   @Override
   public void clearOverlays()
   {
      mMapView.getOverlays().clear();
   }
   
   @Override
   public SlidingIndicatorView getScaleIndicatorView()
   {
      return (SlidingIndicatorView) getView().findViewById(R.id.scaleindicator);
   }   
}
