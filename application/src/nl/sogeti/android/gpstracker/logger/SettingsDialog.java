/*------------------------------------------------------------------------------
 **     Ident: Innovation en Inspiration > Google Android 
 **    Author: rene
 ** Copyright: (c) Jan 22, 2009 Sogeti Nederland B.V. All Rights Reserved.
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
package nl.sogeti.android.gpstracker.logger;

import nl.sogeti.android.gpstracker.R;
import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;

/**
 * 
 * Controller for the settings dialog
 *
 * @version $Id$
 * @author rene (c) Jan 18, 2009, Sogeti B.V.
 */
public class SettingsDialog extends Dialog
{

   private SettingsManager settingsManager;

   public SettingsDialog(Context context)
   {
      super( context );

      setContentView( R.layout.settings );
      setTitle( R.string.menu_settings );

      createSettingsManager();

      populateAutoStartServiceCheckbox();
      addCancelButtonListener();
      addSaveButtonListener();
   }

   private void addCancelButtonListener()
   {
      Button button = (Button) findViewById( R.id.CancelButton );
      button.setOnClickListener( new View.OnClickListener()
         {
            public void onClick( View v )
            {
               hide();
            }
         } );
   }

   private void addSaveButtonListener()
   {
      Button button = (Button) findViewById( R.id.SaveButton );
      button.setOnClickListener( new View.OnClickListener()
         {
            public void onClick( View v )
            {
               saveSettings();
               hide();
            }
         } );
   }

   private void createSettingsManager()
   {
      this.settingsManager = SettingsManager.getInstance();
   }

   private void populateAutoStartServiceCheckbox()
   {
      CheckBox autoStartService = (CheckBox) findViewById( R.id.AutoStartService );
      autoStartService.setChecked( this.settingsManager.isAutoStartService() );
   }

   private void saveAutoStartServiceSetting()
   {
      CheckBox autoStartService = (CheckBox) findViewById( R.id.AutoStartService );
      this.settingsManager.setAutoStartService( autoStartService.isChecked() );
   }

   private void saveSettings()
   {
      saveAutoStartServiceSetting();
   }

}
