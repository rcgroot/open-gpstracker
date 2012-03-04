/*------------------------------------------------------------------------------
 **     Ident: Delivery Center Java
 **    Author: rene
 ** Copyright: (c) Feb 26, 2012 Sogeti Nederland B.V. All Rights Reserved.
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

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.tileprovider.util.CloudmadeUtil;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MyLocationOverlay;
import org.osmdroid.views.overlay.Overlay;

import com.google.android.maps.GeoPoint;

import nl.sogeti.android.gpstracker.R;
import nl.sogeti.android.gpstracker.util.Constants;
import nl.sogeti.android.gpstracker.viewer.map.SegmentOverlay.SegmentOsmOverlay;

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

/**
 * ????
 *
 * @version $Id:$
 * @author rene (c) Feb 26, 2012, Sogeti B.V.
 */
public class OsmLoggerMap extends Activity implements LoggerMap
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
      setContentView(R.layout.osmmap);
      
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

   /******************************/
   /** Loggermap methods        **/ 
   /******************************/
   
   public void updateOverlays()
   {
      SharedPreferences sharedPreferences = mHelper.getPreferences();
      int renderer = sharedPreferences.getInt(Constants.OSMBASEOVERLAY, 0);
      switch( renderer )
      {
         case Constants.OSM_CLOUDMADE:
            CloudmadeUtil.retrieveCloudmadeKey(this.getApplicationContext());
            mMapView.setTileSource(TileSourceFactory.CLOUDMADESTANDARDTILES);
            break;
         case Constants.OSM_MAKNIK:
            mMapView.setTileSource(TileSourceFactory.MAPNIK);
            break;
         case Constants.OSM_CYCLE:
            mMapView.setTileSource(TileSourceFactory.CYCLEMAP);
            break;
         default:
            break;
      }
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
      this.mMapView.getProjection().toPixels(convertGeoPoint(lastPoint), out);
      int height = this.mMapView.getHeight();
      int width = this.mMapView.getWidth();
      return (out.x < 0 || out.y < 0 || out.y > height || out.x > width);
   }

   public boolean isNearScreenEdge(GeoPoint lastPoint)
   {
      Point out = new Point();
      this.mMapView.getProjection().toPixels(convertGeoPoint(lastPoint), out);
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
      mMapView.getController().animateTo(convertGeoPoint(storedPoint));
   }

   public int getZoomLevel()
   {
      return mMapView.getZoomLevel();
   }

   public GeoPoint getMapCenter()
   {
      return convertOSMGeoPoint(mMapView.getMapCenter());
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

   public List<SegmentOverlay> getSegmentOverlays()
   {
      List<SegmentOverlay> segments = new LinkedList<SegmentOverlay>();
      for( Overlay overlay : mMapView.getOverlays() )
      {
         if( overlay instanceof SegmentOverlay.SegmentOsmOverlay )
         {
            SegmentOverlay.SegmentOsmOverlay segmentOverlay = (SegmentOsmOverlay) overlay;
            segments.add(segmentOverlay.getSegmentOverlay());
         }
      }
      return segments;
   }

   public void addOverlay(OverlayProvider overlay)
   {
      mMapView.getOverlays().add(overlay.getOSMOverlay());
   }

   public void clearAnimation()
   {
      mMapView.clearAnimation();
   }

   public void setCenter(GeoPoint lastPoint)
   {
      mMapView.getController().setCenter( convertGeoPoint(lastPoint));
   }

   public int getMaxZoomLevel()
   {
      return mMapView.getMaxZoomLevel();
   }

   public GeoPoint fromPixels(int x, int y)
   {
      return convertOSMGeoPoint(mMapView.getProjection().fromPixels(x, y));
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
      mMapView.getProjection().toPixels( convertGeoPoint(geoPoint), screenPoint);
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
   
   static org.osmdroid.util.GeoPoint convertGeoPoint( GeoPoint point )
   {
      return new org.osmdroid.util.GeoPoint(point.getLatitudeE6(), point.getLongitudeE6() );
   }
   
   static GeoPoint convertOSMGeoPoint( IGeoPoint point )
   {
      return new GeoPoint(point.getLatitudeE6(), point.getLongitudeE6() );
   }

   public void clearOverlays()
   {
      mMapView.getOverlayManager().clear();
   }
}
