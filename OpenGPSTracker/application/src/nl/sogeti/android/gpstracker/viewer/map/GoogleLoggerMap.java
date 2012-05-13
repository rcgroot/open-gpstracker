/*------------------------------------------------------------------------------
 **     Ident: Sogeti Smart Mobile Solutions
 **    Author: rene
 ** Copyright: (c) Apr 24, 2011 Sogeti Nederland B.V. All Rights Reserved.
 **------------------------------------------------------------------------------
 ** Sogeti Nederland B.V.            |  No part of this file may be reproduced  
 ** Distributed Software Engineering |  or transmitted in any form or by any        
 ** Lange Dreef 17                   |  means, electronic or mechanical, for the      
 ** 4131 NJ Vianen                   |  purpose, without the express written    
 ** The Netherlands                  |  permission of the copyright holder.
 *------------------------------------------------------------------------------
 *
 *   This file is part of OpenGPSTracker.
 *
 *   OpenGPSTracker is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   OpenGPSTracker is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with OpenGPSTracker.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package nl.sogeti.android.gpstracker.viewer.map;

import java.util.LinkedList;
import java.util.List;

import nl.sogeti.android.gpstracker.R;
import nl.sogeti.android.gpstracker.util.Constants;
import nl.sogeti.android.gpstracker.util.SlidingIndicatorView;
import nl.sogeti.android.gpstracker.viewer.map.overlay.FixedMyLocationOverlay;
import nl.sogeti.android.gpstracker.viewer.map.overlay.OverlayProvider;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

/**
 * Main activity showing a track and allowing logging control
 * 
 * @version $Id$
 * @author rene (c) Jan 18, 2009, Sogeti B.V.
 */
public class GoogleLoggerMap extends MapActivity implements LoggerMap
{
   LoggerMapHelper mHelper;
   private MapView mMapView;
   private TextView[] mSpeedtexts;
   private TextView mLastGPSSpeedView;
   private TextView mLastGPSAltitudeView;
   private TextView mDistanceView;
   private FixedMyLocationOverlay mMylocation;
   
   /**
    * Called when the activity is first created.
    */
   @Override
   protected void onCreate(Bundle load)
   {
      super.onCreate(load);
      setContentView(R.layout.map_google);
      
      mHelper = new LoggerMapHelper(this);
      mMapView = (MapView) findViewById(R.id.myMapView);
      mMylocation = new FixedMyLocationOverlay(this, mMapView);
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
         case KeyEvent.KEYCODE_S:
            setSatelliteOverlay(!this.mMapView.isSatellite());
            propagate = false;
            break;
         case KeyEvent.KEYCODE_A:
            setTrafficOverlay(!this.mMapView.isTraffic());
            propagate = false;
            break;
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
   /** Own methods              **/ 
   /******************************/
   
   private void setTrafficOverlay(boolean b)
   {
      SharedPreferences sharedPreferences = mHelper.getPreferences();
      Editor editor = sharedPreferences.edit();
      editor.putBoolean(Constants.TRAFFIC, b);
      editor.commit();
   }

   private void setSatelliteOverlay(boolean b)
   {
      SharedPreferences sharedPreferences = mHelper.getPreferences();
      Editor editor = sharedPreferences.edit();
      editor.putBoolean(Constants.SATELLITE, b);
      editor.commit();
   }
   
   @Override
   protected boolean isRouteDisplayed()
   {
      return true;
   }
   
   @Override
   protected boolean isLocationDisplayed()
   {
      SharedPreferences sharedPreferences = mHelper.getPreferences();
      return sharedPreferences.getBoolean(Constants.LOCATION, false) || mHelper.isLogging();
   }

   /******************************/
   /** Loggermap methods        **/ 
   /******************************/
   
   public void updateOverlays()
   {
      SharedPreferences sharedPreferences = mHelper.getPreferences();
      GoogleLoggerMap.this.mMapView.setSatellite(sharedPreferences.getBoolean(Constants.SATELLITE, false));
      GoogleLoggerMap.this.mMapView.setTraffic(sharedPreferences.getBoolean(Constants.TRAFFIC, false));
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
      switch (checkedId)
      {
         case R.id.layer_google_satellite:
            setSatelliteOverlay(true);
            break;
         case R.id.layer_google_regular:
            setSatelliteOverlay(false);
            break;
         case R.id.layer_traffic:
            setTrafficOverlay(isChecked);
            break;
         default:
            break;
      }
   }

   public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
   {
      if (key.equals(Constants.TRAFFIC))
      {
         updateOverlays();
      }
      else if (key.equals(Constants.SATELLITE))
      {
         updateOverlays();
      }
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
      return LoggerMapHelper.GOOGLE_PROVIDER;
   }

   public boolean isOutsideScreen(GeoPoint lastPoint)
   {
      Point out = new Point();
      this.mMapView.getProjection().toPixels(lastPoint, out);
      int height = this.mMapView.getHeight();
      int width = this.mMapView.getWidth();
      return (out.x < 0 || out.y < 0 || out.y > height || out.x > width);
   }

   public boolean isNearScreenEdge(GeoPoint lastPoint)
   {
      Point out = new Point();
      this.mMapView.getProjection().toPixels(lastPoint, out);
      int height = this.mMapView.getHeight();
      int width = this.mMapView.getWidth();
      return (out.x < width / 4 || out.y < height / 4 || out.x > (width / 4) * 3 || out.y > (height / 4) * 3);
   }

   public void executePostponedActions()
   {
      // NOOP for Google Maps
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
      mMapView.getController().animateTo(storedPoint);
   }

   public int getZoomLevel()
   {
      return mMapView.getZoomLevel();
   }

   public GeoPoint getMapCenter()
   {
      return mMapView.getMapCenter();
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

   public void clearAnimation()
   {
      mMapView.clearAnimation();
   }

   public void setCenter(GeoPoint lastPoint)
   {
      mMapView.getController().setCenter(lastPoint);
   }

   public int getMaxZoomLevel()
   {
      return mMapView.getMaxZoomLevel();
   }

   public GeoPoint fromPixels(int x, int y)
   {
      return mMapView.getProjection().fromPixels(x, y);
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
      mMapView.getProjection().toPixels(geoPoint, screenPoint);
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

   public void addOverlay(OverlayProvider overlay)
   {
      mMapView.getOverlays().add(overlay.getGoogleOverlay());
   }

   public void clearOverlays()
   { 
      mMapView.getOverlays().clear();
   }
   
   public SlidingIndicatorView getScaleIndicatorView()
   {
      return (SlidingIndicatorView) findViewById(R.id.scaleindicator);
   }
}