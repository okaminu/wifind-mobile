package lt.unicorns_and_doges.wifind.model;

public class WifiSpot {

    private String id;
    private String ssid;
    private long lastUpdated;
    private Location location;

    public WifiSpot(String ssid, long lastUpdated, Location location) {
        this.ssid = ssid;
        this.lastUpdated = lastUpdated;
        this.location = location;
    }

    public String getSsid() {
        return ssid;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    public Location getLocation() {
        return location;
    }
}
