package org.me.gcu.kelly_scott_s2147649.model;

import java.util.ArrayList;
import java.util.List;

/** One end ("from" or "to") of the linear location of a situation. */
public class RoadPoint {
    private double latitude = Double.NaN;
    private double longitude = Double.NaN;
    private String name = "";       // e.g. "A74(M) J19 Eaglesfield"
    private String junction = "";   // e.g. "J19"
    private final List<String> roads = new ArrayList<>();

    public boolean hasCoordinates() {
        return !Double.isNaN(latitude) && !Double.isNaN(longitude);
    }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getJunction() { return junction; }
    public void setJunction(String junction) { this.junction = junction; }
    public List<String> getRoads() { return roads; }

    public void addRoad(String road) {
        if (road != null && !road.trim().isEmpty() && !roads.contains(road.trim())) {
            roads.add(road.trim());
        }
    }

    /** Best human-readable description of this point. */
    public String getDisplayName() {
        if (!name.isEmpty()) return name;
        if (!junction.isEmpty()) return roads.isEmpty() ? junction : roads.get(0) + " " + junction;
        return roads.isEmpty() ? "" : roads.get(0);
    }
}
