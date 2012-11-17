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
package nl.sogeti.android.gpstracker.fragment;

import nl.sogeti.android.gpstracker.util.SlidingIndicatorView;
import nl.sogeti.android.gpstracker.viewer.map.overlay.OverlayProvider;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.view.View;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.google.android.maps.GeoPoint;

/**
 * ????
 *
 * @version $Id$
 * @author rene (c) Feb 26, 2012, Sogeti B.V.
 */
public interface LoggerMapFragmentInterface
{

   void setDrawingCacheEnabled(boolean b);

   void updateOverlays();

   void onLayerCheckedChanged(int checkedId, boolean b);

   void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key);

   Bitmap getDrawingCache();

   void showMediaDialog(BaseAdapter mediaAdapter);

   String getDataSourceId();

   boolean isOutsideScreen(GeoPoint lastPoint);

   boolean isNearScreenEdge(GeoPoint lastPoint);

   void executePostponedActions();

   void disableMyLocation();

   void disableCompass();

   void setZoom(int int1);

   void animateTo(GeoPoint storedPoint);

   int getZoomLevel();

   GeoPoint getMapCenter();

   boolean zoomOut();

   boolean zoomIn();

   void postInvalidate();

   void enableCompass();

   void enableMyLocation();

   void addOverlay(OverlayProvider overlay);

   void clearAnimation();

   void setCenter(GeoPoint lastPoint);

   int getMaxZoomLevel();

   GeoPoint fromPixels(int x, int y);

   boolean hasProjection();

   float metersToEquatorPixels(float float1);

   void toPixels(GeoPoint geopoint, Point screenPoint);

   TextView[] getSpeedTextViews();

   TextView getAltitideTextView();

   TextView getSpeedTextView();

   TextView getDistanceTextView();

   void clearOverlays();

   SlidingIndicatorView getScaleIndicatorView();

   Activity getActivity();

   View getSpeedbar();
}
