/*------------------------------------------------------------------------------
 **     Ident: Delivery Center Java
 **    Author: rene
 ** Copyright: (c) Jul 27, 2010 Sogeti Nederland B.V. All Rights Reserved.
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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;

import nl.sogeti.android.gpstracker.R;
import nl.sogeti.android.gpstracker.logger.GPSLoggerServiceManager;
import nl.sogeti.android.gpstracker.util.Constants;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Empty Activity that pops up the dialog to name the track
 * 
 * @version $Id: ControlTracking.java 884 2011-03-13 10:56:09Z rcgroot@gmail.com $
 * @author rene (c) Jul 27, 2010, Sogeti B.V.
 */
public class InsertNote extends Activity
{
   private static final int DIALOG_INSERTNOTE = 27;
   
   private static final String TAG = "OGT.InsertNote";

   private static final int MENU_PICTURE = 9;
   private static final int MENU_VOICE = 11;
   private static final int MENU_VIDEO = 12;
   private static final int DIALOG_TEXT = 32;
   private static final int DIALOG_NAME = 33;

   private GPSLoggerServiceManager mLoggerServiceManager;
   private boolean paused;
   private Button name;
   private Button text;
   private Button voice;
   private Button picture;
   private Button video;
   private EditText mNoteNameView;
   private EditText mNoteTextView;
   
   private final OnClickListener mNoteTextDialogListener = new DialogInterface.OnClickListener()
   {
      public void onClick( DialogInterface dialog, int which )
      {
         String noteText = mNoteTextView.getText().toString();
         Calendar c = Calendar.getInstance();
         String newName = String.format( "Textnote_%tY-%tm-%td_%tH%tM%tS.txt", c, c, c, c, c, c );
         File file = new File( Constants.getSdCardDirectory(InsertNote.this) + newName );
         FileWriter filewriter = null;
         try
         {
            file.getParentFile().mkdirs();
            file.createNewFile();
            filewriter = new FileWriter( file );
            filewriter.append( noteText );
            filewriter.flush();
         }
         catch (IOException e)
         {
            Log.e( TAG, "Note storing failed", e );
            CharSequence text = e.getLocalizedMessage();
            Toast toast = Toast.makeText( InsertNote.this, text, Toast.LENGTH_LONG );
            toast.show();
         }
         finally
         {
            if( filewriter!=null){ try { filewriter.close(); } catch (IOException e){ /* */ } }
         }
         InsertNote.this.mLoggerServiceManager.storeMediaUri( Uri.fromFile( file ) );

         setResult( RESULT_CANCELED, new Intent() );
         finish();
      }
      
   };
   private final OnClickListener mNoteNameDialogListener = new DialogInterface.OnClickListener()
   {
      public void onClick( DialogInterface dialog, int which )
      {
         String name = mNoteNameView.getText().toString();
         Uri media = Uri.withAppendedPath( Constants.NAME_URI, Uri.encode( name ) );
         InsertNote.this.mLoggerServiceManager.storeMediaUri( media );
         
         setResult( RESULT_CANCELED, new Intent() );
         finish();
      }
   };
   
   private final View.OnClickListener mNoteInsertListener = new View.OnClickListener()
      {
         public void onClick( View v )
         {
            int id = v.getId();
            switch( id )
            {
               case R.id.noteinsert_picture:
                  addPicture();
                  break;
               case R.id.noteinsert_video:
                  addVideo();
                  break;
               case R.id.noteinsert_voice:
                  addVoice();
                  break;
               case R.id.noteinsert_text:
                  showDialog( DIALOG_TEXT );
                  break;
               case R.id.noteinsert_name:
                  showDialog( DIALOG_NAME );
                  break;
               default:
                  setResult( RESULT_CANCELED, new Intent() );
                  finish();
                  break;
            }
         }
      };
   private OnClickListener mDialogClickListener = new OnClickListener()
      {
         public void onClick( DialogInterface dialog, int which )
         {
            setResult( RESULT_CANCELED,  new Intent() );
            finish();
         }
      };

   @Override
   protected void onCreate( Bundle savedInstanceState )
   {
      super.onCreate( savedInstanceState );
      this.setVisible( false );
      paused = false;
      mLoggerServiceManager = new GPSLoggerServiceManager( this );
   }

   @Override
   protected void onResume()
   {
      super.onResume();
      mLoggerServiceManager.startup( this, new Runnable()
         {
            public void run()
            {
               showDialog( DIALOG_INSERTNOTE );
            }
         } );
   }

   @Override
   protected void onPause()
   {
      super.onPause();
      mLoggerServiceManager.shutdown( this );
      paused = true;
   }
   
   /*
    * (non-Javadoc)
    * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
    */
   @Override
   protected void onActivityResult( int requestCode, int resultCode, Intent intent )
   {
      super.onActivityResult( requestCode, resultCode, intent );
      if( resultCode != RESULT_CANCELED )
      {
         File file;
         Uri uri;
         File newFile;
         String newName;
         Uri fileUri;
         android.net.Uri.Builder builder;
         switch (requestCode)
         {
            case MENU_PICTURE:
               file = new File( Constants.getSdCardTmpFile( this ) );
               Calendar c = Calendar.getInstance();
               newName =  String.format( "Picture_%tY-%tm-%td_%tH%tM%tS.jpg", c, c, c, c, c, c );
               newFile = new File( Constants.getSdCardDirectory( this ) + newName );
               file.getParentFile().mkdirs();
               boolean isRenamed = file.renameTo( newFile );
               if( isRenamed )
               {
                  Bitmap bm = BitmapFactory.decodeFile( newFile.getAbsolutePath() );
                  if( bm != null )
                  {
                     String height = Integer.toString( bm.getHeight() );
                     String width = Integer.toString( bm.getWidth() );
                     bm.recycle();
                     bm = null;
                     System.gc();
                     builder = new Uri.Builder();
                     fileUri = builder.scheme( "file").
                        appendEncodedPath( "/" ).
                        appendEncodedPath( newFile.getAbsolutePath() ).
                        appendQueryParameter( "width", width ).
                        appendQueryParameter( "height", height )
                        .build();
                     this.mLoggerServiceManager.storeMediaUri( fileUri );
                  }
                  else
                  {
                     Log.e( TAG, "Failed to read image from: " + newFile.getAbsolutePath() );
                  }
               }
               else
               {
                  Log.e( TAG, "Failed to rename image: " + file.getAbsolutePath() );
               }
               break;
            case MENU_VIDEO:
               file = new File( Constants.getSdCardTmpFile( this ) );          
               c = Calendar.getInstance();
               newName =  String.format( "Video_%tY%tm%td_%tH%tM%tS.3gp", c, c, c, c, c, c );
               newFile = new File( Constants.getSdCardDirectory( this ) + newName );
               file.getParentFile().mkdirs();
               file.renameTo( newFile );
               builder = new Uri.Builder();
               fileUri = builder.scheme( "file").
                  appendPath( newFile.getAbsolutePath() ).build();
               this.mLoggerServiceManager.storeMediaUri( fileUri );
               break;
            case MENU_VOICE:
               uri = Uri.parse( intent.getDataString() );
               this.mLoggerServiceManager.storeMediaUri(  uri );
               break;
            default:
               Log.e( TAG, "Returned form unknow activity: " + requestCode );
               break;
         }
      }
      else
      {
         Log.w( TAG, "Received unexpected resultcode "+resultCode);
      }
      setResult( resultCode, new Intent() );
      finish();
   }

   @Override
   protected Dialog onCreateDialog( int id )
   {
      Dialog dialog = null;
      LayoutInflater factory = null;
      View view = null;
      Builder builder = null;
      switch( id )
      {
         case DIALOG_INSERTNOTE:
            builder = new AlertDialog.Builder( this );
            factory = LayoutInflater.from( this );
            view = factory.inflate( R.layout.insertnote, null );
            builder.setTitle( R.string.menu_insertnote ).
            setIcon( android.R.drawable.ic_dialog_alert ).
            setNegativeButton( R.string.btn_cancel, mDialogClickListener ).
            setView( view );
            dialog = builder.create();
            name = (Button) view.findViewById( R.id.noteinsert_name );
            text = (Button) view.findViewById( R.id.noteinsert_text );
            voice = (Button) view.findViewById( R.id.noteinsert_voice );
            picture = (Button) view.findViewById( R.id.noteinsert_picture );
            video = (Button) view.findViewById( R.id.noteinsert_video );
            name.setOnClickListener( mNoteInsertListener );
            text.setOnClickListener( mNoteInsertListener );
            voice.setOnClickListener( mNoteInsertListener );
            picture.setOnClickListener( mNoteInsertListener );
            video.setOnClickListener( mNoteInsertListener );
            dialog.setOnDismissListener( new OnDismissListener()
               {
                  public void onDismiss( DialogInterface dialog )
                  {
                     if( !paused )
                     {
                        finish();
                     }
                  }
               });
            return dialog;
         case DIALOG_TEXT:
            builder = new AlertDialog.Builder( this );
            factory = LayoutInflater.from( this );
            view = factory.inflate( R.layout.notetextdialog, null );
            mNoteTextView = (EditText) view.findViewById( R.id.notetext );
            builder
               .setTitle( R.string.dialog_notetext_title )
               .setMessage( R.string.dialog_notetext_message )
               .setIcon( android.R.drawable.ic_dialog_map )
               .setPositiveButton( R.string.btn_okay, mNoteTextDialogListener )
               .setNegativeButton( R.string.btn_cancel, null )
               .setView( view );
            dialog = builder.create();
            return dialog;
         case DIALOG_NAME:
            builder = new AlertDialog.Builder( this );
            factory = LayoutInflater.from( this );
            view = factory.inflate( R.layout.notenamedialog, null );
            mNoteNameView = (EditText) view.findViewById( R.id.notename );
            builder
               .setTitle( R.string.dialog_notename_title )
               .setMessage( R.string.dialog_notename_message )
               .setIcon( android.R.drawable.ic_dialog_map )
               .setPositiveButton( R.string.btn_okay, mNoteNameDialogListener )
               .setNegativeButton( R.string.btn_cancel, null )
               .setView( view );
            dialog = builder.create();
            return dialog;
         default:
            return super.onCreateDialog( id );
      }
   }

   /*
    * (non-Javadoc)
    * @see android.app.Activity#onPrepareDialog(int, android.app.Dialog)
    */
   @Override
   protected void onPrepareDialog( int id, Dialog dialog )
   {
      switch( id )
      {
         case DIALOG_INSERTNOTE:
            boolean prepared = mLoggerServiceManager.isMediaPrepared() && mLoggerServiceManager.getLoggingState() == Constants.LOGGING;
            name = (Button) dialog.findViewById( R.id.noteinsert_name );
            text = (Button) dialog.findViewById( R.id.noteinsert_text );
            voice = (Button) dialog.findViewById( R.id.noteinsert_voice );
            picture = (Button) dialog.findViewById( R.id.noteinsert_picture );
            video = (Button) dialog.findViewById( R.id.noteinsert_video );
            name.setEnabled(prepared);
            text.setEnabled(prepared);
            voice.setEnabled(prepared);
            picture.setEnabled(prepared);
            video.setEnabled(prepared);
            break;
         default:
            break;
      }
      super.onPrepareDialog( id, dialog );
   }

   /***
    * Collecting additional data
    */
   private void addPicture()
   {
      Intent i = new Intent( android.provider.MediaStore.ACTION_IMAGE_CAPTURE );
      File file = new File( Environment.getExternalStorageDirectory().getAbsolutePath() + Constants.getSdCardTmpFile( this ) );
//      Log.d( TAG, "Picture requested at: " + file );
      i.putExtra( android.provider.MediaStore.EXTRA_OUTPUT, Uri.fromFile( file ) );
      i.putExtra( android.provider.MediaStore.EXTRA_VIDEO_QUALITY, 1 );
      startActivityForResult( i, MENU_PICTURE );
   }

   /***
    * Collecting additional data
    */
   private void addVideo()
   {
      Intent i = new Intent( android.provider.MediaStore.ACTION_VIDEO_CAPTURE );
      File file = new File( Environment.getExternalStorageDirectory().getAbsolutePath() + Constants.getSdCardTmpFile( this ) );
//      Log.d( TAG, "Video requested at: " + file );
      i.putExtra( android.provider.MediaStore.EXTRA_OUTPUT, Uri.fromFile( file ) );
      i.putExtra( android.provider.MediaStore.EXTRA_VIDEO_QUALITY, 1 );
      try
      {
         startActivityForResult( i, MENU_VIDEO );
      }
      catch (ActivityNotFoundException e) 
      {
         Log.e(  TAG, "Unable to start Activity to record video", e );
      }
   }

   private void addVoice()
   {
      Intent intent = new Intent( android.provider.MediaStore.Audio.Media.RECORD_SOUND_ACTION );
      try
      {
         startActivityForResult( intent, MENU_VOICE );
      }
      catch (ActivityNotFoundException e) 
      {
         Log.e(  TAG, "Unable to start Activity to record audio", e );
      }
   }
}
