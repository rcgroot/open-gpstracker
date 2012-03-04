package nl.sogeti.android.gpstracker.viewer.map;

import java.util.LinkedList;
import java.util.List;

import android.graphics.Canvas;
import android.os.Handler;

import com.google.android.maps.GeoPoint;

public class BitmapSegmentsOverlay extends AsyncOverlay
{
   List<SegmentOverlay> mOverlays;
   Handler mOverlayHandler;
   
   BitmapSegmentsOverlay(LoggerMap loggermap, Handler handler)
   {
      super(loggermap, handler);
      mOverlays = new LinkedList<SegmentOverlay>();
      mOverlayHandler = handler;
   }

   @Override
   synchronized protected void redrawOffscreen(Canvas asyncBuffer, LoggerMap loggermap)
   {
      for( SegmentOverlay segment : mOverlays)
      {
         segment.draw(asyncBuffer);
      }
   }
   
   @Override 
   synchronized protected void scheduleRecalculation()
   {
      for( SegmentOverlay segment : mOverlays)
      {
         segment.calculateMedia();
         segment.calculateTrack(); 
      }
   }

   @Override
   synchronized protected boolean commonOnTap(GeoPoint tappedGeoPoint)
   {
      boolean handled = false;
      for( SegmentOverlay segment : mOverlays)
      {
         if( !handled )
         {
            handled = segment.commonOnTap(tappedGeoPoint);
         }
      }
      return handled;
   }

   synchronized public void addSegment(SegmentOverlay segment)
   {
      segment.setBitmapHolder(this);
      mOverlays.add(segment);
   }
   
   synchronized public void clearSegments()
   {
      for( SegmentOverlay segment : mOverlays)
      {
         segment.closeResources();
      }
      mOverlays.clear();
   }

   synchronized public void setTrackColoringMethod(int color, double speed)
   {
      for( SegmentOverlay segment : mOverlays)
      {
         segment.setTrackColoringMethod(color, speed);
      }
   }

   public int size()
   {
      return mOverlays.size();
   }
}
