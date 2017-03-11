package lt.unicorns_and_doges.wifind.model;

import java.util.Date;

public class WifiSpot {

    private String id;
    private String SSID;
    private Date lastUpdated;
    private Location location;

    public WifiSpot(String SSID, Date lastUpdated, Location location) {
        this.SSID = SSID;
        this.lastUpdated = lastUpdated;
        this.location = location;
    }

    public String getSSID() {
        return SSID;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public Location getLocation() {
        return location;
    }
}
