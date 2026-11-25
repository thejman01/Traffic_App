package org.me.gcu.kelly_scott_s2147649;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.me.gcu.kelly_scott_s2147649.data.TrafficXmlParser;
import org.me.gcu.kelly_scott_s2147649.model.DelayLevel;
import org.me.gcu.kelly_scott_s2147649.model.Situation;
import org.me.gcu.kelly_scott_s2147649.model.SituationCategory;
import org.me.gcu.kelly_scott_s2147649.model.SituationKind;

import java.io.InputStream;
import java.util.List;
import java.util.Scanner;

/** JVM tests of the PullParser code against real samples of the two feeds (see src/test/resources). */
public class TrafficXmlParserTest {

    private static String resource(String name) {
        InputStream in = TrafficXmlParserTest.class.getClassLoader().getResourceAsStream(name);
        assertNotNull("missing test resource " + name, in);
        try (Scanner sc = new Scanner(in, "UTF-8").useDelimiter("\\A")) {
            return sc.next();
        }
    }

    @Test
    public void incidents_areParsedWithAllFields() throws Exception {
        // the sample deliberately starts with junk before the XML declaration and contains an unknown element
        List<Situation> list = TrafficXmlParser.parse(resource("unplanned_sample.xml"), SituationKind.INCIDENT);
        assertEquals(2, list.size());

        Situation closed = list.get(0);
        assertEquals("I_20260810_248_S", closed.getId());
        assertEquals("RoadOrCarriagewayOrLaneManagement", closed.getRecordType());
        assertEquals("Road closed", closed.getTypeLabel());
        assertEquals(SituationCategory.ROAD_CLOSED, closed.getCategory());
        assertTrue(closed.getComment().startsWith("The A74M(S) junction 19"));
        assertEquals(Integer.valueOf(0), closed.getLanesOperational());
        assertEquals(55.103477, closed.getFrom().getLatitude(), 1e-9);
        assertEquals(-3.273135, closed.getTo().getLongitude(), 1e-9);
        assertEquals("J18", closed.getFrom().getJunction());
        assertEquals("A74(M) J18 Ecclefechan", closed.getFrom().getName());
        assertTrue(closed.getRoadNames().contains("A74(M)"));
        assertEquals("southBound", closed.getDirection());
        assertEquals("mandatory", closed.getCompliance());
        assertNotNull(closed.getStart());
        assertNull(closed.getEnd());

        Situation queue = list.get(1);
        assertEquals(SituationCategory.QUEUING_TRAFFIC, queue.getCategory());
        assertEquals(Integer.valueOf(2), queue.getLanesRestricted());
    }

    @Test
    public void roadworks_haveDelaysDatesAndLocations() throws Exception {
        List<Situation> list = TrafficXmlParser.parse(resource("roadworks_sample.xml"), SituationKind.ROADWORK);
        assertEquals(4, list.size());
        Situation first = list.get(0);
        assertEquals("D_159301_S", first.getId());
        assertEquals(DelayLevel.LONG_DELAYS, first.getDelay());
        assertEquals("M8 WB Woodside - A804 Phoenix Rd - Closure", first.getComment());
        assertNotNull(first.getStart());
        assertNotNull(first.getEnd());
        assertTrue(first.getPlannedDurationDays() > 1000);
        assertTrue(first.getFrom().hasCoordinates() && first.getTo().hasCoordinates());
        assertTrue(first.getRoadNames().contains("M8"));
        assertEquals(SituationCategory.ROADWORKS_CLOSURE, first.getCategory());
    }

    @Test(expected = org.xmlpull.v1.XmlPullParserException.class)
    public void garbageInput_isRejectedCleanly() throws Exception {
        TrafficXmlParser.parse("this is not xml", SituationKind.INCIDENT);
    }
}
