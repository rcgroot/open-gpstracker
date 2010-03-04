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
import nl.sogeti.android.gpstracker.util.Constants;
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
      this.mLocation.setAccuracy( 50f );
   }

   public GPSLoggerServiceTest(Class<GPSLoggerService> serviceClass)
   {
      super( serviceClass );
   }

   @SmallTest
   public void testStartStop()
   {
      startService( new Intent( Constants.SERVICENAME ) );
      shutdownService();
      Assert.assertTrue( "No exceptions thrown", true );
   }
   
   @SmallTest
   public void testStartBind() throws RemoteException
   {
      IBinder ibinder = bindService(new Intent( Constants.SERVICENAME ) ) ;
      IGPSLoggerServiceRemote stub = IGPSLoggerServiceRemote.Stub.asInterface((IBinder)ibinder);
      Assert.assertEquals( "The service should not be logging", Constants.STOPPED ,stub.loggingState() );
      stub.startLogging();
      Assert.assertEquals( "The service should be logging", Constants.LOGGING, stub.loggingState() );
      shutdownService();
   }
      
   @SmallTest
   public void testInaccurateLocation()
   {
      startService( new Intent( Constants.SERVICENAME ) );
      GPSLoggerService service = this.getService();
      
      Location reference = new Location( this.mLocation );
      reference.setLatitude( reference.getLatitude()+0.01d ); //Other side of the golfpark, about 1100 meters
      service.storeLocation( reference );
      
      this.mLocation.setAccuracy( 50f );
      Assert.assertNull( "An unacceptable fix", service.locationFilter( this.mLocation ) );
   }
   
   @SmallTest
   public void testAccurateLocation()
   {
      startService( new Intent( Constants.SERVICENAME ) );
      GPSLoggerService service = this.getService();
      
      Location reference = new Location( this.mLocation );
      reference.setLatitude( reference.getLatitude()+0.01d ); //Other side of the golfpark, about 1100 meters
      service.storeLocation( reference );
      
      this.mLocation.setAccuracy( 9f );
      Location returned = service.locationFilter( this.mLocation ) ;
      Assert.assertNotNull( "An acceptable fix", returned );
   }

   @SmallTest
   public void testCloseLocation()
   {
      startService( new Intent( Constants.SERVICENAME ) );
      GPSLoggerService service = this.getService();
      
      Location reference = new Location( this.mLocation );
      reference.setLatitude( reference.getLatitude()+0.0001d ); // About 11 meters
      service.storeLocation( reference );
      
      this.mLocation.setAccuracy( 9f );
      Assert.assertNotNull( "An acceptable fix", service.locationFilter( this.mLocation ) );
   }
   
   @SmallTest
   public void testBetterSomethingThenNothingAccurateLocation()
   {
      startService( new Intent( Constants.SERVICENAME ) );
      GPSLoggerService service = this.getService();
      
      Location first = this.mLocation;
      first.setAccuracy( 150f );
      
      Location second = new Location( this.mLocation );
      second.setAccuracy( 100f );
      second.setLatitude( second.getLatitude()+0.01d ); //Other side of the golfpark, about 1100 meters    
      
      Location third = new Location( this.mLocation );
      third.setAccuracy( 125f );
      third.setLatitude( third.getLatitude()+0.01d ); //about 1100 meters    

      Assert.assertNull( "An unacceptable fix", service.locationFilter( first ) );
      Assert.assertNull( "An unacceptable fix", service.locationFilter( second ) );
      Location last = service.locationFilter( third );
      Assert.assertNotNull( "An acceptable fix", last );
      Assert.assertEquals(  "Best one was the second one", second, last );
      Assert.assertNull( "An unacceptable fix", service.locationFilter( first ) );
   }
   
   @SmallTest
   public void testToFastLocation()
   {
      startService( new Intent( Constants.SERVICENAME ) );
      GPSLoggerService service = this.getService();
      
      Location reference = new Location( this.mLocation );
      reference.setLatitude( reference.getLatitude()+0.0001d );
      reference.setSpeed( 419f );
      reference.setAccuracy( 9f );
      service.storeLocation( this.mLocation );
      
      Location sane = service.locationFilter( reference );
      Assert.assertFalse( "No speed anymore", sane.hasSpeed() );
      Assert.assertSame( "Still the same", reference, sane );
   }
   
   @SmallTest
   public void testNormalSpeedLocation()
   {
      startService( new Intent( Constants.SERVICENAME ) );
      GPSLoggerService service = this.getService();
      
      Location reference = new Location( this.mLocation );
      reference.setLatitude( reference.getLatitude()+0.0001d );
      reference.setSpeed( 4f );
      reference.setAccuracy( 9f );
      service.storeLocation( this.mLocation );
      
      Location sane = service.locationFilter( reference );
      Assert.assertTrue( "No speed anymore", sane.hasSpeed() );
      Assert.assertSame( "Still the same", reference, sane );
   }
}
   
