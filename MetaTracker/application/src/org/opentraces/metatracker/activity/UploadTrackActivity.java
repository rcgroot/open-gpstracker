/*
 * Copyright (C) 2010  Just Objects B.V.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.opentraces.metatracker.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import org.opentraces.metatracker.Constants;
import org.opentraces.metatracker.R;
import org.opentraces.metatracker.net.OpenTracesClient;

public class UploadTrackActivity extends Activity
{

	protected static final int DIALOG_FILENAME = 11;
	protected static final int PROGRESS_STEPS = 10;
	private static final int DIALOG_INSTALL_FILEMANAGER = 34;
	private static final String TAG = "MT.UploadTrack";

	private String filePath;
	private TextView fileNameView;
	private SharedPreferences sharedPreferences;

	private final DialogInterface.OnClickListener mFileManagerDownloadDialogListener = new DialogInterface.OnClickListener()
	{
		public void onClick(DialogInterface dialog, int which)
		{
			Uri oiDownload = Uri.parse("market://details?id=org.openintents.filemanager");
			Intent oiFileManagerDownloadIntent = new Intent(Intent.ACTION_VIEW, oiDownload);
			try
			{
				startActivity(oiFileManagerDownloadIntent);
			}
			catch (ActivityNotFoundException e)
			{
				oiDownload = Uri.parse("http://openintents.googlecode.com/files/FileManager-1.1.3.apk");
				oiFileManagerDownloadIntent = new Intent(Intent.ACTION_VIEW, oiDownload);
				startActivity(oiFileManagerDownloadIntent);
			}
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		setContentView(R.layout.uploaddialog);
		fileNameView = (TextView) findViewById(R.id.filename);
		filePath = null;
		pickFile();

		Button okay = (Button) findViewById(R.id.okayupload_button);
		okay.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				if (filePath == null) {
					return;
				}
				uploadFile(filePath);
			}
		});
	}

	/*
		* (non-Javadoc)
		* @see android.app.Activity#onCreateDialog(int)
		*/
	@Override
	protected Dialog onCreateDialog(int id)
	{
		Dialog dialog = null;
		Builder builder = null;
		switch (id)
		{
			case DIALOG_INSTALL_FILEMANAGER:
				builder = new AlertDialog.Builder(this);
				builder
						.setTitle(R.string.dialog_nofilemanager)
						.setMessage(R.string.dialog_nofilemanager_message)
						.setIcon(android.R.drawable.ic_dialog_alert)
						.setPositiveButton(R.string.btn_install, mFileManagerDownloadDialogListener)
						.setNegativeButton(R.string.btn_cancel, null);  
				dialog = builder.create();
				break;
			default:
				dialog = super.onCreateDialog(id);
				break;
		}
		return dialog;
	}


	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent)
	{
		super.onActivityResult(requestCode, resultCode, intent);
		// Bundle extras = intent.getExtras();
		switch (requestCode)
		{
			case Constants.REQUEST_CODE_PICK_FILE_OR_DIRECTORY:
				if (resultCode == RESULT_OK && intent != null)
				{
					// obtain the filename
					filePath = intent.getDataString();
					if (filePath != null)
					{
						// Get rid of URI prefix:
						if (filePath.startsWith("file://"))
						{
							filePath = filePath.substring(7);
						}

						fileNameView.setText(filePath);
					}
				}
				break;
		}
	}

	/*
	http://edwards.sdsu.edu/labsite/index.php/josh/179-android-nuts-and-bolts-vii
	http://code.google.com/p/openintents/source/browse/trunk/samples/TestFileManager/src/org/openintents/samples/TestFileManager/TestFileManager.java
	 */
	private void pickFile()
	{

		Intent intent = new Intent(Constants.ACTION_PICK_FILE);
		intent.setData(Uri.parse("file://" + Environment.getExternalStorageDirectory() + Constants.EXTERNAL_DIR));

		try
		{
			startActivityForResult(intent, Constants.REQUEST_CODE_PICK_FILE_OR_DIRECTORY);
		} catch (ActivityNotFoundException e)
		{
			// No compatible file manager was found: install openintents filemanager.
			showDialog(DIALOG_INSTALL_FILEMANAGER);
		}
	}



	/*
	http://edwards.sdsu.edu/labsite/index.php/josh/179-android-nuts-and-bolts-vii
	http://code.google.com/p/openintents/source/browse/trunk/samples/TestFileManager/src/org/openintents/samples/TestFileManager/TestFileManager.java
	 */
	private void uploadFile(String aFilePath)
	{

		Intent intent = new Intent(Constants.ACTION_PICK_FILE);
		intent.setData(Uri.parse("file://" + Environment.getExternalStorageDirectory() + Constants.EXTERNAL_DIR));


		try
		{
			;
			OpenTracesClient openTracesClient = new OpenTracesClient(sharedPreferences.getString(Constants.PREF_SERVER_URL, "http://geotracing.com/tland"));
			openTracesClient.createSession();
			openTracesClient.login(sharedPreferences.getString(Constants.PREF_SERVER_USER, "no_user"), sharedPreferences.getString(Constants.PREF_SERVER_PASSWORD, "no_passwd"));

			openTracesClient.uploadFile(aFilePath);
			openTracesClient.logout();
		} catch (Throwable e)
		{
			 Log.e(TAG, "Error uploading file : ", e);
		}
	}

}
