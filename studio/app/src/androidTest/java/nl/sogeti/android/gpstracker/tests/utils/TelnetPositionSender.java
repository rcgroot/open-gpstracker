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
package nl.sogeti.android.gpstracker.tests.utils;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;


/**
 * Translates SimplePosition objects to a telnet command and sends the commands to a telnet session with an android
 * emulator.
 *
 * @author Bram Pouwelse (c) Jan 22, 2009, Sogeti B.V.
 * @version $Id$
 */
public class TelnetPositionSender {
    private static final String TAG = "TelnetPositionSender";

    private static final String TELNET_OK_FEEDBACK_MESSAGE = "OK\r\n";
    private static String HOST = "10.0.2.2";
    private static int PORT = 5554;

    private Socket socket;
    private OutputStream out;
    private InputStream in;


    /**
     * Constructor
     */
    public TelnetPositionSender() {

    }

    /**
     * When a new position is received it is sent to the android emulator over the telnet connection.
     *
     * @param position the position to send
     */
    public void sendCommand(String telnetString) {
        createTelnetConnection();

        Log.v(TAG, "Sending command: " + telnetString);

        byte[] sendArray = telnetString.getBytes();

        for (byte b : sendArray) {
            try {
                this.out.write(b);
            } catch (IOException e) {
                System.out.println("IOException: " + e.getMessage());
            }
        }

        String feedback = readInput();
        if (!feedback.equals(TELNET_OK_FEEDBACK_MESSAGE)) {
            System.err.println("Warning: no OK mesage message was(" + feedback + ")");
        }
        closeConnection();

    }

    /**
     * Setup a telnet connection to the android emulator
     */
    private void createTelnetConnection() {
        try {
            this.socket = new Socket(HOST, PORT);
            this.in = this.socket.getInputStream();
            this.out = this.socket.getOutputStream();

            Thread.sleep(500); // give the telnet session half a second to
            // respond
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        readInput(); // read the input to throw it away the first time :)
    }

    /**
     * read the input buffer
     *
     * @return
     */
    private String readInput() {
        StringBuffer sb = new StringBuffer();
        try {
            byte[] bytes = new byte[this.in.available()];
            this.in.read(bytes);

            for (byte b : bytes) {
                sb.append((char) b);
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not read the input from the telnet session");
        }

        return sb.toString();
    }

    private void closeConnection() {
        try {
            this.out.close();
            this.in.close();
            this.socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}