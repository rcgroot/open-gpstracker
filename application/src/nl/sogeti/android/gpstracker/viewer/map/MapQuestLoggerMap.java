/*------------------------------------------------------------------------------
 **     Ident: Delivery Center Java
 **    Author: rene
 ** Copyright: (c) Mar 3, 2012 Sogeti Nederland B.V. All Rights Reserved.
 **------------------------------------------------------------------------------
 ** Sogeti Nederland B.V.            |  No part of this file may be reproduced  
 ** Distributed Software Engineering |  or transmitted in any form or by any        
 ** Lange Dreef 17                   |  means, electronic or mechanical, for the      
 ** 4131 NJ Vianen                   |  purpose, without the express written    
 ** The Netherlands                  |  permission of the copyright holder.
 *------------------------------------------------------------------------------
 */
package nl.sogeti.android.gpstracker.viewer.map;

import java.util.LinkedList;
import java.util.List;

import nl.sogeti.android.gpstracker.R;
import nl.sogeti.android.gpstracker.util.SlidingIndicatorView;
import nl.sogeti.android.gpstracker.viewer.map.AsyncOverlay.SegmentMapQuestOverlay;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.google.android.maps.GeoPoint;
import com.mapquest.android.maps.MapActivity;
import com.mapquest.android.maps.MapView;
import com.mapquest.android.maps.MyLocationOverlay;
import com.mapquest.android.maps.Overlay;

/**
 * ????
 *
 * @version $Id:$
 * @author rene (c) Mar 3, 2012, Sogeti B.V.
 */
public class MapQuestLoggerMap extends MapActivity implements LoggerMap
{
   LoggerMapHelper mHelper;
   private MapView mMapView;
   private TextView[] mSpeedtexts;
   private TextView mLastGPSSpeedView;
   private TextView mLastGPSAltitudeView;
   private TextView mDistanceView;
   private MyLocationOverlay mMylocation;
   
   /**
    * Called when the activity is first created.
    */
   @Override
   protected void onCreate(Bundle load)
   {
      super.onCreate(load);
      setContentView(R.layout.map_mapquest);
      
      mMapView = (MapView) findViewById(R.id.myMapView);
      mHelper = new LoggerMapHelper(this);
      mMapView = (MapView) findViewById(R.id.myMapView);
      mMylocation = new MyLocationOverlay(this, mMapView);
      mMapView.setBuiltInZoomControls(true);
      TextView[] speeds = { (TextView) findViewById(R.id.speedview05), (TextView) findViewById(R.id.speedview04), (TextView) findViewById(R.id.speedview03),
            (TextView) findViewById(R.id.speedview02), (TextView) findViewById(R.id.speedview01), (TextView) findViewById(R.id.speedview00) };
      mSpeedtexts = speeds;
      mLastGPSSpeedView = (TextView) findViewById(R.id.currentSpeed);
      mLastGPSAltitudeView = (TextView) findViewById(R.id.currentAltitude);
      mDistanceView = (TextView) findViewById(R.id.currentDistance);
      
      mHelper.onCreate(load);
   }
   
   @Override
   protected void onResume()
   {
      super.onResume();
      mHelper.onResume();
   }
   
   @Override
   protected void onPause()
   {
      mHelper.onPause();
      super.onPause();
   }
   
   @Override
   protected void onDestroy()
   {
      mHelper.onDestroy();
      super.onDestroy();
   }
   
   @Override
   public void onNewIntent(Intent newIntent)
   {
      mHelper.onNewIntent(newIntent);
   }

   @Override
   protected void onRestoreInstanceState(Bundle load)
   {
      if (load != null)
      {
         super.onRestoreInstanceState(load);
      }
      mHelper.onRestoreInstanceState(load);
   }
   
   @Override
   protected void onSaveInstanceState(Bundle save)
   {
      super.onSaveInstanceState(save);
      mHelper.onSaveInstanceState(save);
   }
   
   @Override
   public boolean onCreateOptionsMenu(Menu menu)
   {
      boolean result = super.onCreateOptionsMenu(menu);
      mHelper.onCreateOptionsMenu(menu);
      return result;
   }
   
   @Override
   public boolean onPrepareOptionsMenu(Menu menu)
   {
      mHelper.onPrepareOptionsMenu(menu);
      return super.onPrepareOptionsMenu(menu);
   }
   
   @Override
   public boolean onOptionsItemSelected(MenuItem item)
   {
      boolean handled = mHelper.onOptionsItemSelected(item);
      if( !handled )
      {
         handled = super.onOptionsItemSelected(item);
      }
      return handled;
   }
   
   @Override
   protected void onActivityResult(int requestCode, int resultCode, Intent intent)
   {
      super.onActivityResult(requestCode, resultCode, intent);
      mHelper.onActivityResult(requestCode, resultCode, intent);
   }
   
   @Override
   public boolean onKeyDown(int keyCode, KeyEvent event)
   {
      boolean propagate = true;
      switch (keyCode)
      {
         default:
            propagate = mHelper.onKeyDown(keyCode, event);
            if( propagate )
            {
               propagate = super.onKeyDown(keyCode, event);
            }
            break;
      }
      return propagate;
   }
   
   @Override
   protected Dialog onCreateDialog(int id)
   {
      Dialog dialog = mHelper.onCreateDialog(id);
      if( dialog == null )
      {
         dialog = super.onCreateDialog(id);
      }
      return dialog;
   }
   
   @Override
   protected void onPrepareDialog(int id, Dialog dialog)
   {
      mHelper.onPrepareDialog(id, dialog);
      super.onPrepareDialog(id, dialog);
   }
   
   /******************************/
   /** Loggermap methods        **/ 
   /******************************/
   
   public void updateOverlays()
   {
   }
   
   public void setDrawingCacheEnabled(boolean b)
   {
      findViewById(R.id.mapScreen).setDrawingCacheEnabled(true);
   }
   
   public Activity getActivity()
   {
      return this;
   }

   public void onLayerCheckedChanged(int checkedId, boolean isChecked)
   {
   }

   public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
   {
   }

   public Bitmap getDrawingCache()
   {
      return findViewById(R.id.mapScreen).getDrawingCache();
   }

   public void showMediaDialog(BaseAdapter mediaAdapter)
   {
      mHelper.showMediaDialog(mediaAdapter);
   }

   public void onDateOverlayChanged()
   {
      mMapView.postInvalidate();
   }
   
   public String getDataSourceId()
   {
      return LoggerMapHelper.MAPQUEST_PROVIDER;
   }

   public boolean isOutsideScreen(GeoPoint lastPoint)
   {
      Point out = new Point();
      this.mMapView.getProjection().toPixels(MapQuestLoggerMap.convertGeoPoint(lastPoint), out);
      int height = this.mMapView.getHeight();
      int width = this.mMapView.getWidth();
      return (out.x < 0 || out.y < 0 || out.y > height || out.x > width);
   }

   public boolean isNearScreenEdge(GeoPoint lastPoint)
   {
      Point out = new Point();
      this.mMapView.getProjection().toPixels(MapQuestLoggerMap.convertGeoPoint(lastPoint), out);
      int height = this.mMapView.getHeight();
      int width = this.mMapView.getWidth();
      return (out.x < width / 4 || out.y < height / 4 || out.x > (width / 4) * 3 || out.y > (height / 4) * 3);
   }

   public void executePostponedActions()
   {
   }
   
   public void enableCompass()
   {
      mMylocation.enableCompass();
   }

   public void enableMyLocation()
   {
      mMylocation.enableMyLocation(); 
   }
   public void disableMyLocation()
   {
      mMylocation.disableMyLocation();
   }

   public void disableCompass()
   {
      mMylocation.disableCompass();
   }

   public void setZoom(int zoom)
   {
      mMapView.getController().setZoom(zoom);
   }

   public void animateTo(GeoPoint storedPoint)
   {
      mMapView.getController().animateTo(MapQuestLoggerMap.convertGeoPoint(storedPoint));
   }

   public int getZoomLevel()
   {
      return mMapView.getZoomLevel();
   }

   public GeoPoint getMapCenter()
   {
      return MapQuestLoggerMap.convertMapQuestGeoPoint(mMapView.getMapCenter());
   }

   public boolean zoomOut()
   {
      return mMapView.getController().zoomOut();
   }

   public boolean zoomIn()
   {
      return  mMapView.getController().zoomIn();
   }

   public void postInvalidate()
   {
      mMapView.postInvalidate();
   }

   public void addOverlay(OverlayProvider overlay)
   {
      mMapView.getOverlays().add(overlay.getMapQuestOverlay());
   }

   public void clearAnimation()
   {
      mMapView.clearAnimation();
   }

   public void setCenter(GeoPoint lastPoint)
   {
      mMapView.getController().setCenter( MapQuestLoggerMap.convertGeoPoint(lastPoint));
   }

   public int getMaxZoomLevel()
   {
      return mMapView.getMaxZoomLevel();
   }

   public GeoPoint fromPixels(int x, int y)
   {
      com.mapquest.android.maps.GeoPoint mqGeopoint = mMapView.getProjection().fromPixels(x, y);
      return convertMapQuestGeoPoint(mqGeopoint);
   }

   public boolean hasProjection()
   {
      return mMapView.getProjection() != null;
   }

   public float metersToEquatorPixels(float float1)
   {
      return mMapView.getProjection().metersToEquatorPixels(float1);
   }

   public void toPixels(GeoPoint geoPoint, Point screenPoint)
   {
      com.mapquest.android.maps.GeoPoint mqGeopoint = MapQuestLoggerMap.convertGeoPoint(geoPoint);
      mMapView.getProjection().toPixels( mqGeopoint, screenPoint);
   }

   public TextView[] getSpeedTextViews()
   {
      return mSpeedtexts;
   }

   public TextView getAltitideTextView()
   {
      return mLastGPSAltitudeView;
   }

   public TextView getSpeedTextView()
   {
      return mLastGPSSpeedView;
   }

   public TextView getDistanceTextView()
   {
      return mDistanceView;
   }
   
   static com.mapquest.android.maps.GeoPoint convertGeoPoint( GeoPoint point )
   {
      return new com.mapquest.android.maps.GeoPoint(point.getLatitudeE6(), point.getLongitudeE6() );
   }
   
   static GeoPoint convertMapQuestGeoPoint( com.mapquest.android.maps.GeoPoint mqPoint )
   {
      return new GeoPoint(mqPoint.getLatitudeE6(), mqPoint.getLongitudeE6() );
   }

   public void clearOverlays()
   {
      mMapView.getOverlays().clear();
   }
   
   public SlidingIndicatorView getScaleIndicatorView()
   {
      return (SlidingIndicatorView) findViewById(R.id.scaleindicator);
   }

   /******************************/
   /** Own methods              **/ 
   /******************************/
   
   @Override
   public boolean isRouteDisplayed()
   {
      return true;
   }

}
