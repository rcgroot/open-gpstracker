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
package nl.sogeti.android.gpstracker.actions.tasks;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import nl.sogeti.android.gpstracker.R;
import nl.sogeti.android.gpstracker.actions.utils.ProgressListener;
import nl.sogeti.android.gpstracker.db.GPStracking;
import nl.sogeti.android.gpstracker.db.GPStracking.Media;
import nl.sogeti.android.gpstracker.db.GPStracking.Segments;
import nl.sogeti.android.gpstracker.db.GPStracking.Tracks;
import nl.sogeti.android.gpstracker.db.GPStracking.Waypoints;
import nl.sogeti.android.gpstracker.util.Constants;

import org.xmlpull.v1.XmlSerializer;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore.MediaColumns;
import android.util.Log;
import android.util.Xml;

/**
 * Create a KMZ version of a stored track
 * 
 * @version $Id$
 * @author rene (c) Mar 22, 2009, Sogeti B.V.
 */
public class KmzCreator extends XmlCreator
{
   public static final String NS_SCHEMA = "http://www.w3.org/2001/XMLSchema-instance";
   public static final String NS_KML_22 = "http://www.opengis.net/kml/2.2";
   public static final SimpleDateFormat ZULU_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
   static
   {
      TimeZone utc = TimeZone.getTimeZone("UTC");
      ZULU_DATE_FORMAT.setTimeZone(utc); // ZULU_DATE_FORMAT format ends with Z for UTC so make that true
   }

   private String TAG = "OGT.KmzCreator";

   public KmzCreator(Context context, Uri trackUri, String chosenFileName, ProgressListener listener)
   {
      super(context, trackUri, chosenFileName, listener);
   }

   @Override
   protected Uri doInBackground(Void... params)
   {
      determineProgressGoal();

      Uri resultFilename = exportKml();
      return resultFilename;
   }

   private Uri exportKml()
   {
      if (mFileName.endsWith(".kmz") || mFileName.endsWith(".zip"))
      {
         setExportDirectoryPath(Constants.getSdCardDirectory(mContext) + mFileName.substring(0, mFileName.length() - 4));
      }
      else
      {
         setExportDirectoryPath(Constants.getSdCardDirectory(mContext) + mFileName);
      }

      new File(getExportDirectoryPath()).mkdirs();
      String xmlFilePath = getExportDirectoryPath() + "/doc.kml";

      String resultFilename = null;
      FileOutputStream fos = null;
      BufferedOutputStream buf = null;
      try
      {
         verifySdCardAvailibility();

         XmlSerializer serializer = Xml.newSerializer();
         File xmlFile = new File(xmlFilePath);
         fos = new FileOutputStream(xmlFile);
         buf = new BufferedOutputStream(fos, 8192);
         serializer.setOutput(buf, "UTF-8");

         serializeTrack(mTrackUri, mFileName, serializer);
         buf.close();
         buf = null;
         fos.close();
         fos = null;

         resultFilename = bundlingMediaAndXml(xmlFile.getParentFile().getName(), ".kmz");
         mFileName = new File(resultFilename).getName();
      }
      catch (IllegalArgumentException e)
      {
         String text = mContext.getString(R.string.ticker_failed) + " \"" + xmlFilePath + "\" " + mContext.getString(R.string.error_filename);
         handleError(mContext.getString(R.string.taskerror_kmz_write), e, text);
      }
      catch (IllegalStateException e)
      {
         String text = mContext.getString(R.string.ticker_failed) + " \"" + xmlFilePath + "\" " + mContext.getString(R.string.error_buildxml);
         handleError(mContext.getString(R.string.taskerror_kmz_write), e, text);
      }
      catch (IOException e)
      {
         String text = mContext.getString(R.string.ticker_failed) + " \"" + xmlFilePath + "\" " + mContext.getString(R.string.error_writesdcard);
         handleError(mContext.getString(R.string.taskerror_kmz_write), e, text);
      }
      finally
      {
         if (buf != null)
         {
            try
            {
               buf.close();
            }
            catch (IOException e)
            {
               Log.e(TAG, "Failed to close buf after completion, ignoring.", e);
            }
         }
         if (fos != null)
         {
            try
            {
               fos.close();
            }
            catch (IOException e)
            {
               Log.e(TAG, "Failed to close fos after completion, ignoring.", e);
            }
         }
      }
      return Uri.fromFile(new File(resultFilename));
   }

   private void serializeTrack(Uri trackUri, String trackName, XmlSerializer serializer) throws IOException
   {
      serializer.startDocument("UTF-8", true);
      serializer.setPrefix("xsi", NS_SCHEMA);
      serializer.setPrefix("kml", NS_KML_22);
      serializer.startTag("", "kml");
      serializer.attribute(NS_SCHEMA, "schemaLocation", NS_KML_22 + " http://schemas.opengis.net/kml/2.2.0/ogckml22.xsd");
      serializer.attribute(null, "xmlns", NS_KML_22);

      serializer.text("\n");
      serializer.startTag("", "Document");
      serializer.text("\n");
      serializer.startTag("", "name");
      serializer.text(trackName);
      serializer.endTag("", "name");

      /* from <name/> upto <Folder/> */
      serializeTrackHeader(serializer, trackUri);

      serializer.text("\n");
      serializer.endTag("", "Document");

      serializer.endTag("", "kml");
      serializer.endDocument();
   }

   private String serializeTrackHeader(XmlSerializer serializer, Uri trackUri) throws IOException
   {
      ContentResolver resolver = mContext.getContentResolver();
      Cursor trackCursor = null;
      String name = null;

      try
      {
         trackCursor = resolver.query(trackUri, new String[] { Tracks.NAME }, null, null, null);
         if (trackCursor.moveToFirst())
         {
            serializer.text("\n");
            serializer.startTag("", "Style");
            serializer.attribute(null, "id", "lineStyle");
            serializer.startTag("", "LineStyle");
            serializer.text("\n");
            serializer.startTag("", "color");
            serializer.text("99ffac59");
            serializer.endTag("", "color");
            serializer.text("\n");
            serializer.startTag("", "width");
            serializer.text("6");
            serializer.endTag("", "width");
            serializer.text("\n");
            serializer.endTag("", "LineStyle");
            serializer.text("\n");
            serializer.endTag("", "Style");
            serializer.text("\n");
            serializer.startTag("", "Folder");
            name = trackCursor.getString(0);
            serializer.text("\n");
            serializer.startTag("", "name");
            serializer.text(name);
            serializer.endTag("", "name");
            serializer.text("\n");
            serializer.startTag("", "open");
            serializer.text("1");
            serializer.endTag("", "open");
            serializer.text("\n");

            serializeSegments(serializer, Uri.withAppendedPath(trackUri, "segments"));

            serializer.text("\n");
            serializer.endTag("", "Folder");
         }
      }
      finally
      {
         if (trackCursor != null)
         {
            trackCursor.close();
         }
      }
      return name;
   }

   /**
    * <pre>
    * &lt;Folder>
    *    &lt;Placemark>
    *      serializeSegmentToTimespan()
    *      &lt;LineString>
    *         serializeWaypoints()
    *      &lt;/LineString>
    *    &lt;/Placemark>
    *    &lt;Placemark/>
    *    &lt;Placemark/>
    * &lt;/Folder>
    * </pre>
    * 
    * @param serializer
    * @param segments
    * @throws IOException
    */
   private void serializeSegments(XmlSerializer serializer, Uri segments) throws IOException
   {
      Cursor segmentCursor = null;
      ContentResolver resolver = mContext.getContentResolver();
      try
      {
         segmentCursor = resolver.query(segments, new String[] { Segments._ID }, null, null, null);
         if (segmentCursor.moveToFirst())
         {
            do
            {
               Uri waypoints = Uri.withAppendedPath(segments, segmentCursor.getLong(0) + "/waypoints");
               serializer.text("\n");
               serializer.startTag("", "Folder");
               serializer.text("\n");
               serializer.startTag("", "name");
               serializer.text(String.format("Segment %d", 1 + segmentCursor.getPosition()));
               serializer.endTag("", "name");
               serializer.text("\n");
               serializer.startTag("", "open");
               serializer.text("1");
               serializer.endTag("", "open");

               /* Single <TimeSpan/> element */
               serializeSegmentToTimespan(serializer, waypoints);

               serializer.text("\n");
               serializer.startTag("", "Placemark");
               serializer.text("\n");
               serializer.startTag("", "name");
               serializer.text("Path");
               serializer.endTag("", "name");
               serializer.text("\n");
               serializer.startTag("", "styleUrl");
               serializer.text("#lineStyle");
               serializer.endTag("", "styleUrl");
               serializer.text("\n");
               serializer.startTag("", "LineString");
               serializer.text("\n");
               serializer.startTag("", "tessellate");
               serializer.text("0");
               serializer.endTag("", "tessellate");
               serializer.text("\n");
               serializer.startTag("", "altitudeMode");
               serializer.text("clampToGround");
               serializer.endTag("", "altitudeMode");

               /* Single <coordinates/> element */
               serializeWaypoints(serializer, waypoints);

               serializer.text("\n");
               serializer.endTag("", "LineString");
               serializer.text("\n");
               serializer.endTag("", "Placemark");

               serializeWaypointDescription(serializer, Uri.withAppendedPath(segments, "/" + segmentCursor.getLong(0) + "/media"));

               serializer.text("\n");
               serializer.endTag("", "Folder");

            }
            while (segmentCursor.moveToNext());

         }
      }
      finally
      {
         if (segmentCursor != null)
         {
            segmentCursor.close();
         }
      }
   }

   /**
    * &lt;TimeSpan>&lt;begin>...&lt;/begin>&lt;end>...&lt;/end>&lt;/TimeSpan>
    * 
    * @param serializer
    * @param waypoints
    * @throws IOException
    */
   private void serializeSegmentToTimespan(XmlSerializer serializer, Uri waypoints) throws IOException
   {
      Cursor waypointsCursor = null;
      Date segmentStartTime = null;
      Date segmentEndTime = null;
      ContentResolver resolver = mContext.getContentResolver();
      try
      {
         waypointsCursor = resolver.query(waypoints, new String[] { Waypoints.TIME }, null, null, null);

         if (waypointsCursor.moveToFirst())
         {
            segmentStartTime = new Date(waypointsCursor.getLong(0));
            if (waypointsCursor.moveToLast())
            {
               segmentEndTime = new Date(waypointsCursor.getLong(0));

               serializer.text("\n");
               serializer.startTag("", "TimeSpan");
               serializer.text("\n");
               serializer.startTag("", "begin");
               serializer.text(ZULU_DATE_FORMAT.format(segmentStartTime));
               serializer.endTag("", "begin");
               serializer.text("\n");
               serializer.startTag("", "end");
               serializer.text(ZULU_DATE_FORMAT.format(segmentEndTime));
               serializer.endTag("", "end");
               serializer.text("\n");
               serializer.endTag("", "TimeSpan");
            }
         }
      }
      finally
      {
         if (waypointsCursor != null)
         {
            waypointsCursor.close();
         }
      }
   }

   /**
    * &lt;coordinates>...&lt;/coordinates>
    * 
    * @param serializer
    * @param waypoints
    * @throws IOException
    */
   private void serializeWaypoints(XmlSerializer serializer, Uri waypoints) throws IOException
   {
      Cursor waypointsCursor = null;
      ContentResolver resolver = mContext.getContentResolver();
      try
      {
         waypointsCursor = resolver.query(waypoints, new String[] { Waypoints.LONGITUDE, Waypoints.LATITUDE, Waypoints.ALTITUDE }, null, null, null);
         if (waypointsCursor.moveToFirst())
         {
            serializer.text("\n");
            serializer.startTag("", "coordinates");
            do
            {
               publishProgress(1);
               // Single Coordinate tuple
               serializeCoordinates(serializer, waypointsCursor);
               serializer.text(" ");
            }
            while (waypointsCursor.moveToNext());

            serializer.endTag("", "coordinates");
         }
      }
      finally
      {
         if (waypointsCursor != null)
         {
            waypointsCursor.close();
         }
      }

   }

   /**
    * lon,lat,alt tuple without trailing spaces
    * 
    * @param serializer
    * @param waypointsCursor
    * @throws IOException
    */
   private void serializeCoordinates(XmlSerializer serializer, Cursor waypointsCursor) throws IOException
   {
      serializer.text(Double.toString(waypointsCursor.getDouble(0)));
      serializer.text(",");
      serializer.text(Double.toString(waypointsCursor.getDouble(1)));
      serializer.text(",");
      serializer.text(Double.toString(waypointsCursor.getDouble(2)));
   }

   private void serializeWaypointDescription(XmlSerializer serializer, Uri media) throws IOException
   {
      String mediaPathPrefix = Constants.getSdCardDirectory(mContext);
      Cursor mediaCursor = null;
      ContentResolver resolver = mContext.getContentResolver();
      try
      {
         mediaCursor = resolver.query(media, new String[] { Media.URI, Media.TRACK, Media.SEGMENT, Media.WAYPOINT }, null, null, null);
         if (mediaCursor.moveToFirst())
         {
            do
            {
               Uri mediaUri = Uri.parse(mediaCursor.getString(0));
               Uri singleWaypointUri = Uri.withAppendedPath(Tracks.CONTENT_URI, mediaCursor.getLong(1) + "/segments/" + mediaCursor.getLong(2) + "/waypoints/"
                     + mediaCursor.getLong(3));
               String lastPathSegment = mediaUri.getLastPathSegment();
               if (mediaUri.getScheme().equals("file"))
               {
                  if (lastPathSegment.endsWith("3gp"))
                  {
                     String includedMediaFile = includeMediaFile(lastPathSegment);
                     serializer.text("\n");
                     serializer.startTag("", "Placemark");
                     serializer.text("\n");
                     quickTag(serializer, "", "name", lastPathSegment);
                     serializer.text("\n");
                     serializer.startTag("", "description");
                     String kmlAudioUnsupported = mContext.getString(R.string.kmlVideoUnsupported);
                     serializer.text(String.format(kmlAudioUnsupported, includedMediaFile));
                     serializer.endTag("", "description");
                     serializeMediaPoint(serializer, singleWaypointUri);
                     serializer.text("\n");
                     serializer.endTag("", "Placemark");
                  }
                  else if (lastPathSegment.endsWith("jpg"))
                  {
                     String includedMediaFile = includeMediaFile(mediaPathPrefix + lastPathSegment);
                     serializer.text("\n");
                     serializer.startTag("", "Placemark");
                     serializer.text("\n");
                     quickTag(serializer, "", "name", lastPathSegment);
                     serializer.text("\n");
                     quickTag(serializer, "", "description", "<img src=\"" + includedMediaFile + "\" width=\"500px\"/><br/>" + lastPathSegment);
                     serializer.text("\n");
                     serializeMediaPoint(serializer, singleWaypointUri);
                     serializer.text("\n");
                     serializer.endTag("", "Placemark");
                  }
                  else if (lastPathSegment.endsWith("txt"))
                  {
                     serializer.text("\n");
                     serializer.startTag("", "Placemark");
                     serializer.text("\n");
                     quickTag(serializer, "", "name", lastPathSegment);
                     serializer.text("\n");
                     serializer.startTag("", "description");
                     BufferedReader buf = new BufferedReader(new FileReader(mediaUri.getEncodedPath()));
                     String line;
                     while ((line = buf.readLine()) != null)
                     {
                        serializer.text(line);
                        serializer.text("\n");
                     }
                     serializer.endTag("", "description");
                     serializeMediaPoint(serializer, singleWaypointUri);
                     serializer.text("\n");
                     serializer.endTag("", "Placemark");
                  }
               }
               else if (mediaUri.getScheme().equals("content"))
               {
                  if (mediaUri.getAuthority().equals(GPStracking.AUTHORITY + ".string"))
                  {
                     serializer.text("\n");
                     serializer.startTag("", "Placemark");
                     serializer.text("\n");
                     quickTag(serializer, "", "name", lastPathSegment);
                     serializeMediaPoint(serializer, singleWaypointUri);
                     serializer.text("\n");
                     serializer.endTag("", "Placemark");
                  }
                  else if (mediaUri.getAuthority().equals("media"))
                  {
                     Cursor mediaItemCursor = null;
                     try
                     {
                        mediaItemCursor = resolver.query(mediaUri, new String[] { MediaColumns.DATA, MediaColumns.DISPLAY_NAME }, null, null, null);
                        if (mediaItemCursor.moveToFirst())
                        {
                           String includedMediaFile = includeMediaFile(mediaItemCursor.getString(0));
                           serializer.text("\n");
                           serializer.startTag("", "Placemark");
                           serializer.text("\n");
                           quickTag(serializer, "", "name", mediaItemCursor.getString(1));
                           serializer.text("\n");
                           serializer.startTag("", "description");
                           String kmlAudioUnsupported = mContext.getString(R.string.kmlAudioUnsupported);
                           serializer.text(String.format(kmlAudioUnsupported, includedMediaFile));
                           serializer.endTag("", "description");
                           serializeMediaPoint(serializer, singleWaypointUri);
                           serializer.text("\n");
                           serializer.endTag("", "Placemark");
                        }
                     }
                     finally
                     {
                        if (mediaItemCursor != null)
                        {
                           mediaItemCursor.close();
                        }
                     }
                  }
               }
            }
            while (mediaCursor.moveToNext());
         }
      }
      finally
      {
         if (mediaCursor != null)
         {
            mediaCursor.close();
         }
      }
   }

   /**
    * &lt;Point>...&lt;/Point> &lt;shape>rectangle&lt;/shape>
    * 
    * @param serializer
    * @param singleWaypointUri
    * @throws IllegalArgumentException
    * @throws IllegalStateException
    * @throws IOException
    */
   private void serializeMediaPoint(XmlSerializer serializer, Uri singleWaypointUri) throws IllegalArgumentException, IllegalStateException, IOException
   {
      Cursor waypointsCursor = null;
      ContentResolver resolver = mContext.getContentResolver();
      try
      {
         waypointsCursor = resolver.query(singleWaypointUri, new String[] { Waypoints.LONGITUDE, Waypoints.LATITUDE, Waypoints.ALTITUDE }, null, null, null);
         if (waypointsCursor.moveToFirst())
         {
            serializer.text("\n");
            serializer.startTag("", "Point");
            serializer.text("\n");
            serializer.startTag("", "coordinates");
            serializeCoordinates(serializer, waypointsCursor);
            serializer.endTag("", "coordinates");
            serializer.text("\n");
            serializer.endTag("", "Point");
            serializer.text("\n");
         }
      }
      finally
      {
         if (waypointsCursor != null)
         {
            waypointsCursor.close();
         }
      }
   }

   @Override
   protected String getContentType()
   {
      return "application/vnd.google-earth.kmz";
   }

   @Override
   public boolean needsBundling()
   {
      return true;
   }

}