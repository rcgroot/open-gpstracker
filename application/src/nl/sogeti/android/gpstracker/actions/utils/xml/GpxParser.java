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
package nl.sogeti.android.gpstracker.actions.utils.xml;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.Vector;

import nl.sogeti.android.gpstracker.R;
import nl.sogeti.android.gpstracker.actions.utils.ProgressListener;
import nl.sogeti.android.gpstracker.db.GPStracking;
import nl.sogeti.android.gpstracker.db.GPStracking.Tracks;
import nl.sogeti.android.gpstracker.db.GPStracking.Waypoints;
import nl.sogeti.android.gpstracker.util.ProgressFilterInputStream;
import nl.sogeti.android.gpstracker.util.UnicodeReader;
import nl.sogeti.android.gpstracker.viewer.TrackList;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

public class GpxParser extends AsyncTask<Uri, Integer, Uri>
{
   private static final String LATITUDE_ATRIBUTE = "lat";
   private static final String LONGITUDE_ATTRIBUTE = "lon";
   private static final String TRACK_ELEMENT = "trkpt";
   private static final String SEGMENT_ELEMENT = "trkseg";
   private static final String NAME_ELEMENT = "name";
   private static final String TIME_ELEMENT = "time";
   private static final String ELEVATION_ELEMENT = "ele";
   private static final String COURSE_ELEMENT = "course";
   private static final String ACCURACY_ELEMENT = "accuracy";
   private static final String SPEED_ELEMENT = "speed";
   public static final SimpleDateFormat ZULU_DATE_FORMAT    = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
   public static final SimpleDateFormat ZULU_DATE_FORMAT_MS = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
   public static final SimpleDateFormat ZULU_DATE_FORMAT_BC = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'UTC'");
   protected static final int DEFAULT_UNKNOWN_FILESIZE = 1024 * 1024 * 10;
   private static final String TAG = "OGT.GpxParser";
   static
   {
      TimeZone utc = TimeZone.getTimeZone("UTC");
      ZULU_DATE_FORMAT.setTimeZone(utc); // ZULU_DATE_FORMAT format ends with Z for UTC so make that true
      ZULU_DATE_FORMAT_MS.setTimeZone(utc);

   }
   
   private ContentResolver mContentResolver;
   protected String mErrorDialogMessage;
   protected Exception mErrorDialogException;
   protected Context mContext;
   protected ProgressListener mProgressListener;
   private int mLength;
   
   public GpxParser(Context trackList, ProgressListener progressListener)
   {
      mContext = trackList;
      mProgressListener = progressListener;
      mContentResolver = mContext.getContentResolver();
   }
   
   public int getMaximumProgress()
   {
      return mLength;
   }

   public void setMaximumProgress(int maximumProgress)
   {
      this.mLength = maximumProgress;
   }
   
   public void determineProgressGoal(Uri importFileUri)
   {
      mLength = DEFAULT_UNKNOWN_FILESIZE;
      if (importFileUri != null && importFileUri.getScheme().equals("file"))
      {
         File file = new File(importFileUri.getPath());
         mLength = file.length() < (long) Integer.MAX_VALUE ? (int) file.length() : Integer.MAX_VALUE;
      }
      mProgressListener.setMax(mLength);
   }

   public Uri importUri(Uri importFileUri) 
   {
      Uri result = null;
      String trackName = null;
      InputStream fis = null;
      if (importFileUri.getScheme().equals("file"))
      {
         trackName = importFileUri.getLastPathSegment();
      }
      try
      {
         fis = mContentResolver.openInputStream(importFileUri);
      }
      catch (IOException e)
      {
         handleError(e, mContext.getString(R.string.error_importgpx_io));
      }
      
      result = importTrack( fis, trackName);
      
      return result;
   }

   /**
    * 
    * TODO
    * @param fis opened stream te read from, will be closed after this call
    * @param trackName
    * @return
    */
   public Uri importTrack( InputStream fis, String trackName )
   {
      Uri trackUri = null;
      int eventType;
      ContentValues lastPosition = null;
      Vector<ContentValues> bulk = new Vector<ContentValues>();
      boolean speed = false;
      boolean accuracy = false;
      boolean bearing = false;
      boolean elevation = false;
      boolean name = false;
      boolean time = false;
      Long importDate = new Long(new Date().getTime());
      try
      {
         XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
         factory.setNamespaceAware(true);
         XmlPullParser xmlParser = factory.newPullParser();


         ProgressFilterInputStream pfis = new ProgressFilterInputStream(fis, this);
         BufferedInputStream bis = new BufferedInputStream(pfis);
         UnicodeReader ur = new UnicodeReader(bis, "UTF-8");
         xmlParser.setInput(ur);


         eventType = xmlParser.getEventType();

         String attributeName;

         Uri segmentUri = null;
         while (eventType != XmlPullParser.END_DOCUMENT)
         {
            if (eventType == XmlPullParser.START_TAG)
            {
               if (xmlParser.getName().equals(NAME_ELEMENT))
               {
                  name = true;
               }
               else
               {
                  ContentValues trackContent = new ContentValues();
                  trackContent.put(Tracks.NAME, trackName);
                  if (xmlParser.getName().equals("trk") && trackUri == null)
                  {
                     trackUri = startTrack(trackContent);
                  }
                  else if (xmlParser.getName().equals(SEGMENT_ELEMENT))
                  {
                     segmentUri = startSegment(trackUri);
                  }
                  else if (xmlParser.getName().equals(TRACK_ELEMENT))
                  {
                     lastPosition = new ContentValues();
                     for (int i = 0; i < 2; i++)
                     {
                        attributeName = xmlParser.getAttributeName(i);
                        if (attributeName.equals(LATITUDE_ATRIBUTE))
                        {
                           lastPosition.put(Waypoints.LATITUDE, new Double(xmlParser.getAttributeValue(i)));
                        }
                        else if (attributeName.equals(LONGITUDE_ATTRIBUTE))
                        {
                           lastPosition.put(Waypoints.LONGITUDE, new Double(xmlParser.getAttributeValue(i)));
                        }
                     }
                  }
                  else if (xmlParser.getName().equals(SPEED_ELEMENT))
                  {
                     speed = true;
                  }
                  else if (xmlParser.getName().equals(ACCURACY_ELEMENT))
                  {
                     accuracy = true;
                  }
                  else if (xmlParser.getName().equals(COURSE_ELEMENT))
                  {
                     bearing = true;
                  }
                  else if (xmlParser.getName().equals(ELEVATION_ELEMENT))
                  {
                     elevation = true;
                  }
                  else if (xmlParser.getName().equals(TIME_ELEMENT))
                  {
                     time = true;
                  }
               }
            }
            else if (eventType == XmlPullParser.END_TAG)
            {
               if (xmlParser.getName().equals(NAME_ELEMENT))
               {
                  name = false;
               }
               else if (xmlParser.getName().equals(SPEED_ELEMENT))
               {
                  speed = false;
               }
               else if (xmlParser.getName().equals(ACCURACY_ELEMENT))
               {
                  accuracy = false;
               }
               else if (xmlParser.getName().equals(COURSE_ELEMENT))
               {
                  bearing = false;
               }
               else if (xmlParser.getName().equals(ELEVATION_ELEMENT))
               {
                  elevation = false;
               }
               else if (xmlParser.getName().equals(TIME_ELEMENT))
               {
                  time = false;
               }
               else if (xmlParser.getName().equals(SEGMENT_ELEMENT))
               {
                  if (segmentUri == null)
                  {
                     segmentUri = startSegment( trackUri );
                  }
                  mContentResolver.bulkInsert(Uri.withAppendedPath(segmentUri, "waypoints"), bulk.toArray(new ContentValues[bulk.size()]));
                  bulk.clear();
               }
               else if (xmlParser.getName().equals(TRACK_ELEMENT))
               {
                  if (!lastPosition.containsKey(Waypoints.TIME))
                  {
                     lastPosition.put(Waypoints.TIME, importDate);
                  }
                  if (!lastPosition.containsKey(Waypoints.SPEED))
                  {
                     lastPosition.put(Waypoints.SPEED, 0);
                  }
                  bulk.add(lastPosition);
                  lastPosition = null;
               }
            }
            else if (eventType == XmlPullParser.TEXT)
            {
               String text = xmlParser.getText();
               if (name)
               {
                  ContentValues nameValues = new ContentValues();
                  nameValues.put(Tracks.NAME, text);
                  if (trackUri == null)
                  {
                     trackUri = startTrack(new ContentValues());
                  }
                  mContentResolver.update(trackUri, nameValues, null, null);
               }
               else if (lastPosition != null && speed)
               {
                  lastPosition.put(Waypoints.SPEED, Double.parseDouble(text));
               }
               else if (lastPosition != null && accuracy)
               {
                  lastPosition.put(Waypoints.ACCURACY, Double.parseDouble(text));
               }
               else if (lastPosition != null && bearing)
               {
                  lastPosition.put(Waypoints.BEARING, Double.parseDouble(text));
               }
               else if (lastPosition != null && elevation)
               {
                  lastPosition.put(Waypoints.ALTITUDE, Double.parseDouble(text));
               }
               else if (lastPosition != null && time)
               {
                  lastPosition.put(Waypoints.TIME, parseXmlDateTime(text));
               }
            }
            eventType = xmlParser.next();
         }
      }
      catch (XmlPullParserException e)
      {
         handleError(e, mContext.getString(R.string.error_importgpx_xml));
      }
      catch (ParseException e)
      {
         handleError(e, mContext.getString(R.string.error_importgpx_parse));
      }
      catch (IOException e)
      {
         handleError(e, mContext.getString(R.string.error_importgpx_io));
      }
      finally
      {
         try
         {
            fis.close();
         }
         catch (IOException e)
         {
            Log.w( TAG, "Failed closing inputstream");
         }
      }
      return trackUri;
   }

   private Uri startSegment(Uri trackUri)
   {
      if (trackUri == null)
      {
         trackUri = startTrack(new ContentValues());
      }
      return mContentResolver.insert(Uri.withAppendedPath(trackUri, "segments"), new ContentValues());
   }

   private Uri startTrack(ContentValues trackContent)
   {
      return mContentResolver.insert(Tracks.CONTENT_URI, trackContent);
   }

   public static Long parseXmlDateTime(String text) throws ParseException
   {
      if(text==null)
      {
         throw new ParseException("Unable to parse dateTime "+text+" of length ", 0);
      }
      Long dateTime = null;
      int length = text.length();
      switch (length)
      {
         case 20:
            dateTime = new Long(ZULU_DATE_FORMAT.parse(text).getTime());
            break;
         case 23:
            dateTime = new Long(ZULU_DATE_FORMAT_BC.parse(text).getTime());
            break;
         case 24:
            dateTime = new Long(ZULU_DATE_FORMAT_MS.parse(text).getTime());
            break;
         default:
            throw new ParseException("Unable to parse dateTime "+text+" of length "+length, 0);
      }
      return dateTime;
   }

   protected void handleError(Exception e, String text)
   {
      Log.e(TAG, "Unable to save ", e);
      mErrorDialogException = e;
      mErrorDialogMessage = text;
      cancel(true);
   }
   
   @Override
   protected void onPreExecute()
   {
      mProgressListener.started();
   }
   
   @Override
   protected Uri doInBackground(Uri... params)
   {
      Uri importUri = params[0];
      determineProgressGoal( importUri);
      Uri result = importUri( importUri );
      return result;
   }
   
   @Override
   protected void onProgressUpdate(Integer... values)
   {
      mProgressListener.increaseProgress(values[0]);
   }
   
   @Override
   protected void onPostExecute(Uri result)
   {
      mProgressListener.finished(result);
   }

   @Override
   protected void onCancelled()
   {
      mProgressListener.showError(mErrorDialogMessage, mErrorDialogException);
   }

   /**
    * Glue to ProgressFilterInputStream
    * 
    * @param add
    */
   public void incrementProgressBy(int add)
   {
      publishProgress( new Integer(add) );
   }
};