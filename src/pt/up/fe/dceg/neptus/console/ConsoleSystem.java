/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by rjpg
 * 200?/??/??
 */
package pt.up.fe.dceg.neptus.console;

import java.util.Vector;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.console.plugins.MissionChangeListener;
import pt.up.fe.dceg.neptus.console.plugins.SubPanelChangeEvent;
import pt.up.fe.dceg.neptus.console.plugins.SubPanelChangeListener;
import pt.up.fe.dceg.neptus.imc.EstimatedState;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.messages.listener.MessageInfo;
import pt.up.fe.dceg.neptus.messages.listener.MessageListener;
import pt.up.fe.dceg.neptus.mp.SystemPositionAndAttitude;
import pt.up.fe.dceg.neptus.renderer2d.VehicleStateListener;
import pt.up.fe.dceg.neptus.types.coord.CoordinateSystem;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.types.map.MapGroup;
import pt.up.fe.dceg.neptus.types.map.MapType;
import pt.up.fe.dceg.neptus.types.map.ScatterPointsElement;
import pt.up.fe.dceg.neptus.types.mission.MissionType;
import pt.up.fe.dceg.neptus.types.vehicle.VehicleType;
import pt.up.fe.dceg.neptus.types.vehicle.VehiclesHolder;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcMsgManager;
import pt.up.fe.dceg.neptus.util.conf.GeneralPreferences;
import pt.up.fe.dceg.neptus.util.conf.PreferencesListener;

/**
 * This class centralize vehicles info and it's trees for the console panels variables feed and external render.
 * 
 * @author Rui Gonçalves
 * @author Paulo Dias
 */
public class ConsoleSystem implements MissionChangeListener, PreferencesListener, SubPanelChangeListener,
        MessageListener<MessageInfo, IMCMessage> {

    public final int POS_ONLY_XYZ = 0; // only fields x, y, z have meaninful values
    public final int POS_ONLY_LAT_LON = 1; // only fields lat, lon, depth have meaninful values
    public final int POS_LAT_LON_XYZ = 2; // fields x, y, z, lat, lon, depth have meaninful values

    protected VehicleType vehicle;
    private ImcMsgManager imc;
    protected SystemPositionAndAttitude state;
    protected ConsoleLayout console;
    private LocationType vehicleHomeRef;
    private CoordinateSystem homeRef;

    private boolean neptusCommunications = false;

    protected Vector<VehicleStateListener> feedRenders = new Vector<VehicleStateListener>();

    protected int minDelay = -1;
    protected long lastVehicleState = -1;

    // (pdias) Used to draw the tail of the vehicle movement
    protected MapType mapCS = new MapType();
    protected ScatterPointsElement scatter = null;
    private MissionType missionType = new MissionType();
    protected boolean tailOn = false;
    private int numberOfShownPoints = 500;

    public ConsoleSystem(String id, ConsoleLayout console, VehicleType vehicle, ImcMsgManager imcMsgManager) {
        this.imc = imcMsgManager;
        this.console = console;
        this.vehicle = vehicle;
        state = new SystemPositionAndAttitude(new LocationType(), 0, 0, 0);
        if (console.getMission() != null) {
            try {
                vehicleHomeRef = (console.getMission().getVehiclesList().get(getVehicleId())).getCoordinateSystem();
            }
            catch (Exception e) {
                NeptusLog.pub().info(this + " getting vehicleHomeRef " + getVehicleId());
                vehicleHomeRef = console.getMission().getHomeRef();
            }
            state = new SystemPositionAndAttitude(new LocationType(vehicleHomeRef), 0, 0, 0);
            homeRef = console.getMission().getHomeRef();
            this.missionType = console.getMission();
        }
        else {
            // System.err.println("o veiculo não estava na missão");
            state = new SystemPositionAndAttitude(new LocationType(), 0, 0, 0);
            missionType = new MissionType();
        }
        this.console.addMissionListener(this);
        this.console.addSubPanelListener(this);
        preferencesUpdated();
        GeneralPreferences.addPreferencesListener(this);

        MapGroup.getMapGroupInstance(missionType).addMap(mapCS);
        NeptusLog.pub()
                .warn(this.getClass().getSimpleName() + " [" + this.hashCode() + "] started for vehicle "
                        + vehicle.getName());

    }

    public ConsoleSystem enableIMC() {
        if (neptusCommunications == false) {
            if (vehicle == null)
                System.out.println("vehicle null");
            if (imc == null)
                System.out.println("imc null");
            this.imc.addListener(this, vehicle.getId());
            neptusCommunications = true;
        }
        return this;
    }

    public ConsoleSystem disableIMC() {
        if (neptusCommunications == true) {
            this.imc.removeListener(this, vehicle.getId());
            neptusCommunications = false;
        }
        return this;
    }

    public ConsoleSystem toggleIMC() {
        if (neptusCommunications) {
            this.imc.removeListener(this, vehicle.getId());
            neptusCommunications = false;
        }
        else {
            this.imc.addListener(this, vehicle.getId());
            neptusCommunications = true;
        }
        return this;
    }

    public void clean() {
        feedRenders.clear();

        this.disableIMC();
        GeneralPreferences.removePreferencesListener(this);

        MapGroup mgp = MapGroup.getMapGroupInstance(missionType);

        if (mapCS != null)
            mgp.removeMap(mapCS.getId());
        mapCS = null;

        console.removeMissionListener(this);
        console.removeSubPanelListener(this);
        NeptusLog.pub().warn(
                this.getClass().getSimpleName() + " [" + this.hashCode() + "] shutdown for vehicle "
                        + vehicle.getName());
    }

    public boolean isTranslateClicle() {
        return false;
    }

    public String getVehicleId() {
        if (vehicle != null)
            return vehicle.getId();
        else
            return null;
    }

    @Override
    public void onMessage(MessageInfo info, IMCMessage msg) {

        if (msg.getAbbrev().equals("EstimatedState")) {

            EstimatedState es = (EstimatedState) msg;
            LocationType loc = new LocationType();
            loc.setLatitudeRads(es.getLat());
            loc.setLongitudeRads(es.getLon());
            loc.setHeight(loc.getHeight());

            double phi = es.getPhi();
            double theta = es.getTheta();
            double psi = es.getPsi();

            double p = es.getP();
            double q = es.getQ();
            double r = es.getR();

            double u = es.getU();
            double v = es.getV();
            double w = es.getW();

            double vx = es.getVx();
            double vy = es.getVy();
            double vz = es.getVz();

            loc.translatePosition(es.getX(), es.getY(), es.getZ());

            double time = info.getTimeReceivedNanos() / 1000000; // milliseconds

            state.setPosition(loc);
            state.setRoll(phi);
            state.setPitch(theta);
            state.setYaw(psi);
            state.setUVW(u, v, w);
            state.setPQR(p, q, r);
            state.setVxyz(vx, vy, vz);

            if (time <= 0)
                state.setTime(System.currentTimeMillis());
            else
                state.setTime((long) time);

            long previousVehicleState = lastVehicleState;

            VehicleType ve = VehiclesHolder.getVehicleById(getVehicleId());
            if (!((minDelay > 0) && (System.currentTimeMillis() - previousVehicleState < minDelay))) {
                lastVehicleState = System.currentTimeMillis();

                for (VehicleStateListener j : feedRenders) {
                    j.setVehicleState(ve, state);
                }

                if (isTailOn()) {
                    if (scatter == null) {
                        scatter = new ScatterPointsElement(MapGroup.getMapGroupInstance(missionType), mapCS);
                        scatter.setCenterLocation(new LocationType(loc));
                        mapCS.addObject(scatter);
                    }

                    double[] distFromRef = loc.getOffsetFrom(scatter.getCenterLocation());
                    scatter.addPoint(distFromRef[0], distFromRef[1], distFromRef[2]);

                }
                else {
                    if (scatter != null)
                        scatter.clearPoints();
                }
            }
        }
    }

    public SystemPositionAndAttitude getState() {
        return state;
    }

    /**
     * @return the tailOn
     */
    public boolean isTailOn() {
        return tailOn;
    }

    /**
     * @param tailOn the tailOn to set
     */
    public void setTailOn(boolean tailOn) {
        this.tailOn = tailOn;
    }

    public void addRenderFeed(VehicleStateListener mr) {
        if (!feedRenders.contains(mr))
            feedRenders.add(mr);
        // System.err.println("foi chamdo o add render e está com :"+feedRenders.size());
    }

    public void removeRenderFeed(VehicleStateListener mr) {
        feedRenders.remove(mr);
    }

    public VehicleType getVehicle() {
        return vehicle;
    }

    public boolean isNeptusCommunications() {
        return neptusCommunications;
    }

    public boolean getNeptusCommunications() {
        return neptusCommunications;
    }

    @Override
    public void missionReplaced(MissionType mission) {
        state = new SystemPositionAndAttitude(new LocationType(), 0, 0, 0);
        if (console.getMission() != null) {
            try {
                vehicleHomeRef = (console.getMission().getVehiclesList().get(getVehicleId())).getCoordinateSystem();
            }
            catch (Exception e) {
                NeptusLog.pub().warn(this + " getting vehicleHomeRef " + getVehicleId());
                vehicleHomeRef = console.getMission().getHomeRef();
            }
            state = new SystemPositionAndAttitude(new LocationType(vehicleHomeRef), 0, 0, 0);
            homeRef = console.getMission().getHomeRef();
        }
        else {
            // System.err.println("o veiculo não estava na missão");
            state = new SystemPositionAndAttitude(new LocationType(), 0, 0, 0);
        }
        this.missionType = mission;
        if (scatter != null) {
            mapCS.remove(scatter.getId());
            scatter = null;
        }

        MapGroup.getMapGroupInstance(mission).addMap(mapCS);
    }

    public LocationType getVehicleHomeRef() {
        return vehicleHomeRef;
    }

    public LocationType getHomeRef() {
        return homeRef;
    }

    public void preferencesUpdated() {
        // try {
        // minDelay = GeneralPreferences.getPropertyInteger(GeneralPreferences.RENDERER_UPDATE_VEHICLE_STATE);
        // }
        // catch (GeneralPreferencesException e) {
        // minDelay = -1;
        // }
        minDelay = GeneralPreferences.rendererUpdatePeriodeForVehicleStateMillis;

        int np = 500;
        if (scatter != null) {
            // try {
            // np = GeneralPreferences.getPropertyInteger(GeneralPreferences.NUMBER_OF_SHOWN_POINTS);
            // }
            // catch (GeneralPreferencesException e) {
            // e.printStackTrace();
            // }
            np = GeneralPreferences.numberOfShownPoints;
            scatter.setNumberOfPoints(np);
            if (np < 0)
                numberOfShownPoints = ScatterPointsElement.INFINITE_NUMBER_OF_POINTS;
            else
                numberOfShownPoints = np;
        }
    }

    public int getNumberOfShownPoints() {
        return numberOfShownPoints;
    }

    @Override
    public void missionUpdated(MissionType mission) {
    }

    @Override
    public void subPanelChanged(SubPanelChangeEvent panelChange) {
        if (VehicleStateListener.class.isAssignableFrom(panelChange.getPanel().getClass())) {

            if (panelChange.removed()) {
                feedRenders.remove(panelChange.getPanel());
            }
            if (panelChange.added()) {
                feedRenders.add((VehicleStateListener) panelChange.getPanel());
            }
        }
    }
}