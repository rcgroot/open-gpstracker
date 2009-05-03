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
package nl.sogeti.android.gpstracker.tests.logger;

import junit.framework.Assert;
import nl.sogeti.android.gpstracker.logger.GPSLoggerService;
import nl.sogeti.android.gpstracker.logger.IGPSLoggerServiceRemote;
import android.content.Intent;
import android.location.Location;
import android.os.IBinder;
import android.os.RemoteException;
import android.test.ServiceTestCase;
import android.test.suitebuilder.annotation.SmallTest;

/**
 * Test cases for nl.sogeti.android.gpstracker.logger.GPSLoggerService
 *
 * @version $Id$
 * @author rene (c) Mar 14, 2009, Sogeti B.V.
 */
public class GPSLoggerServiceTest extends ServiceTestCase<GPSLoggerService>
{
   Location mLocation;
   
   public GPSLoggerServiceTest()
   {
      this( GPSLoggerService.class );
      this.mLocation = new Location("GPSLoggerServiceTest");
      this.mLocation.setLatitude( 37.422006d );
      this.mLocation.setLongitude( -122.084095d );
   }

   public GPSLoggerServiceTest(Class<GPSLoggerService> serviceClass)
   {
      super( serviceClass );
   }

   @SmallTest
   public void testStartStop()
   {
      startService( new Intent( GPSLoggerService.SERVICENAME ) );
      shutdownService();
      Assert.assertTrue( "No exceptions thrown", true );
   }
   
   @SmallTest
   public void testStartBind() throws RemoteException
   {
      IBinder ibinder = bindService(new Intent( GPSLoggerService.SERVICENAME ) ) ;
      IGPSLoggerServiceRemote stub = IGPSLoggerServiceRemote.Stub.asInterface((IBinder)ibinder);
      Assert.assertFalse( "The service should not be logging", stub.isLogging() );
      stub.startLogging();
      Assert.assertTrue( "The service should be logging", stub.isLogging() );
      shutdownService();
   }
      
   @SmallTest
   public void testInaccurateLocation()
   {
      startService( new Intent( GPSLoggerService.SERVICENAME ) );
      GPSLoggerService service = this.getService();
      
      Location reference = new Location( this.mLocation );
      reference.setLatitude( reference.getLatitude()+0.01d ); //Other side of the golfpark, about 1100 meters
      GPSLoggerService.storeLocation( this.getContext(), reference );
      
      this.mLocation.setAccuracy( 2000f );
      Assert.assertFalse( "An unacceptable fix", service.isLocationAcceptable( this.mLocation ) );
   }
   
   @SmallTest
   public void testAccurateLocation()
   {
      startService( new Intent( GPSLoggerService.SERVICENAME ) );
      GPSLoggerService service = this.getService();
      
      Location reference = new Location( this.mLocation );
      reference.setLatitude( reference.getLatitude()+0.01d ); //Other side of the golfpark, about 1100 meters
      GPSLoggerService.storeLocation( this.getContext(), reference );
      
      this.mLocation.setAccuracy( 50f );
      Assert.assertTrue( "An acceptable fix", service.isLocationAcceptable( this.mLocation ) );
   }

}
