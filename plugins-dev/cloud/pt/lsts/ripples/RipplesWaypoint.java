package pt.lsts.ripples;

public class RipplesWaypoint {
    private double latitude;
	private double longitude;
	private long eta = 0;
	private int duration = 0;


	public RipplesWaypoint(double[] latLng) {
		this.latitude = latLng[0];
		this.longitude = latLng[1];
	}

	public double getLatitude() {
		return latitude;
	}

	public int getDuration() {
		return duration;
	}

	public void setDuration(int duration) {
		this.duration = duration;
	}

	public long getEta() {
		return eta;
	}

	public void setEta(long eta) {
		this.eta = eta;
	}

	public double getLongitude() {
		return longitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}
}