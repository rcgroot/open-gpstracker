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

import nl.sogeti.android.gpstracker.R;
import nl.sogeti.android.gpstracker.util.Constants;
import nl.sogeti.android.gpstracker.util.SlidingIndicatorView;
import nl.sogeti.android.gpstracker.viewer.map.overlay.OverlayProvider;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.tileprovider.util.CloudmadeUtil;
import org.osmdroid.views.MapView;
import org.osmdroid.views.MapView.Projection;
import org.osmdroid.views.overlay.MyLocationOverlay;
import org.osmdroid.views.overlay.Overlay;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.google.android.maps.GeoPoint;

/**
 * ????
 *
 * @version $Id:$
 * @author rene (c) Feb 26, 2012, Sogeti B.V.
 */
public class OsmLoggerMap extends Activity implements LoggerMap
{
   protected static final String TAG = "OsmLoggerMap";
   LoggerMapHelper mHelper;
   private MapView mMapView;
   private TextView[] mSpeedtexts;
   private TextView mLastGPSSpeedView;
   private TextView mLastGPSAltitudeView;
   private TextView mDistanceView;
   private MyLocationOverlay mMylocation;
   private Projection mProjecton;
   
   /**
    * Called when the activity is first created.
    */
   @Override
   protected void onCreate(Bundle load)
   {
      super.onCreate(load);
      setContentView(R.layout.map_osm);
      
      mMapView = (MapView) findViewById(R.id.myMapView);
      TextView[] speeds = { (TextView) findViewById(R.id.speedview05), (TextView) findViewById(R.id.speedview04), (TextView) findViewById(R.id.speedview03),
            (TextView) findViewById(R.id.speedview02), (TextView) findViewById(R.id.speedview01), (TextView) findViewById(R.id.speedview00) };
      mSpeedtexts = speeds;
      mLastGPSSpeedView = (TextView) findViewById(R.id.currentSpeed);
      mLastGPSAltitudeView = (TextView) findViewById(R.id.currentAltitude);
      mDistanceView = (TextView) findViewById(R.id.currentDistance);

      mHelper = new LoggerMapHelper(this);
      mMapView.setBuiltInZoomControls(true);
      mProjecton = mMapView.getProjection();
      mHelper.onCreate(load);
      
      mMylocation = new MyLocationOverlay(this, mMapView);
      mMapView.getOverlays().add( new Overlay(this)
      {
         
         @Override
         protected void draw(Canvas arg0, MapView map, boolean arg2)
         {
            Projection projecton = map.getProjection();
            mProjecton = projecton;
            IGeoPoint gepoint = map.getMapCenter();
            Point point = projecton.toPixels(gepoint, null);
            Log.d(TAG, "Found center ("+gepoint.getLatitudeE6()+","+gepoint.getLongitudeE6()+") matching screen point ("+point.x+","+point.y+") ");
         }
      } );
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
   
   @Override
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
   
   @Override
   public void setDrawingCacheEnabled(boolean b)
   {
      findViewById(R.id.mapScreen).setDrawingCacheEnabled(true);
   }
   
   @Override
   public Activity getActivity()
   {
      return this;
   }

   @Override
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

   @Override
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

   @Override
   public Bitmap getDrawingCache()
   {
      return findViewById(R.id.mapScreen).getDrawingCache();
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
      return LoggerMapHelper.GOOGLE_PROVIDER;
   }

   @Override
   public boolean isOutsideScreen(GeoPoint lastPoint)
   {
      Point out = new Point();
      mProjecton.toMapPixels(convertGeoPoint(lastPoint), out);
      int height = this.mMapView.getHeight();
      int width = this.mMapView.getWidth();
      return (out.x < 0 || out.y < 0 || out.y > height || out.x > width);
   }

   @Override
   public boolean isNearScreenEdge(GeoPoint lastPoint)
   {
      Point out = new Point();
      mProjecton.toMapPixels(convertGeoPoint(lastPoint), out);
      int height = this.mMapView.getHeight();
      int width = this.mMapView.getWidth();
      return (out.x < width / 4 || out.y < height / 4 || out.x > (width / 4) * 3 || out.y > (height / 4) * 3);
   }

   @Override
   public void executePostponedActions()
   {
      // NOOP for Google Maps
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
      return convertOSMGeoPoint(mMapView.getMapCenter());
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
      mMapView.getOverlays().add(overlay.getOSMOverlay());
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
      IGeoPoint osmGeopoint = mProjecton.fromPixels(x, y);
      GeoPoint geopoint = convertOSMGeoPoint(osmGeopoint);
      return geopoint;
   }
   
   @Override
   public void toPixels(GeoPoint geoPoint, Point screenPoint)
   {
      org.osmdroid.util.GeoPoint localGeopoint = convertGeoPoint(geoPoint);
      mProjecton.toMapPixels( localGeopoint, screenPoint);
   }

   @Override
   public boolean hasProjection()
   {
      return mProjecton != null;
   }

   @Override
   public float metersToEquatorPixels(float float1)
   {
      return mProjecton.metersToEquatorPixels(float1);
   }

   @Override
   public TextView[] getSpeedTextViews()
   {
      return mSpeedtexts;
   }

   @Override
   public TextView getAltitideTextView()
   {
      return mLastGPSAltitudeView;
   }

   @Override
   public TextView getSpeedTextView()
   {
      return mLastGPSSpeedView;
   }

   @Override
   public TextView getDistanceTextView()
   {
      return mDistanceView;
   }
   
   static org.osmdroid.util.GeoPoint convertGeoPoint( GeoPoint point )
   {
      org.osmdroid.util.GeoPoint geopoint = new org.osmdroid.util.GeoPoint(point.getLatitudeE6(), point.getLongitudeE6());
      return geopoint;
   }
   
   static GeoPoint convertOSMGeoPoint( IGeoPoint point )
   {
      return new GeoPoint(point.getLatitudeE6(), point.getLongitudeE6() );
   }

   @Override
   public void clearOverlays()
   {
      mMapView.getOverlayManager().clear();
   }

   @Override
   public SlidingIndicatorView getScaleIndicatorView()
   {
      return (SlidingIndicatorView) findViewById(R.id.scaleindicator);
   }
}
