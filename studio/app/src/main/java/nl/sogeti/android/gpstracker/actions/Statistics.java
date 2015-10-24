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
package nl.sogeti.android.gpstracker.actions;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.ContextMenu;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ViewFlipper;

import nl.sogeti.android.gpstracker.R;
import nl.sogeti.android.gpstracker.actions.utils.GraphCanvas;
import nl.sogeti.android.gpstracker.actions.utils.StatisticsCalulator;
import nl.sogeti.android.gpstracker.actions.utils.StatisticsDelegate;
import nl.sogeti.android.gpstracker.db.GPStracking.Tracks;
import nl.sogeti.android.gpstracker.util.UnitsI18n;
import nl.sogeti.android.gpstracker.viewer.TrackList;

/**
 * Display some calulations based on a track
 *
 * @author rene (c) Oct 19, 2009, Sogeti B.V.
 * @version $Id$
 */
public class Statistics extends Activity implements StatisticsDelegate
{

   private static final int DIALOG_GRAPHTYPE = 3;
   private static final int MENU_GRAPHTYPE = 11;
   private static final int MENU_TRACKLIST = 12;
   private static final int MENU_SHARE = 41;
   private static final String TRACKURI = "TRACKURI";
   private static final String TAG = "OGT.Statistics";

   private static final int SWIPE_MIN_DISTANCE = 120;
   private static final int SWIPE_MAX_OFF_PATH = 250;
   private static final int SWIPE_THRESHOLD_VELOCITY = 200;

   private Uri mTrackUri = null;
   private boolean calculating;
   private TextView overallavgSpeedView;
   private TextView avgSpeedView;
   private TextView distanceView;
   private TextView endtimeView;
   private TextView starttimeView;
   private TextView maxSpeedView;
   private TextView waypointsView;
   private TextView mAscensionView;
   private TextView mElapsedTimeView;

   private UnitsI18n mUnits;
   private final ContentObserver mTrackObserver = new ContentObserver(new Handler())
   {

      @Override
      public void onChange(boolean selfUpdate)
      {
         if (!calculating)
         {
            Statistics.this.drawTrackingStatistics();
         }
      }
   };
   private GraphCanvas mGraphTimeSpeed;
   private ViewFlipper mViewFlipper;
   private Animation mSlideLeftIn;
   private Animation mSlideLeftOut;
   private Animation mSlideRightIn;
   private Animation mSlideRightOut;
   private GestureDetector mGestureDetector;
   private GraphCanvas mGraphDistanceSpeed;
   private GraphCanvas mGraphTimeAltitude;
   private GraphCanvas mGraphDistanceAltitude;
   private OnClickListener mGraphControlListener = new View.OnClickListener()
   {
      @Override
      public void onClick(View v)
      {
         int id = v.getId();
         switch (id)
         {
            case R.id.graphtype_timespeed:
               mViewFlipper.setDisplayedChild(0);
               break;
            case R.id.graphtype_distancespeed:
               mViewFlipper.setDisplayedChild(1);
               break;
            case R.id.graphtype_timealtitude:
               mViewFlipper.setDisplayedChild(2);
               break;
            case R.id.graphtype_distancealtitude:
               mViewFlipper.setDisplayedChild(3);
               break;
            default:
               break;
         }
         dismissDialog(DIALOG_GRAPHTYPE);
      }
   };

   /**
    * Called when the activity is first created.
    */
   @Override
   protected void onCreate(Bundle load)
   {
      super.onCreate(load);
      mUnits = new UnitsI18n(this, new UnitsI18n.UnitsChangeListener()
      {
         @Override
         public void onUnitsChange()
         {
            drawTrackingStatistics();
         }
      });
      setContentView(R.layout.statistics);

      mViewFlipper = (ViewFlipper) findViewById(R.id.flipper);
      mViewFlipper.setDrawingCacheEnabled(true);
      mSlideLeftIn = AnimationUtils.loadAnimation(this, R.anim.slide_left_in);
      mSlideLeftOut = AnimationUtils.loadAnimation(this, R.anim.slide_left_out);
      mSlideRightIn = AnimationUtils.loadAnimation(this, R.anim.slide_right_in);
      mSlideRightOut = AnimationUtils.loadAnimation(this, R.anim.slide_right_out);

      mGraphTimeSpeed = (GraphCanvas) mViewFlipper.getChildAt(0);
      mGraphDistanceSpeed = (GraphCanvas) mViewFlipper.getChildAt(1);
      mGraphTimeAltitude = (GraphCanvas) mViewFlipper.getChildAt(2);
      mGraphDistanceAltitude = (GraphCanvas) mViewFlipper.getChildAt(3);

      mGraphTimeSpeed.setType(GraphCanvas.TIMESPEEDGRAPH);
      mGraphDistanceSpeed.setType(GraphCanvas.DISTANCESPEEDGRAPH);
      mGraphTimeAltitude.setType(GraphCanvas.TIMEALTITUDEGRAPH);
      mGraphDistanceAltitude.setType(GraphCanvas.DISTANCEALTITUDEGRAPH);

      mGestureDetector = new GestureDetector(new MyGestureDetector());

      maxSpeedView = (TextView) findViewById(R.id.stat_maximumspeed);
      mAscensionView = (TextView) findViewById(R.id.stat_ascension);
      mElapsedTimeView = (TextView) findViewById(R.id.stat_elapsedtime);
      overallavgSpeedView = (TextView) findViewById(R.id.stat_overallaveragespeed);
      avgSpeedView = (TextView) findViewById(R.id.stat_averagespeed);
      distanceView = (TextView) findViewById(R.id.stat_distance);
      starttimeView = (TextView) findViewById(R.id.stat_starttime);
      endtimeView = (TextView) findViewById(R.id.stat_endtime);
      waypointsView = (TextView) findViewById(R.id.stat_waypoints);

      if (load != null && load.containsKey(TRACKURI))
      {
         mTrackUri = Uri.withAppendedPath(Tracks.CONTENT_URI, load.getString(TRACKURI));
      }
      else
      {
         mTrackUri = this.getIntent().getData();
      }
   }

   @Override
   protected void onRestoreInstanceState(Bundle load)
   {
      if (load != null)
      {
         super.onRestoreInstanceState(load);
      }
      if (load != null && load.containsKey(TRACKURI))
      {
         mTrackUri = Uri.withAppendedPath(Tracks.CONTENT_URI, load.getString(TRACKURI));
      }
      if (load != null && load.containsKey("FLIP"))
      {
         mViewFlipper.setDisplayedChild(load.getInt("FLIP"));
      }
   }

   /*
    * (non-Javadoc)
    * @see android.app.Activity#onResume()
    */
   @Override
   protected void onResume()
   {
      super.onResume();
      drawTrackingStatistics();

      ContentResolver resolver = this.getContentResolver();
      resolver.registerContentObserver(mTrackUri, true, this.mTrackObserver);
   }

   @Override
   protected void onSaveInstanceState(Bundle save)
   {
      super.onSaveInstanceState(save);
      save.putString(TRACKURI, mTrackUri.getLastPathSegment());
      save.putInt("FLIP", mViewFlipper.getDisplayedChild());
   }

   /*
    * (non-Javadoc)
    * @see android.app.Activity#onPause()
    */
   @Override
   protected void onPause()
   {
      super.onPause();
      mViewFlipper.stopFlipping();
      mGraphTimeSpeed.clearData();
      mGraphDistanceSpeed.clearData();
      mGraphTimeAltitude.clearData();
      mGraphDistanceAltitude.clearData();
      ContentResolver resolver = this.getContentResolver();
      resolver.unregisterContentObserver(this.mTrackObserver);
   }

   @Override
   public boolean onTouchEvent(MotionEvent event)
   {
      if (mGestureDetector.onTouchEvent(event))
      {
         return true;
      }
      else
      {
         return false;
      }
   }

   @Override
   public boolean onCreateOptionsMenu(Menu menu)
   {
      boolean result = super.onCreateOptionsMenu(menu);
      menu.add(ContextMenu.NONE, MENU_GRAPHTYPE, ContextMenu.NONE, R.string.menu_graphtype).setIcon(R.drawable
            .ic_menu_picture).setAlphabeticShortcut('t');
      menu.add(ContextMenu.NONE, MENU_TRACKLIST, ContextMenu.NONE, R.string.menu_tracklist).setIcon(R.drawable
            .ic_menu_show_list).setAlphabeticShortcut('l');
      menu.add(ContextMenu.NONE, MENU_SHARE, ContextMenu.NONE, R.string.menu_shareTrack).setIcon(R.drawable
            .ic_menu_share).setAlphabeticShortcut('s');
      return result;
   }

   @Override
   public boolean onOptionsItemSelected(MenuItem item)
   {
      boolean handled = false;
      Intent intent;
      switch (item.getItemId())
      {
         case MENU_GRAPHTYPE:
            showDialog(DIALOG_GRAPHTYPE);
            handled = true;
            break;
         case MENU_TRACKLIST:
            intent = new Intent(this, TrackList.class);
            intent.putExtra(Tracks._ID, mTrackUri.getLastPathSegment());
            startActivityForResult(intent, MENU_TRACKLIST);
            break;
         case MENU_SHARE:
            intent = new Intent(Intent.ACTION_RUN);
            intent.setDataAndType(mTrackUri, Tracks.CONTENT_ITEM_TYPE);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Bitmap bm = mViewFlipper.getDrawingCache();
            Uri screenStreamUri = ShareTrack.storeScreenBitmap(bm);
            intent.putExtra(Intent.EXTRA_STREAM, screenStreamUri);
            startActivityForResult(Intent.createChooser(intent, getString(R.string.share_track)), MENU_SHARE);
            handled = true;
            break;
         default:
            handled = super.onOptionsItemSelected(item);
      }
      return handled;
   }

   /*
    * (non-Javadoc)
    * @see android.app.Activity#onCreateDialog(int)
    */
   @Override
   protected Dialog onCreateDialog(int id)
   {
      Dialog dialog = null;
      LayoutInflater factory = null;
      View view = null;
      Builder builder = null;
      switch (id)
      {
         case DIALOG_GRAPHTYPE:
            builder = new AlertDialog.Builder(this);
            factory = LayoutInflater.from(this);
            view = factory.inflate(R.layout.graphtype, null);
            builder.setTitle(R.string.dialog_graphtype_title).setIcon(android.R.drawable.ic_dialog_alert)
                   .setNegativeButton(R.string.btn_cancel, null).setView(view);
            dialog = builder.create();
            return dialog;
         default:
            return super.onCreateDialog(id);
      }
   }

   /*
    * (non-Javadoc)
    * @see android.app.Activity#onPrepareDialog(int, android.app.Dialog)
    */
   @Override
   protected void onPrepareDialog(int id, Dialog dialog)
   {
      switch (id)
      {
         case DIALOG_GRAPHTYPE:
            Button speedtime = (Button) dialog.findViewById(R.id.graphtype_timespeed);
            Button speeddistance = (Button) dialog.findViewById(R.id.graphtype_distancespeed);
            Button altitudetime = (Button) dialog.findViewById(R.id.graphtype_timealtitude);
            Button altitudedistance = (Button) dialog.findViewById(R.id.graphtype_distancealtitude);
            speedtime.setOnClickListener(mGraphControlListener);
            speeddistance.setOnClickListener(mGraphControlListener);
            altitudetime.setOnClickListener(mGraphControlListener);
            altitudedistance.setOnClickListener(mGraphControlListener);
         default:
            break;
      }
      super.onPrepareDialog(id, dialog);
   }

   /*
    * (non-Javadoc)
    * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
    */
   @Override
   protected void onActivityResult(int requestCode, int resultCode, Intent intent)
   {
      super.onActivityResult(requestCode, resultCode, intent);
      switch (requestCode)
      {
         case MENU_TRACKLIST:
            if (resultCode == RESULT_OK)
            {
               mTrackUri = intent.getData();
               drawTrackingStatistics();
            }
            break;
         case MENU_SHARE:
            ShareTrack.clearScreenBitmap();
            break;
         default:
            Log.w(TAG, "Unknown activity result request code");
      }
   }

   private void drawTrackingStatistics()
   {
      calculating = true;
      StatisticsCalulator calculator = new StatisticsCalulator(this, mUnits, this);
      calculator.execute(mTrackUri);
   }

   @Override
   public void finishedCalculations(StatisticsCalulator calculated)
   {
      mGraphTimeSpeed.setData(mTrackUri, calculated);
      mGraphDistanceSpeed.setData(mTrackUri, calculated);
      mGraphTimeAltitude.setData(mTrackUri, calculated);
      mGraphDistanceAltitude.setData(mTrackUri, calculated);

      mViewFlipper.postInvalidate();

      maxSpeedView.setText(calculated.getMaxSpeedText());
      mElapsedTimeView.setText(calculated.getDurationText());
      mAscensionView.setText(calculated.getAscensionText());
      overallavgSpeedView.setText(calculated.getOverallavgSpeedText());
      avgSpeedView.setText(calculated.getAvgSpeedText());
      distanceView.setText(calculated.getDistanceText());
      starttimeView.setText(Long.toString(calculated.getStarttime()));
      endtimeView.setText(Long.toString(calculated.getEndtime()));
      String titleFormat = getString(R.string.stat_title);
      setTitle(String.format(titleFormat, calculated.getTracknameText()));
      waypointsView.setText(calculated.getWaypointsText());

      calculating = false;
   }

   class MyGestureDetector extends SimpleOnGestureListener
   {
      @Override
      public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY)
      {
         if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH)
         {
            return false;
         }
         // right to left swipe
         if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY)
         {
            mViewFlipper.setInAnimation(mSlideLeftIn);
            mViewFlipper.setOutAnimation(mSlideLeftOut);
            mViewFlipper.showNext();
         }
         else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY)
         {
            mViewFlipper.setInAnimation(mSlideRightIn);
            mViewFlipper.setOutAnimation(mSlideRightOut);
            mViewFlipper.showPrevious();
         }
         return false;
      }
   }
}
