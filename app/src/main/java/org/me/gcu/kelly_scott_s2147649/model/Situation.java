package org.me.gcu.kelly_scott_s2147649.model;

import org.me.gcu.kelly_scott_s2147649.util.GeoUtils;
import org.me.gcu.kelly_scott_s2147649.util.RoadNames;
import org.me.gcu.kelly_scott_s2147649.util.TextUtils;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * A single Traffic Scotland "situation" (an unplanned incident or a roadworks record)
 * as extracted from the DATEX II XML by {@link org.me.gcu.kelly_scott_s2147649.data.TrafficXmlParser}.
 */
public class Situation {
    private final SituationKind kind;
    private String id = "";
    private String recordId = "";
    private String recordType = "";      // xsi:type, e.g. AbnormalTraffic, MaintenanceWorks
    private String subType = "";         // roadClosed, queuingTraffic, roadworks, ...
    private String comment = "";         // generalPublicComment
    private String probability = "";
    private String validityStatus = "";
    private String compliance = "";
    private String informationStatus = "";
    private String source = "";
    private OffsetDateTime created;
    private OffsetDateTime versionTime;
    private OffsetDateTime start;
    private OffsetDateTime end;
    private Integer lanesRestricted;
    private Integer lanesOperational;
    private DelayLevel delay = DelayLevel.NONE;
    private boolean urgent;
    private String direction = "";
    private String locationType = "";
    private RoadPoint from;
    private RoadPoint to;
    private Set<String> roadNames = Collections.emptySet();

    public Situation(SituationKind kind) {
        this.kind = kind;
    }

    // ---------------------------------------------------------------- derived values

    /** Called once by the parser when the end of the situation is reached. */
    public void finish() {
        Set<String> roads = new LinkedHashSet<>();
        if (from != null) for (String r : from.getRoads()) roads.add(RoadNames.canonical(r));
        if (to != null) for (String r : to.getRoads()) roads.add(RoadNames.canonical(r));
        // "further parsing": pull road names out of the free text as well
        roads.addAll(RoadNames.extractFromText(comment));
        if (from != null) roads.addAll(RoadNames.extractFromText(from.getName()));
        if (to != null) roads.addAll(RoadNames.extractFromText(to.getName()));
        roads.remove("");
        roadNames = Collections.unmodifiableSet(roads);
    }

    public SituationCategory getCategory() {
        String t = subType.toLowerCase(Locale.ROOT);
        if (kind == SituationKind.ROADWORK) {
            String c = comment.toLowerCase(Locale.ROOT);
            return (c.contains("closure") || c.contains("closed"))
                    ? SituationCategory.ROADWORKS_CLOSURE : SituationCategory.ROADWORKS;
        }
        if (t.contains("roadclosed")) return SituationCategory.ROAD_CLOSED;
        if (t.contains("queuing") || t.contains("stationary")) return SituationCategory.QUEUING_TRAFFIC;
        if (t.contains("slow") || t.contains("heavy")) return SituationCategory.SLOW_TRAFFIC;
        if (t.contains("lane") || t.contains("contraflow") || t.contains("narrow")
                || (lanesRestricted != null && lanesRestricted > 0)) return SituationCategory.LANE_RESTRICTION;
        if (lanesOperational != null && lanesOperational == 0) return SituationCategory.ROAD_CLOSED;
        return SituationCategory.OTHER;
    }

    /** Short type text for the list, e.g. "Road closed" or "Roadworks". */
    public String getTypeLabel() {
        if (kind == SituationKind.ROADWORK) {
            return urgent ? "Urgent roadworks" : "Roadworks";
        }
        if (!subType.isEmpty()) return TextUtils.humanise(subType);
        if (!recordType.isEmpty()) return TextUtils.humanise(recordType);
        return "Incident";
    }

    public boolean hasLocation() {
        return (from != null && from.hasCoordinates()) || (to != null && to.hasCoordinates());
    }

    /** Midpoint of the situation (or the single known point). NaN if it has no coordinates. */
    public double[] getMidpoint() {
        boolean f = from != null && from.hasCoordinates();
        boolean t = to != null && to.hasCoordinates();
        if (f && t) {
            return new double[]{(from.getLatitude() + to.getLatitude()) / 2.0,
                    (from.getLongitude() + to.getLongitude()) / 2.0};
        }
        if (f) return new double[]{from.getLatitude(), from.getLongitude()};
        if (t) return new double[]{to.getLatitude(), to.getLongitude()};
        return new double[]{Double.NaN, Double.NaN};
    }

    /** Smallest distance (km) from a position to this situation's from / mid / to points; +infinity if unknown. */
    public double distanceKmFrom(double lat, double lon) {
        if (!hasLocation()) return Double.POSITIVE_INFINITY;
        double best = Double.POSITIVE_INFINITY;
        if (from != null && from.hasCoordinates())
            best = Math.min(best, GeoUtils.distanceKm(lat, lon, from.getLatitude(), from.getLongitude()));
        if (to != null && to.hasCoordinates())
            best = Math.min(best, GeoUtils.distanceKm(lat, lon, to.getLatitude(), to.getLongitude()));
        double[] mid = getMidpoint();
        best = Math.min(best, GeoUtils.distanceKm(lat, lon, mid[0], mid[1]));
        return best;
    }

    /** Planned length of the works in days, or -1 if start or end is unknown. */
    public long getPlannedDurationDays() {
        if (start == null || end == null) return -1;
        return Duration.between(start, end).toDays();
    }

    /** All text of the situation in lower case, for free text searching. */
    public String getSearchableText() {
        StringBuilder sb = new StringBuilder(comment);
        if (from != null) sb.append(' ').append(from.getDisplayName());
        if (to != null) sb.append(' ').append(to.getDisplayName());
        return sb.toString().toLowerCase(Locale.ROOT);
    }

    // ---------------------------------------------------------------- plain getters / setters

    public SituationKind getKind() { return kind; }
    public String getId() { return id; }
    public void setId(String id) { this.id = id == null ? "" : id; }
    public String getRecordId() { return recordId; }
    public void setRecordId(String recordId) { this.recordId = recordId == null ? "" : recordId; }
    public String getRecordType() { return recordType; }
    public void setRecordType(String recordType) { this.recordType = recordType == null ? "" : recordType; }
    public String getSubType() { return subType; }
    public void setSubType(String subType) { this.subType = subType == null ? "" : subType; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment == null ? "" : comment; }
    public String getProbability() { return probability; }
    public void setProbability(String probability) { this.probability = probability; }
    public String getValidityStatus() { return validityStatus; }
    public void setValidityStatus(String validityStatus) { this.validityStatus = validityStatus; }
    public String getCompliance() { return compliance; }
    public void setCompliance(String compliance) { this.compliance = compliance; }
    public String getInformationStatus() { return informationStatus; }
    public void setInformationStatus(String informationStatus) { this.informationStatus = informationStatus; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public OffsetDateTime getCreated() { return created; }
    public void setCreated(OffsetDateTime created) { this.created = created; }
    public OffsetDateTime getVersionTime() { return versionTime; }
    public void setVersionTime(OffsetDateTime versionTime) { this.versionTime = versionTime; }
    public OffsetDateTime getStart() { return start; }
    public void setStart(OffsetDateTime start) { this.start = start; }
    public OffsetDateTime getEnd() { return end; }
    public void setEnd(OffsetDateTime end) { this.end = end; }
    public Integer getLanesRestricted() { return lanesRestricted; }
    public void setLanesRestricted(Integer lanesRestricted) { this.lanesRestricted = lanesRestricted; }
    public Integer getLanesOperational() { return lanesOperational; }
    public void setLanesOperational(Integer lanesOperational) { this.lanesOperational = lanesOperational; }
    public DelayLevel getDelay() { return delay; }
    public void setDelay(DelayLevel delay) { this.delay = delay; }
    public boolean isUrgent() { return urgent; }
    public void setUrgent(boolean urgent) { this.urgent = urgent; }
    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction == null ? "" : direction; }
    public String getLocationType() { return locationType; }
    public void setLocationType(String locationType) { this.locationType = locationType == null ? "" : locationType; }
    public RoadPoint getFrom() { return from; }
    public void setFrom(RoadPoint from) { this.from = from; }
    public RoadPoint getTo() { return to; }
    public void setTo(RoadPoint to) { this.to = to; }
    public Set<String> getRoadNames() { return roadNames; }
}
