package org.me.gcu.kelly_scott_s2147649.data;

import org.me.gcu.kelly_scott_s2147649.model.DelayLevel;
import org.me.gcu.kelly_scott_s2147649.model.RoadPoint;
import org.me.gcu.kelly_scott_s2147649.model.Situation;
import org.me.gcu.kelly_scott_s2147649.model.SituationKind;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;
import java.time.OffsetDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Parses the Traffic Scotland DATEX II XML (unplanned events or roadworks) using the
 * {@link XmlPullParser} (PullParser) approach, on the already-downloaded String.
 *
 * Every leaf element is handled when its END_TAG is reached (text is accumulated between tags), using
 * the stack of open elements to decide what the value means. Unknown elements are ignored, so new
 * fields appearing in the feed cannot crash the app.
 */
public final class TrafficXmlParser {

    private TrafficXmlParser() {}

    public static List<Situation> parse(String xml, SituationKind kind)
            throws XmlPullParserException, IOException {

        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        XmlPullParser xpp = factory.newPullParser();
        xpp.setInput(new StringReader(stripLeadingGarbage(xml)));

        List<Situation> result = new ArrayList<>();
        Deque<String> path = new ArrayDeque<>();   // names of the currently open elements
        StringBuilder text = new StringBuilder();  // text of the element being read
        Situation current = null;
        RoadPoint point = null;                    // the "from" or "to" being read
        String pendingValue = null;                // descriptor text waiting for its descriptor type

        int eventType = xpp.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            switch (eventType) {
                case XmlPullParser.START_TAG: {
                    String name = xpp.getName();
                    path.push(name);
                    text.setLength(0);
                    if (name.equals("situation")) {
                        current = new Situation(kind);
                        current.setId(attribute(xpp, "id"));
                    } else if (name.equals("situationRecord") && current != null) {
                        // attributes: xsi:type (the record type) and id
                        current.setRecordType(attribute(xpp, "type"));
                        current.setRecordId(attribute(xpp, "id"));
                    } else if ((name.equals("from") || name.equals("to")) && current != null) {
                        point = new RoadPoint();
                        pendingValue = null;
                    } else if (name.equals("groupOfLocations") && current != null) {
                        current.setLocationType(attribute(xpp, "type"));
                    }
                    break;
                }
                case XmlPullParser.TEXT:
                    text.append(xpp.getText());
                    break;
                case XmlPullParser.END_TAG: {
                    String name = xpp.getName();
                    String value = text.toString().trim();
                    text.setLength(0);

                    if (current != null) {
                        switch (name) {
                            case "from":
                                current.setFrom(point);
                                point = null;
                                break;
                            case "to":
                                current.setTo(point);
                                point = null;
                                break;
                            case "situation":
                                current.finish();
                                result.add(current);
                                current = null;
                                break;
                            case "value":
                                if (path.contains("generalPublicComment")) {
                                    if (current.getComment().isEmpty()) current.setComment(value);
                                } else if (path.contains("sourceName")) {
                                    current.setSource(value);
                                } else if (point != null) {
                                    pendingValue = value;
                                }
                                break;
                            case "tpegJunctionPointDescriptorType":
                            case "tpegIlcPointDescriptorType":
                            case "tpegOtherPointDescriptorType":
                                if (point != null && pendingValue != null) {
                                    classifyDescriptor(point, value, pendingValue);
                                }
                                pendingValue = null;
                                break;
                            case "latitude":
                                if (point != null) point.setLatitude(toDouble(value));
                                break;
                            case "longitude":
                                if (point != null) point.setLongitude(toDouble(value));
                                break;
                            default:
                                handleRecordField(current, name, value);
                                break;
                        }
                    }
                    path.pop();
                    break;
                }
                default:
                    break;
            }
            eventType = xpp.next();
        }
        return result;
    }

    /** Simple leaf elements of a situationRecord. */
    private static void handleRecordField(Situation s, String name, String value) {
        switch (name) {
            case "situationRecordCreationTime":
                s.setCreated(toTime(value));
                break;
            case "situationRecordVersionTime":
                s.setVersionTime(toTime(value));
                break;
            case "overallStartTime":
                s.setStart(toTime(value));
                break;
            case "overallEndTime":
                s.setEnd(toTime(value));
                break;
            case "probabilityOfOccurrence":
                s.setProbability(value);
                break;
            case "informationStatus":
                s.setInformationStatus(value);
                break;
            case "validityStatus":
                s.setValidityStatus(value);
                break;
            case "complianceOption":
                s.setCompliance(value);
                break;
            case "numberOfLanesRestricted":
                s.setLanesRestricted(toInteger(value));
                break;
            case "numberOfOperationalLanes":
                s.setLanesOperational(toInteger(value));
                break;
            case "delaysType":
                s.setDelay(DelayLevel.fromFeed(value));
                break;
            case "urgentRoadworks":
                s.setUrgent(Boolean.parseBoolean(value));
                break;
            case "roadOrCarriagewayOrLaneManagementType":
            case "abnormalTrafficType":
            case "roadMaintenanceType":
                s.setSubType(value);
                break;
            case "tpegDirection":
                s.setDirection(value);
                break;
            default:
                break; // unknown or irrelevant element: ignore
        }
    }

    /** The descriptor type arrives AFTER its text value, so it is applied when the type tag closes. */
    private static void classifyDescriptor(RoadPoint p, String type, String text) {
        switch (type) {
            case "junctionName":
                p.setJunction(text);
                break;
            case "tpegIlcName1":
            case "tpegIlcName2":
            case "linkName":
                p.addRoad(text);
                break;
            default: // intersectionName, nonLinkedPointName, ...
                if (p.getName().isEmpty()) p.setName(text);
                break;
        }
    }

    // ---------------------------------------------------------------- helpers

    /** The feed can start with garbage characters before the XML declaration. */
    static String stripLeadingGarbage(String xml) throws XmlPullParserException {
        if (xml == null) throw new XmlPullParserException("No data");
        int i = xml.indexOf("<?");
        if (i < 0) i = xml.indexOf('<');
        if (i < 0) throw new XmlPullParserException("No XML found in response");
        return i == 0 ? xml : xml.substring(i);
    }

    /** Attribute by local name (namespace-aware parser: "xsi:type" is reported as "type"). */
    private static String attribute(XmlPullParser xpp, String localName) {
        for (int i = 0; i < xpp.getAttributeCount(); i++) {
            if (localName.equals(xpp.getAttributeName(i))) return xpp.getAttributeValue(i);
        }
        return "";
    }

    private static double toDouble(String s) {
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    private static Integer toInteger(String s) {
        try {
            return Integer.valueOf(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static OffsetDateTime toTime(String s) {
        try {
            return OffsetDateTime.parse(s.trim());
        } catch (Exception e) {
            return null;
        }
    }
}
