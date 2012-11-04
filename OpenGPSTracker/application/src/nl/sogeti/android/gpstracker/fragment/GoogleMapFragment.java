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
package nl.sogeti.android.gpstracker.fragment;

import nl.sogeti.android.gpstracker.R;
import nl.sogeti.android.gpstracker.googlemapfragment.ActivityHostFragment;
import nl.sogeti.android.gpstracker.googlemapfragment.FixedMyLocationOverlay;
import nl.sogeti.android.gpstracker.googlemapfragment.MyGoogleMapActivity;
import nl.sogeti.android.gpstracker.util.Constants;
import nl.sogeti.android.gpstracker.util.SlidingIndicatorView;
import nl.sogeti.android.gpstracker.viewer.map.LoggerMapHelper;
import nl.sogeti.android.gpstracker.viewer.map.overlay.OverlayProvider;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;

/**
 * Fragment showing a track and allowing logging control
 * 
 * @version $Id$
 * @author rene (c) Jan 18, 2009, Sogeti B.V.
 */
public class GoogleMapFragment extends ActivityHostFragment
{
   private MapView mMapView;
   private FixedMyLocationOverlay mMylocation;

   @Override
   protected Class< ? extends Activity> getActivityClass()
   {
      return MyGoogleMapActivity.class;
   }

   @Override
   public void onAttach(Activity activity)
   {
      super.onAttach(activity);
   }

   @Override
   public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
   {
      View v = super.onCreateView(inflater, container, savedInstanceState);
      mMapView = (MapView) getHostedActivity().findViewById(R.id.myMapView);
      mMapView.setBuiltInZoomControls(true);
      mMylocation = new FixedMyLocationOverlay(this.getActivity(), mMapView);
      super.didCreateView(v, savedInstanceState);
      
      return v;
   }

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
   /** Loggermap methods **/
   /******************************/

   @Override
   public void updateOverlays()
   {
      SharedPreferences sharedPreferences = mHelper.getPreferences();
      mMapView.setSatellite(sharedPreferences.getBoolean(Constants.SATELLITE, false));
      mMapView.setTraffic(sharedPreferences.getBoolean(Constants.TRAFFIC, false));
   }

   @Override
   public void setDrawingCacheEnabled(boolean b)
   {
      mMapView.getRootView().setDrawingCacheEnabled(true);
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
      return LoggerMapHelper.GOOGLE_PROVIDER;
   }

   @Override
   public boolean isOutsideScreen(GeoPoint lastPoint)
   {
      Point out = new Point();
      this.mMapView.getProjection().toPixels(lastPoint, out);
      int height = this.mMapView.getHeight();
      int width = this.mMapView.getWidth();
      return (out.x < 0 || out.y < 0 || out.y > height || out.x > width);
   }

   @Override
   public boolean isNearScreenEdge(GeoPoint lastPoint)
   {
      Point out = new Point();
      this.mMapView.getProjection().toPixels(lastPoint, out);
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
      mMapView.getController().animateTo(storedPoint);
   }

   @Override
   public int getZoomLevel()
   {
      return mMapView.getZoomLevel();
   }

   @Override
   public GeoPoint getMapCenter()
   {
      return mMapView.getMapCenter();
   }

   @Override
   public boolean zoomOut()
   {
      return mMapView.getController().zoomOut();
   }

   @Override
   public boolean zoomIn()
   {
      return mMapView.getController().zoomIn();
   }

   @Override
   public void postInvalidate()
   {
      mMapView.postInvalidate();
   }

   @Override
   public void clearAnimation()
   {
      mMapView.clearAnimation();
   }

   @Override
   public void setCenter(GeoPoint lastPoint)
   {
      mMapView.getController().setCenter(lastPoint);
   }

   @Override
   public int getMaxZoomLevel()
   {
      return mMapView.getMaxZoomLevel();
   }

   @Override
   public GeoPoint fromPixels(int x, int y)
   {
      return mMapView.getProjection().fromPixels(x, y);
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
      mMapView.getProjection().toPixels(geoPoint, screenPoint);
   }

   @Override
   public void addOverlay(OverlayProvider overlay)
   {
      mMapView.getOverlays().add(overlay.getGoogleOverlay());
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
