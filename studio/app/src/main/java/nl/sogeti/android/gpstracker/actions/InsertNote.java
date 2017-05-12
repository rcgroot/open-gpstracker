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

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AlertDialog.Builder;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;

import nl.sogeti.android.gpstracker.R;
import nl.sogeti.android.gpstracker.integration.ServiceConstants;
import nl.sogeti.android.gpstracker.integration.ServiceManager;
import nl.sogeti.android.gpstracker.util.Constants;
import timber.log.Timber;

/**
 * Empty Activity that pops up the dialog to add a note to the most current
 * point in the logger service
 *
 * @author rene (c) Jul 27, 2010, Sogeti B.V.
 * @version $Id$
 */
public class InsertNote extends AppCompatActivity {
    private static final int DIALOG_INSERTNOTE = 27;
    private static final int MENU_PICTURE = 9;
    private static final int MENU_VOICE = 11;
    private static final int MENU_VIDEO = 12;
    private static final int DIALOG_TEXT = 32;
    private static final int DIALOG_NAME = 33;
    private final View.OnClickListener mNoteInsertListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int id = v.getId();
            switch (id) {
                case R.id.noteinsert_picture:
                    addPicture();
                    break;
                case R.id.noteinsert_video:
                    addVideo();
                    break;
                case R.id.noteinsert_text:
                    showDialog(DIALOG_TEXT);
                    break;
                case R.id.noteinsert_name:
                    showDialog(DIALOG_NAME);
                    break;
                default:
                    setResult(RESULT_CANCELED, new Intent());
                    finish();
                    break;
            }
        }
    };
    private ServiceManager mLoggerServiceManager;
    /**
     * Action to take when the LoggerService is bound
     */
    private Runnable mServiceBindAction;
    private boolean paused;
    private Button name;
    private Button text;
    private Button picture;
    private Button video;
    private EditText mNoteNameView;
    private final OnClickListener mNoteNameDialogListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            String name = mNoteNameView.getText().toString();
            Uri media = Uri.withAppendedPath(Constants.NAME_URI, Uri.encode(name));
            InsertNote.this.mLoggerServiceManager.storeMediaUri(media);

            setResult(RESULT_CANCELED, new Intent());
            finish();
        }
    };
    private EditText mNoteTextView;
    private final OnClickListener mNoteTextDialogListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            String noteText = mNoteTextView.getText().toString();
            Calendar c = Calendar.getInstance();
            String newName = String.format("Textnote_%tY-%tm-%td_%tH%tM%tS.txt", c, c, c, c, c, c);
            File file = new File(Constants.getStorageDirectory(InsertNote.this), newName);
            FileWriter filewriter = null;
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
                filewriter = new FileWriter(file);
                filewriter.append(noteText);
                filewriter.flush();
            } catch (IOException e) {
                Timber.e(e, "Note storing failed");
                CharSequence text = e.getLocalizedMessage();
                Toast toast = Toast.makeText(InsertNote.this, text, Toast.LENGTH_LONG);
                toast.show();
            } finally {
                if (filewriter != null) {
                    try {
                        filewriter.close();
                    } catch (IOException e) { /* */
                    }
                }
            }
            InsertNote.this.mLoggerServiceManager.storeMediaUri(Uri.fromFile(file));

            setResult(RESULT_CANCELED, new Intent());
            finish();
        }

    };
    private OnClickListener mDialogClickListener = new OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            setResult(RESULT_CANCELED, new Intent());
            finish();
        }
    };

    private static boolean copyFile(File fileIn, File fileOut) {
        boolean succes = false;
        FileInputStream in = null;
        FileOutputStream out = null;
        try {
            in = new FileInputStream(fileIn);
            out = new FileOutputStream(fileOut);
            byte[] buf = new byte[8192];
            int i = 0;
            while ((i = in.read(buf)) != -1) {
                out.write(buf, 0, i);
            }
            succes = true;
        } catch (IOException e) {
            Timber.e(e, "File copy failed");
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    Timber.w(e, "File close after copy failed");
                }
            }
            if (in != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    Timber.w(e, "File close after copy failed");
                }
            }
        }
        return succes;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setVisible(false);
        paused = false;
        mLoggerServiceManager = new ServiceManager();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mServiceBindAction == null) {
            mServiceBindAction = new Runnable() {
                @Override
                public void run() {
                    showDialog(DIALOG_INSERTNOTE);
                }
            };
        }
        ;
        mLoggerServiceManager.startup(this, mServiceBindAction);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mLoggerServiceManager.shutdown(this);
        paused = true;
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog = null;
        LayoutInflater factory = null;
        View view = null;
        Builder builder = null;
        switch (id) {
            case DIALOG_INSERTNOTE:
                builder = new AlertDialog.Builder(this);
                factory = LayoutInflater.from(this);
                view = factory.inflate(R.layout.insertnote, (ViewGroup) findViewById(android.R.id.content), false);
                builder.setTitle(R.string.menu_insertnote).setIcon(android.R.drawable.ic_dialog_alert).setNegativeButton
                        (R.string.btn_cancel, mDialogClickListener)
                        .setView(view);
                dialog = builder.create();
                name = (Button) view.findViewById(R.id.noteinsert_name);
                text = (Button) view.findViewById(R.id.noteinsert_text);
                picture = (Button) view.findViewById(R.id.noteinsert_picture);
                video = (Button) view.findViewById(R.id.noteinsert_video);
                name.setOnClickListener(mNoteInsertListener);
                text.setOnClickListener(mNoteInsertListener);
                picture.setOnClickListener(mNoteInsertListener);
                video.setOnClickListener(mNoteInsertListener);
                dialog.setOnDismissListener(new OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        if (!paused) {
                            finish();
                        }
                    }
                });
                return dialog;
            case DIALOG_TEXT:
                builder = new AlertDialog.Builder(this);
                factory = LayoutInflater.from(this);
                view = factory.inflate(R.layout.notetextdialog, (ViewGroup) findViewById(android.R.id.content), false);
                mNoteTextView = (EditText) view.findViewById(R.id.notetext);
                builder.setTitle(R.string.dialog_notetext_title).setMessage(R.string.dialog_notetext_message).setIcon
                        (android.R.drawable.ic_dialog_map)
                        .setPositiveButton(R.string.btn_okay, mNoteTextDialogListener).setNegativeButton(R.string
                        .btn_cancel, null).setView(view);
                dialog = builder.create();
                return dialog;
            case DIALOG_NAME:
                builder = new AlertDialog.Builder(this);
                factory = LayoutInflater.from(this);
                view = factory.inflate(R.layout.notenamedialog, (ViewGroup) findViewById(android.R.id.content), false);
                mNoteNameView = (EditText) view.findViewById(R.id.notename);
                builder.setTitle(R.string.dialog_notename_title).setMessage(R.string.dialog_notename_message).setIcon
                        (android.R.drawable.ic_dialog_map)
                        .setPositiveButton(R.string.btn_okay, mNoteNameDialogListener).setNegativeButton(R.string
                        .btn_cancel, null).setView(view);
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
    protected void onPrepareDialog(int id, Dialog dialog) {
        switch (id) {
            case DIALOG_INSERTNOTE:
                boolean prepared = mLoggerServiceManager.isMediaPrepared() && mLoggerServiceManager.getLoggingState() ==
                        ServiceConstants.STATE_LOGGING;
                name = (Button) dialog.findViewById(R.id.noteinsert_name);
                text = (Button) dialog.findViewById(R.id.noteinsert_text);
                picture = (Button) dialog.findViewById(R.id.noteinsert_picture);
                video = (Button) dialog.findViewById(R.id.noteinsert_video);
                name.setEnabled(prepared);
                text.setEnabled(prepared);
                picture.setEnabled(prepared);
                video.setEnabled(prepared);
                break;
            default:
                break;
        }
        super.onPrepareDialog(id, dialog);
    }

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onActivityResult(int, int,
     * android.content.Intent)
     */
    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        mServiceBindAction = new Runnable() {

            @Override
            public void run() {
                if (resultCode != RESULT_CANCELED) {
                    boolean isLocal = false;
                    switch (requestCode) {
                        case MENU_PICTURE: {
                            File tmpFile = Constants.getSdCardTmpFile(InsertNote.this);
                            Calendar c = Calendar.getInstance();
                            String newName = String.format("Picture_%tY-%tm-%td_%tH%tM%tS.jpg", c, c, c, c, c, c);
                            File newFile = new File(Constants.getStorageDirectory(InsertNote.this), newName);
                            tmpFile.getParentFile().mkdirs();
                            isLocal = tmpFile.renameTo(newFile); //
                            if (!isLocal) {
                                Timber.w("Failed rename will try copy image: " + tmpFile.getAbsolutePath());
                                isLocal = copyFile(tmpFile, newFile);
                            }
                            if (isLocal) {
                                System.gc();
                                Options opts = new Options();
                                opts.inJustDecodeBounds = true;
                                Bitmap bm = BitmapFactory.decodeFile(newFile.getAbsolutePath(), opts);
                                String height, width;
                                if (bm != null) {
                                    height = Integer.toString(bm.getHeight());
                                    width = Integer.toString(bm.getWidth());
                                } else {
                                    height = Integer.toString(opts.outHeight);
                                    width = Integer.toString(opts.outWidth);
                                }
                                bm = null;
                                Uri fileUri = Constants.getPictureUriFromFile(InsertNote.this, newFile, width, height);
                                InsertNote.this.mLoggerServiceManager.storeMediaUri(fileUri);

                            } else {
                                Timber.e("Failed either rename or copy image: " + tmpFile.getAbsolutePath());
                            }
                            break;
                        }
                        case MENU_VIDEO: {
                            File tmpFile = Constants.getSdCardTmpFile(InsertNote.this);
                            Calendar c = Calendar.getInstance();
                            String newName = String.format("Video_%tY%tm%td_%tH%tM%tS.3gp", c, c, c, c, c, c);
                            File newFile = new File(Constants.getStorageDirectory(InsertNote.this), newName);
                            tmpFile.getParentFile().mkdirs();
                            isLocal = tmpFile.renameTo(newFile);
                            if (!isLocal) {
                                Timber.w("Failed rename will try copy video: " + tmpFile.getAbsolutePath());
                                isLocal = copyFile(tmpFile, newFile);
                            }
                            if (isLocal) {
                                InsertNote.this.mLoggerServiceManager.storeMediaUri(Constants.getUriFromFile(InsertNote.this, newFile));
                            } else {
                                Timber.e("Failed either rename or copy video: " + tmpFile.getAbsolutePath());
                            }
                            break;
                        }
                        case MENU_VOICE: {
                            Uri uri = Uri.parse(intent.getDataString());
                            InsertNote.this.mLoggerServiceManager.storeMediaUri(uri);
                            break;
                        }
                        default:
                            Timber.e("Returned form unknow activity: " + requestCode);
                            break;
                    }
                } else {
                    Timber.w("Received unexpected resultcode " + resultCode);
                }
                setResult(resultCode, new Intent());
                finish();
            }
        };
    }

    /***
     * Collecting additional data
     */
    private void addPicture() {
        Intent i = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        File tmpFile = Constants.getSdCardTmpFile(this);
        //      Log.d( TAG, "Picture requested at: " + file );
        i.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, Constants.getUriFromFile(this, tmpFile));
        i.putExtra(android.provider.MediaStore.EXTRA_VIDEO_QUALITY, 1);
        startActivityForResult(i, MENU_PICTURE);
    }

    /***
     * Collecting additional data
     */
    private void addVideo() {
        Intent i = new Intent(android.provider.MediaStore.ACTION_VIDEO_CAPTURE);
        File tmpFile = Constants.getSdCardTmpFile(this);
        i.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, Constants.getUriFromFile(this, tmpFile));
        i.putExtra(android.provider.MediaStore.EXTRA_VIDEO_QUALITY, 1);

        try {
            startActivityForResult(i, MENU_VIDEO);
        } catch (ActivityNotFoundException e) {
            Timber.e(e, "Unable to start Activity to record video");
        }
    }
}
