package nl.sogeti.android.gpstracker.tests.viewer;

import android.test.suitebuilder.annotation.SmallTest;

import junit.framework.Assert;
import junit.framework.TestCase;

import java.text.ParseException;

import nl.sogeti.android.gpstracker.actions.tasks.GpxParser;

public class TrackListTest extends TestCase {


    @SmallTest
    public void testGarminDateTime() throws ParseException {
        String dateTime = "2010-10-19T07:58:23.000Z";
        long result = GpxParser.parseXmlDateTime(dateTime);
        Assert.assertEquals("Date Time test", 1287475103000l, result);
    }

    @SmallTest
    public void testOGTDateTime() throws ParseException {
        String dateTime = "2010-09-06T15:36:44Z";
        long result = GpxParser.parseXmlDateTime(dateTime);
        Assert.assertEquals("Date Time test", 1283787404000l, result);
    }
}