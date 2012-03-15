package nl.sogeti.android.gpstracker.viewer.map;

import java.util.LinkedList;
import java.util.List;

import android.graphics.Canvas;
import android.os.Handler;
import android.util.Log;

import com.google.android.maps.GeoPoint;

public class BitmapSegmentsOverlay extends AsyncOverlay
{
   private static final String TAG = "GG.BitmapSegmentsOverlay";
   
   List<SegmentRendering> mOverlays;
   Handler mOverlayHandler;
   
   BitmapSegmentsOverlay(LoggerMap loggermap, Handler handler)
   {
      super(loggermap, handler);
      mOverlays = new LinkedList<SegmentRendering>();
      mOverlayHandler = handler;
   }

   @Override
   synchronized protected void redrawOffscreen(Canvas asyncBuffer, LoggerMap loggermap)
   {
      for( SegmentRendering segment : mOverlays)
      {
         segment.draw(asyncBuffer);
      }
   }
   
   @Override 
   synchronized protected void scheduleRecalculation()
   {
      Log.d( TAG, "scheduleRecalculation()");
      for( SegmentRendering segment : mOverlays)
      {
         segment.calculateMedia();
         segment.calculateTrack(); 
      }
   }

   @Override
   synchronized protected boolean commonOnTap(GeoPoint tappedGeoPoint)
   {
      boolean handled = false;
      for( SegmentRendering segment : mOverlays)
      {
         if( !handled )
         {
            handled = segment.commonOnTap(tappedGeoPoint);
         }
      }
      return handled;
   }

   synchronized public void addSegment(SegmentRendering segment)
   {
      Log.d( TAG, "addSegment(SegmentRendering segment)");
      segment.setBitmapHolder(this);
      mOverlays.add(segment);
   }
   
   synchronized public void clearSegments()
   {
      Log.d( TAG, "clearSegments()");
      for( SegmentRendering segment : mOverlays)
      {
         segment.closeResources();
      }
      mOverlays.clear();
      reset();
   }

   synchronized public void setTrackColoringMethod(int color, double speed, double height)
   {
      Log.d( TAG, "setTrackColoringMethod(int color, double speed, double height)");
      for( SegmentRendering segment : mOverlays)
      {
         segment.setTrackColoringMethod(color, speed, height);
      }
      scheduleRecalculation();
   }

   public int size()
   {
      return mOverlays.size();
   }
}
