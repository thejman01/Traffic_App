package org.me.gcu.kelly_scott_s2147649;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.me.gcu.kelly_scott_s2147649.data.TrafficXmlParser;
import org.me.gcu.kelly_scott_s2147649.model.SearchHit;
import org.me.gcu.kelly_scott_s2147649.model.Situation;
import org.me.gcu.kelly_scott_s2147649.model.SituationKind;
import org.me.gcu.kelly_scott_s2147649.util.GeoUtils;
import org.me.gcu.kelly_scott_s2147649.util.RoadNames;
import org.me.gcu.kelly_scott_s2147649.util.SituationSearch;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Scanner;

public class RoadSearchTest {

    private static List<Situation> incidents() throws Exception {
        try (Scanner sc = new Scanner(RoadSearchTest.class.getClassLoader().getResourceAsStream("unplanned_sample.xml"), "UTF-8").useDelimiter("\\A")) {
            return TrafficXmlParser.parse(sc.next(), SituationKind.INCIDENT);
        }
    }

    @Test
    public void roadMatching_isPrecise() {
        assertTrue(RoadNames.matches("A74(M)", "A74"));
        assertTrue(RoadNames.matches("A74(M)", "a74m"));
        assertTrue(RoadNames.matches("A74(M)", "A74 (M)"));
        assertFalse(RoadNames.matches("A74(M)", "A7"));
        assertFalse(RoadNames.matches("M80", "M8"));
        assertTrue(RoadNames.matches("M8", "m8"));
    }

    @Test
    public void roadsAreExtractedFromFreeText() {
        assertEquals(new LinkedHashSet<>(Arrays.asList("A78", "B957", "M73")),
                RoadNames.extractFromText("The A78 northbound at Barrassie and the B957 / M73."));
    }

    @Test
    public void searchByRoad_findsOnlySituationsOnThatRoad() throws Exception {
        List<SearchHit> m8 = SituationSearch.byRoad(incidents(), "M8");
        assertEquals(1, m8.size());
        assertEquals("I_20260831_129_S", m8.get(0).situation.getId());
        assertEquals(0, SituationSearch.byRoad(incidents(), "A9").size());
        assertEquals(1, SituationSearch.byRoad(incidents(), "a74").size());
    }

    @Test
    public void searchByDistance_respectsRadiusAndSorts() throws Exception {
        // from central Glasgow: the M8 queue is ~1 km away, the A74(M) closure is ~100 km away
        List<SearchHit> r10 = SituationSearch.nearby(incidents(), 55.8668, -4.25, 10);
        assertEquals(1, r10.size());
        List<SearchHit> r150 = SituationSearch.nearby(incidents(), 55.8668, -4.25, 150);
        assertEquals(2, r150.size());
        assertTrue(r150.get(0).distanceKm < r150.get(1).distanceKm);
    }

    @Test
    public void haversine_isSane() {
        assertEquals(0.0, GeoUtils.distanceKm(55.8, -4.2, 55.8, -4.2), 1e-9);
        assertEquals(67.0, GeoUtils.distanceKm(55.8642, -4.2518, 55.9533, -3.1883), 2.0);   // Glasgow-Edinburgh
    }
}
