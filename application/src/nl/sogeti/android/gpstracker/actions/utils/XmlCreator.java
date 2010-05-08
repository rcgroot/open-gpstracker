/*------------------------------------------------------------------------------
 **     Ident: Delivery Center Java
 **    Author: rene
 ** Copyright: (c) Jan 21, 2010 Sogeti Nederland B.V. All Rights Reserved.
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
package nl.sogeti.android.gpstracker.actions.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.xmlpull.v1.XmlSerializer;

import nl.sogeti.android.gpstracker.util.Constants;
import android.os.Environment;
import android.util.Log;

public abstract class XmlCreator extends Thread
{
   
   private String TAG = "OGT.XmlCreator";
   private String mExportDirectoryPath;
   private String mXmlFileName;
   private int mProgress = 0;
   private int mGoal = 0;
   private boolean mNeedsBundling;

   
   /**
    * Copies media into the export directory and returns the relative path of the media
    * 
    * @param inputFilePath
    * @return file path relative to the export dir
    * @throws IOException
    */
   protected String includeMediaFile( String inputFilePath ) throws IOException
   {
      mNeedsBundling = true ;
      File source = new File( inputFilePath );
      File target = new File( mExportDirectoryPath + "/" + source.getName() );

      FileChannel inChannel = new FileInputStream( source ).getChannel();
      FileChannel outChannel = new FileOutputStream( target ).getChannel();
      try
      {
         inChannel.transferTo( 0, inChannel.size(), outChannel );
      }
      catch( IOException e )
      {
         throw e;
      }
      finally
      {
         if( inChannel != null )
            inChannel.close();
         if( outChannel != null )
            outChannel.close();
      }

      return target.getName();
   }

   /**
    * Create a zip of the export directory based on the given filename
    * 
    * @param mExportDirectoryPath2
    * @return
    * @throws IOException
    */
   protected String bundlingMediaAndXml( String fileName, String extenstion ) throws IOException
   {
      String zipFilePath;
      if( fileName.endsWith( ".zip" ) )
      {
         zipFilePath = Environment.getExternalStorageDirectory() + Constants.EXTERNAL_DIR + fileName;
      }
      else
      {
         zipFilePath = Environment.getExternalStorageDirectory() + Constants.EXTERNAL_DIR + fileName + extenstion;
      }
      String[] filenames = new File( mExportDirectoryPath ).list();
      Log.d( TAG, String.format( "Creating zip from %s into zip file %s", mExportDirectoryPath, zipFilePath ) );
      byte[] buf = new byte[1024];
      ZipOutputStream zos = null;
      try
      {
         zos = new ZipOutputStream( new FileOutputStream( zipFilePath ) );
         for( int i = 0; i < filenames.length; i++ )
         {
            String entryFilePath = mExportDirectoryPath + "/" + filenames[i];
            Log.d( TAG, String.format( "Adding to zip %s the file %s", zipFilePath, entryFilePath ) );
            FileInputStream in = new FileInputStream( entryFilePath );
            zos.putNextEntry( new ZipEntry( filenames[i] ) );
            int len;
            while( ( len = in.read( buf ) ) >= 0 )
            {
               zos.write( buf, 0, len );
            }
            zos.closeEntry();
            in.close();
         }
      }
      finally
      {
         if( zos != null )
         {
            zos.close();
         }
      }

      deleteRecursive( new File( mExportDirectoryPath ) );

      return zipFilePath;
   }

   public static boolean deleteRecursive( File file )
   {
      if( file.isDirectory() )
      {
         String[] children = file.list();
         for( int i = 0; i < children.length; i++ )
         {
            boolean success = deleteRecursive( new File( file, children[i] ) );
            if( !success )
            {
               return false;
            }
         }
      }
      return file.delete();
   }

   public void setXmlFileName( String mXmlFileName )
   {
      this.mXmlFileName = mXmlFileName;
   }

   public String getXmlFileName()
   {
      return mXmlFileName;
   }
   
   public void setExportDirectoryPath( String exportDirectoryPath )
   {
      this.mExportDirectoryPath = exportDirectoryPath;
   }

   public String getExportDirectoryPath()
   {
      return mExportDirectoryPath;
   }
   

   public void increaseGoal( int goal )
   {
      this.mGoal += goal;
   }

   public int getGoal()
   {
      return mGoal;
   }

   public void increaseProgress( int progress )
   {
      this.mProgress += progress;
   }

   public int getProgress()
   {
      return mProgress;
   }

   public void quickTag( XmlSerializer serializer, String ns, String tag, String content) throws IllegalArgumentException, IllegalStateException, IOException
   {
      serializer.text( "\n" );
      serializer.startTag( "", tag );
      serializer.text( content );
      serializer.endTag( "", tag );
   }

   public boolean isNeedsBundling()
   {
      return mNeedsBundling;
   }
}
   
