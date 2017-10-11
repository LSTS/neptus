package pt.lsts.autonomy.soi;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Vector;

import pt.lsts.imc.Goto;
import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IMCOutputStream;
import pt.lsts.imc.IMCUtil;
import pt.lsts.imc.Maneuver;
import pt.lsts.imc.PlanSpecification;
import pt.lsts.imc.ScheduledGoto;
import pt.lsts.imc.SoiPlan;
import pt.lsts.imc.SoiWaypoint;
import pt.lsts.util.PlanUtilities;
import pt.lsts.util.WGS84Utilities;

public class Plan {

	private final String planId;
	private boolean cyclic = false;
	private ArrayList<Waypoint> waypoints = new ArrayList<>();
	
	/**
	 * @return the planId
	 */
	public final String getPlanId() {
		return planId;
	}

	/**
	 * @return the cyclic
	 */
	public final boolean isCyclic() {
		return cyclic;
	}

	/**
	 * @param cyclic the cyclic to set
	 */
	public final void setCyclic(boolean cyclic) {
		this.cyclic = cyclic;
	}

	public Plan(String id) {
		this.planId = id;
	}

	public static Plan parse(PlanSpecification spec) {
		Plan plan = new Plan(spec.getPlanId());
		int id = 1;
		for (Maneuver m : PlanUtilities.getManeuverCycleOrSequence(spec)) {
			try {
				plan.addWaypoint(new Waypoint(id++, m));
			} catch (Exception e) {
				e.printStackTrace();
			}			
		}

		if (PlanUtilities.isCyclic(spec))
			plan.cyclic = true;
		
		return plan;
	}
	
	public static Plan parse(SoiPlan spec) {
		Plan plan = new Plan("soi_"+spec.getPlanId());
		int id = 1;
		for (SoiWaypoint wpt : spec.getWaypoints()) {
			Waypoint soiWpt = new Waypoint(id++, (float)wpt.getLat(), (float)wpt.getLon());
			soiWpt.setDuration(wpt.getDuration());
			if (wpt.getEta() > 0)
				soiWpt.setArrivalTime(new Date(1000 * wpt.getEta()));
			plan.addWaypoint(soiWpt);
		}
		return plan;		
	}
	
	public SoiPlan asImc() {
		SoiPlan plan = new SoiPlan();
		Vector<SoiWaypoint> wpts = new Vector<>();
		for (Waypoint wpt : waypoints) {
			SoiWaypoint waypoint = new SoiWaypoint();
			if (wpt.getArrivalTime() != null)
			    waypoint.setEta(wpt.getArrivalTime().getTime() / 1000);
			waypoint.setLat(wpt.getLatitude());
			waypoint.setLon(wpt.getLongitude());
			waypoint.setDuration(wpt.getDuration());
			wpts.add(waypoint);
		}
		plan.setWaypoints(wpts);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		IMCOutputStream ios = new IMCOutputStream(baos);  		        
		try {
            IMCDefinition.getInstance().serializeFields(plan, ios);
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
		byte[] data = baos.toByteArray();
		plan.setPlanId(IMCUtil.computeCrc16(data, 2, data.length-2, 0));
		return plan;
	}
	
	public int checksum() {
	    
	    ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IMCOutputStream ios = new IMCOutputStream(baos);                
        try {
            IMCDefinition.getInstance().serializeFields(asImc(), ios);
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        byte[] data = baos.toByteArray();
        return IMCUtil.computeCrc16(data, 2, data.length-2, 0);
	}

	public void addWaypoint(Waypoint waypoint) {
		synchronized (waypoints) {
			waypoints.add(waypoint);			
		}
	}
	
	public Waypoint waypoint(int index) {
		if (index < 0 || index >= waypoints.size())
			return null;
		return waypoints.get(index);
	}

	public void remove(int index) {
		synchronized (waypoints) {
			waypoints.remove(index);
		}
	}

	public void scheduleWaypoints(long startTime, double lat, double lon, double speed) {
		long curTime = startTime;
		synchronized (waypoints) {
			for (Waypoint waypoint : waypoints) {
				double distance = WGS84Utilities.distance(lat, lon, waypoint.getLatitude(), waypoint.getLongitude());
				double timeToReach = distance / speed;
				curTime += (long) (1000.0 * (timeToReach + waypoint.getDuration()));
				waypoint.setArrivalTime(new Date(curTime));
				lat = waypoint.getLatitude();
				lon = waypoint.getLongitude();
			}
		}
	}

	public void scheduleWaypoints(long startTime, double speed) {
		if (waypoints.isEmpty())
			return;

		Waypoint start = waypoints.get(0);
		scheduleWaypoints(startTime, start.getLatitude(), start.getLongitude(), speed);
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Plan '"+planId+"'"+(cyclic? " (cyclic):\n": ":\n"));
		synchronized (waypoints) {
			for (Waypoint wpt : waypoints) {
				sb.append("\t"+wpt.getId() + ", " + (float) wpt.getLatitude() + ", " + (float) wpt.getLongitude() + ", "
						+ wpt.getArrivalTime() + ", "+wpt.getDuration()+"\n");
			}
		}

		return sb.toString();

	}

	public void remove(Waypoint waypoint) {
		remove(waypoint.getId());
	}
	
	public static void main(String[] args) throws Exception {
		Plan plan = new Plan("test");
		ScheduledGoto goto1 = new ScheduledGoto();
		goto1.setLat(Math.toRadians(41));
		goto1.setLon(Math.toRadians(-8));
		goto1.setArrivalTime(new Date().getTime() / 1000.0 + 3600);

		ScheduledGoto goto2 = new ScheduledGoto();
		goto2.setLat(Math.toRadians(41.5));
		goto2.setLon(Math.toRadians(-8.5));
		goto2.setArrivalTime(new Date().getTime() / 1000.0 + 1800);

		Goto goto3 = new Goto();
		goto3.setLat(Math.toRadians(41.2));
		goto3.setLon(Math.toRadians(-8.2));

		Goto goto4 = new Goto();
		goto4.setLat(Math.toRadians(41.4));
		goto4.setLon(Math.toRadians(-8.4));

		plan.addWaypoint(new Waypoint(1, goto1));
		plan.addWaypoint(new Waypoint(2, goto2));
		plan.addWaypoint(new Waypoint(3, goto3));
		plan.addWaypoint(new Waypoint(4, goto4));
		
		plan.scheduleWaypoints(System.currentTimeMillis(), 1);

		System.out.println(plan);
	}
}
