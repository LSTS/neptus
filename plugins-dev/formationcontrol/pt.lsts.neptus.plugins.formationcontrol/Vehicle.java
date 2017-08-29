package pt.lsts.neptus.plugins.formationcontrol;//package elias.kth.MyFirstPlugin;

import pt.lsts.imc.*;
import pt.lsts.imc.def.SpeedUnits;
import pt.lsts.imc.def.ZUnits;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.notifications.Notification;
import pt.lsts.neptus.plugins.SimpleRendererInteraction;
import java.awt.*;
import java.util.Arrays;

/**
 * Created by elias on 8/11/16.
 */
public class Vehicle extends SimpleRendererInteraction {
    private String systemName;
    private EstimatedState estimatedState;
    private Announce announce;

    public VehicleParameters params = new VehicleParameters();

    private long lastAnnounce = 0;
    private long announceTimeout = 60000;
    private boolean isConnected = false;
    private boolean followReferenceIsActive = false;
    private int imc_address;
    private double lat, lon, heading, speed;
    public Reference currentReference = new Reference();
    public FollowRefState refState;
    public double radius = 6;
    public Color referenceColor = new Color(0,0,0);

    public double prev[] = new double[2];

    public Vehicle(ConsoleLayout Console, String name, Color ReferenceColor, double d, double delta, double alpha) {
        super(Console);
        systemName = name;
        referenceColor = ReferenceColor;
        params.setRadius(d);
        params.setDelta(delta);
        params.setAlpha(alpha);
    }
    public void newAnnouce(Announce msg) {
        lastAnnounce = System.currentTimeMillis();
        announce = msg;
        if (!isConnected) {
            post(Notification.success(systemName, "elias.kth.MyFirstPlugin.Vehicle connected."));
            isConnected = true;
        }
    }
    public void setAddress(int address) {
        imc_address = address;
    }
    public int getAddress() {
        return imc_address;
    }
    public void update() {
        if ((System.currentTimeMillis() - lastAnnounce > announceTimeout) && isConnected) {
            post(Notification.warning("Multiple-vehicle controller", this.systemName + " has disconnected."));
            isConnected = false;
        }
    }

    public boolean isConnected() {
        return isConnected;
    }
    public String getSystemName(){
        return systemName;
    }
    public void setEstimatedState(EstimatedState esta) {
        estimatedState = esta;
        lat = esta.getLat();
        lon = esta.getLon();
        heading = esta.getPsi();
        speed = esta.getU();
        //System.out.println(this.systemName + " got new EstimatedState.");
    }
    public void setRefState(FollowRefState rs) {
        this.refState = rs;
    }
    public void setConnectionTimeout(long timeout) { announceTimeout = timeout;
    }
    public double getLat() {
        return lat;
    }
    public double getLon() {
        return lon;
    }
    public double getAnnounceLat() {
        return announce.getLat();
    }
    public double getAnnounceLon() {
        return announce.getLon();
    }
    public double getHeading() { return heading; }
    public double getSpeed() { return speed; }

    public void startFollowReferenceManeuver() {
        PlanControl startPlan = new PlanControl();
        startPlan.setType(PlanControl.TYPE.REQUEST);
        startPlan.setOp(PlanControl.OP.START);
        startPlan.setPlanId("follow_neptus");
        FollowReference man = new FollowReference();

        man.setDst(this.imc_address);
        man.setControlEnt((short)255);
        man.setControlSrc(65535);
        man.setAltitudeInterval(2);
        man.setTimeout(5);

        PlanSpecification spec = new PlanSpecification();

        spec.setDst(this.imc_address);
        spec.setPlanId("follow_neptus");
        spec.setStartManId("1");
        PlanManeuver pm = new PlanManeuver();
        pm.setData(man);
        pm.setManeuverId("1");
        spec.setManeuvers(Arrays.asList(pm));
        startPlan.setArg(spec);
        int reqId = 0;
        startPlan.setRequestId(reqId);
        startPlan.setFlags(0);

        startPlan.setDst(this.imc_address);

        send(systemName, startPlan);

        followReferenceIsActive = true;
    }
    public void stopFollowReferenceManeuver() {
//        DesiredPath path = new DesiredPath();
//        path.setSpeed(0);
//        path.setEndLat(getLat());
//        path.setEndLon(getLon());
//        path.setStartLat(getLat());
//        path.setStartLon(getLon());

//        send(systemName, path);

        PlanControl stop = new PlanControl();
        stop.setDst(this.imc_address);
        stop.setType(PlanControl.TYPE.REQUEST);
        stop.setOp(PlanControl.OP.STOP);
        send(systemName, stop);
        followReferenceIsActive = false;
    }

    public Reference createReference(double Latitude, double Longitude, double Speed, double Radius) {
        System.out.println("SystemName: " + systemName);

        if (systemName.equals("caravela")) {
            Speed = Math.min(Speed,3);
            Speed = Math.max(Speed,2);
            System.out.println("Caravela speed limited to: " + Speed);
        }

        if (systemName.equals("swordfish")) {
            Speed = Math.min(Speed,1.5);
            Speed = Math.max(Speed,0);
            System.out.println("Swordfish speed limited to: " + Speed);
        }

        Reference ref = new Reference();
        ref.setLat(Latitude);
        ref.setLon(Longitude);
        ref.setRadius(0);

        DesiredZ z = new DesiredZ();
        z.setValue(0);
        z.setDst(this.imc_address);
        z.setZUnits(ZUnits.DEPTH);//(DesiredZ.Z_UNITS.DEPTH);

        DesiredSpeed spd = new DesiredSpeed();
        spd.setDst(this.imc_address);
        spd.setSpeedUnits(SpeedUnits.METERS_PS);//DesiredSpeed.SPEED_UNITS.METERS_PS);
        spd.setValue(Speed);

        Short flags = Reference.FLAG_SPEED | Reference.FLAG_LOCATION | Reference.FLAG_RADIUS;

        ref.setFlags(flags);

        ref.setZ(z);
        ref.setSpeed(spd);
        ref.setDst(this.imc_address);
        //ref.setDstEnt(msg.getDstEnt());

        currentReference = ref;
        return ref;
    }

    public DesiredPath createDesiredPath(double Latitude, double Longitude, double Speed)
    {
        System.out.println("SystemName: " + systemName);

        if (systemName.equals("caravela")) {
            Speed = Math.min(Speed,20);
            Speed = Math.max(Speed,2);
            System.out.println("Caravela speed limited to: " + Speed);
        }

        DesiredPath path = new DesiredPath();

        path.setSpeed(Speed);
        path.setStartLat(Latitude);
        path.setStartLon(Longitude);
        path.setEndLat(Latitude);
        path.setEndLon(Longitude);
        path.setEndZ(0);

        return path;
    }

    @Override
    public void cleanSubPanel() {
    }

    @Override
    public void initSubPanel() {

    }

    @Override
    public boolean isExclusive() {
        return true;
    }
}
