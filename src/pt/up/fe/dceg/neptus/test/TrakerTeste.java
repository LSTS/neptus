/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by Paulo Dias 
 * 2005/12/01
 * $Id:: TrakerTeste.java 9880 2013-02-07 15:23:52Z jqcorreia             $:
 */
package pt.up.fe.dceg.neptus.test;
import java.awt.BorderLayout;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import pt.up.fe.dceg.neptus.mp.SystemPositionAndAttitude;
import pt.up.fe.dceg.neptus.renderer2d.MissionRenderer;
import pt.up.fe.dceg.neptus.types.coord.CoordinateSystem;
import pt.up.fe.dceg.neptus.types.coord.CoordinateUtil;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.types.map.MapGroup;
import pt.up.fe.dceg.neptus.types.map.MapType;
import pt.up.fe.dceg.neptus.types.map.MarkElement;
import pt.up.fe.dceg.neptus.types.map.TransponderElement;
import pt.up.fe.dceg.neptus.types.mission.MapMission;
import pt.up.fe.dceg.neptus.types.mission.MissionType;
import pt.up.fe.dceg.neptus.types.mission.VehicleMission;
import pt.up.fe.dceg.neptus.types.vehicle.VehicleType;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.MathMiscUtils;
import pt.up.fe.dceg.neptus.util.conf.ConfigFetch;

/**
 * @author Paulo
 *
 */
public class TrakerTeste extends JPanel
{

    private static final long serialVersionUID = 2846762143138236266L;
    private JFrame jFrame = null;  //  @jve:decl-index=0:visual-constraint="474,124"
    private JPanel frameContentPane = null;
    private JPanel rendererPanel = null;
    private MissionType missionType = new MissionType();
    private LinkedList<TransponderElement> transpondersList = new LinkedList<TransponderElement>();
    private LocationType locStart = new LocationType();
    private LinkedList<CoordinateSystem> coordSystemsList = new LinkedList<CoordinateSystem>();
    private LinkedList<Double> distanciesList = new LinkedList<Double>();
    private MissionRenderer missionRenderer = null;
    private VehicleType ve;
    private JButton runButton = null;
    private LocationType lastKnownPos = new LocationType();
    private JPanel controlTrackerPanel = null;
    private JButton resetButton = null;
    private MapGroup mgp;
    private LinkedList<String> addedMapsIds = new LinkedList<String>();


    /**
     * This method initializes jFrame
     *
     * @return javax.swing.JFrame
     */
    private JFrame getJFrame()
    {
        if (jFrame == null)
        {
            jFrame = new JFrame();
            jFrame.setContentPane(getFrameContentPane());
        }
        return jFrame;
    }

    /**
     * This method initializes frameContentPane
     *
     * @return javax.swing.JPanel
     */
    private JPanel getFrameContentPane()
    {
        if (frameContentPane == null)
        {
            frameContentPane = new JPanel();
            frameContentPane.setLayout(new BorderLayout());
            frameContentPane.add(this, BorderLayout.CENTER);
        }
        return frameContentPane;
    }


    /**
     * This is the default constructor
     */
    public TrakerTeste()
    {
        super();
        initTestMission();
        initialize();
    }

    /**
     * This method initializes this
     *
     * @return void
     */
    private void initialize()
    {
        this.setLayout(new BorderLayout());
        this.setSize(300, 200);
        this.setSize(300, 200);
        this.add(getRendererPanel(), java.awt.BorderLayout.CENTER);
        this.add(getControlTrackerPanel(), java.awt.BorderLayout.NORTH);
    }

    private void initTestMission()
    {
        MissionType mt = new MissionType("testeTracker/mission-tracker.xml");
        missionType = mt;
    }

    public static JFrame showFrame(String title)
    {
        TrakerTeste tr = new TrakerTeste();
        JFrame fr = tr.getJFrame();
        fr.setSize(500,400);
        fr.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        fr.setVisible(true);
        tr.work();
        return fr;
    }

    /**
     *
     */
    private void work()
    {
        ve = ((VehicleMission) missionType.getVehiclesList().values()
                .iterator().next()).getVehicle();
        LinkedHashMap<String, MapMission> mpList = missionType.getMapsList();
        LinkedHashMap<String, MapMission> mapList = mpList;
        for (MapMission mpm : mapList.values())
        {
            //LinkedHashMap traList = mpm.getMap().getTranspondersList();
            LinkedHashMap<String, TransponderElement> transList = mpm.getMap().getTranspondersList();
            for (TransponderElement tmp : transList.values())
            {
                transpondersList.add(tmp);
            }
        }

        //locStart;
        boolean isFound = false;
        for (MapMission mpm : mapList.values())
        {
            //LinkedHashMap traList = mpm.getMap().getMarksList();
            LinkedHashMap<String, MarkElement> transList = mpm.getMap().getMarksList();
            for (MarkElement tmp : transList.values())
            {
                String name = tmp.getName();
                if (name.equalsIgnoreCase("start"))
                {
                    locStart.setLocation(tmp.getCenterLocation());
                    isFound = true;
                    break;
                }
            }
            if (isFound)
                break;
        }
        if (isFound)
            lastKnownPos = new LocationType(locStart);

        //Calc the CoordenateSystems
        for (int i = 0; i < transpondersList.size(); i++)
        {
            LocationType t1 = transpondersList.get(i).getCenterLocation();
            LocationType t2;
            if (i < (transpondersList.size() -1 ))
                t2 = transpondersList.get(i+1).getCenterLocation();
            else
                t2 = transpondersList.getFirst().getCenterLocation();
            double[] res = t1.getOffsetFrom(t2);
            CoordinateUtil.cartesianToCylindricalCoordinates(res[0], res[1], res[2]);
            double distance = t1.getHorizontalDistanceInMeters(new LocationType(t2));
            double xyAngle = t1.getXYAngle(new LocationType(t2));
            CoordinateSystem cs = new CoordinateSystem();
            cs.setLocation(t1);
            cs.setYaw(Math.toDegrees(xyAngle - Math.PI/2));
            cs.setId(t1.getId() + t2.getId());
            cs.setName(cs.getId());
            coordSystemsList.add(cs);
            distanciesList.add(Double.valueOf(distance));
        }

        MapGroup mgp = MapGroup.getMapGroupInstance(missionType);
        MapType mapCS = new MapType();
        for (CoordinateSystem cs : coordSystemsList)
        {
            MarkElement mo = new MarkElement(null, null);
            mo.setCenterLocation(new LocationType(cs));
            mo.setId(cs.getId());
            mo.setName(cs.getName());
            mo.setYawDeg(cs.getYaw());
            mapCS.addObject(mo);
        }
        mgp.addMap(mapCS);
        SystemPositionAndAttitude sv = new SystemPositionAndAttitude(new LocationType(), 0., 0., 0.);
        missionRenderer.setVehicleState(ve,sv);

        /*
        UpdateTask uTask = new UpdateTask();
        uTask.init();
        Timer timer = new Timer();
        timer.schedule(new UpdateTask(), 500);
        */
    }


    /**
     * @param trans1
     * @param trans2
     * @param trans1ToVehicleDistance
     * @param trans2ToVehicleDistance
     * @return
     */
    private LocationType[] calculate(int trans1, int trans2,
            double trans1ToVehicleDistance, double trans2ToVehicleDistance)
    {
        CoordinateSystem cs = coordSystemsList.get(trans1);
        double distance = distanciesList.get(trans1);

        double da1 = trans1ToVehicleDistance;
        double db1 = trans2ToVehicleDistance;
        double paY = 0;
        double pbY = distance;

        String lat = cs.getLatitude();
        String lon = cs.getLongitude();
        double yawHR = cs.getYaw();
        double[] cyl = CoordinateUtil.sphericalToCylindricalCoordinates(cs
                .getOffsetDistance(), cs.getAzimuth(), cs.getZenith());
        double legacyOffsetDistance = MathMiscUtils.round(cyl[0], 3);
        double legacyTheta = MathMiscUtils.round(Math.toDegrees(cyl[1]), 3);
        double legacyOffsetNorth = cs.getOffsetNorth();
        double legacyOffsetEast = cs.getOffsetEast();

        double t1Depth = 0;
        double daH1 = Math.sqrt(Math.pow(da1, 2) - Math.pow(t1Depth, 2));
        double dbH1 = Math.sqrt(Math.pow(db1, 2) - Math.pow(t1Depth, 2));
        double offsetY = (Math.pow(daH1, 2) - Math.pow(dbH1, 2)
                + Math.pow(pbY, 2) - Math.pow(paY, 2))
                / (2 * pbY - 2 * paY);
        double offsetX = Math.sqrt(Math.pow(daH1, 2) - Math.pow(offsetY - paY, 2));

        double[] offsetsIne = CoordinateUtil.bodyFrameToInertialFrame(offsetX,
                offsetY, 0, 0, 0, Math.toRadians(yawHR));
        double offsetNorth = MathMiscUtils.round(offsetsIne[0], 3) + legacyOffsetNorth;
        double offsetEast = MathMiscUtils.round(offsetsIne[1], 3) + legacyOffsetEast;

        double[] offsetsIne2 = CoordinateUtil.bodyFrameToInertialFrame(-offsetX,
                offsetY, 0, 0, 0, Math.toRadians(yawHR));
        double offsetNorth2 = MathMiscUtils.round(offsetsIne2[0], 3) + legacyOffsetNorth;
        double offsetEast2 = MathMiscUtils.round(offsetsIne2[1], 3) + legacyOffsetEast;

        LocationType loc = new LocationType();
        loc.setLatitude(lat);
        loc.setLongitude(lon);
        loc.setDepth(t1Depth);
        loc.setOffsetNorth(offsetNorth);
        loc.setOffsetEast(offsetEast);
        loc.setOffsetDistance(legacyOffsetDistance);
        loc.setAzimuth(legacyTheta);

        LocationType loc2 = new LocationType();
        loc2.setLatitude(lat);
        loc2.setLongitude(lon);
        loc2.setDepth(t1Depth);
        loc2.setOffsetNorth(offsetNorth2);
        loc2.setOffsetEast(offsetEast2);
        loc2.setOffsetDistance(legacyOffsetDistance);
        loc2.setAzimuth(legacyTheta);

        LocationType[] locArray = {loc, loc2};

        return locArray;
    }

    /**
     * @param newLoc
     * @param lasKnownLoc
     * @return
     */
    public LocationType fixLocationWithLastKnown(LocationType[] newLocArray,
            LocationType lasKnownLoc)
    {
        LocationType fixedLoc = new LocationType();
        LocationType newLoc = new LocationType(newLocArray[0]);
        LocationType helperLoc = new LocationType(newLocArray[1]);
        //helperLoc
        /*
        double[] res = CoordinateUtil.sphericalToCartesianCoordinates(newLoc
                .getOffsetDistance(), newLoc.getAzimuth(), newLoc.getZenith());
        helperLoc.setOffsetDistance(0);
        helperLoc.setAzimuth(0);
        helperLoc.setZenith(90);
        helperLoc.setOffsetNorth((helperLoc.getOffsetNorth() + res[0]) * -1);
        helperLoc.setOffsetEast(helperLoc.getOffsetEast() + res[1]);
        helperLoc.setOffsetDown(helperLoc.getOffsetDown() + res[2]);
        */

        double newLocDist = lasKnownLoc.getDistanceInMeters(newLoc);
        double lasKnownLocDist = lasKnownLoc.getDistanceInMeters(helperLoc);
        if (newLocDist <= lasKnownLocDist)
        {
            fixedLoc = newLoc;
            System.out.println("" + newLocDist + " & " + lasKnownLocDist);
        }
        else
        {
            fixedLoc = helperLoc;
            System.out.println("Trocou!! " + newLocDist + " & " + lasKnownLocDist);
        }
        System.out.println("    " + newLoc.getDebugString());
        System.out.println("    " + helperLoc.getDebugString());

        return fixedLoc;
    }

    class UpdateTask
    extends TimerTask
    {
        LinkedList<DataFeed> dataFeed = new LinkedList<DataFeed>();
        int i = 0;

        public void init()
        {
            dataFeed.add(new DataFeed(0, 1, 10.9, 9.83));
            dataFeed.add(new DataFeed(1, 2, 9.83, 14.00));
            dataFeed.add(new DataFeed(2, 0, 14.0, 10.90));

            dataFeed.add(new DataFeed(0, 1, 8.83, 6.80));
            dataFeed.add(new DataFeed(1, 2, 6.80, 10.90));
            dataFeed.add(new DataFeed(2, 0, 10.90, 8.83));

            dataFeed.add(new DataFeed(0, 1, 6.99, 4.86));
            dataFeed.add(new DataFeed(1, 2, 4.86, 8.12));//2.33
            dataFeed.add(new DataFeed(2, 0, 8.12, 6.99));

            dataFeed.add(new DataFeed(0, 1, 9.0, 17.7));
            dataFeed.add(new DataFeed(1, 2, 17.7, 16.0));
            dataFeed.add(new DataFeed(2, 0, 16.0, 9.0));

            dataFeed.add(new DataFeed(0, 1, 7.98, 17.56));
            dataFeed.add(new DataFeed(1, 2, 17.56, 12.33));
            dataFeed.add(new DataFeed(2, 0, 12.33, 7.98));

            i = 0;
            System.out.println("DataFeed: " + dataFeed.size());
        }

        @Override
        public void run()
        {
            System.out.println("Tick: " + i);
            DataFeed dFeed = dataFeed.get(i++);
            LocationType[] locArray = calculate(dFeed.trans1, dFeed.trans2,
                    dFeed.dist1, dFeed.dist2);
            LocationType loc = fixLocationWithLastKnown(locArray, lastKnownPos);
            double newYaw = lastKnownPos.getXYAngle(loc);
            lastKnownPos.setLocation(loc);
            SystemPositionAndAttitude sv = new SystemPositionAndAttitude(loc, 0.0, 0.0, newYaw);
            missionRenderer.setVehicleState(ve, sv);

            mgp = MapGroup.getMapGroupInstance(missionType);
            MapType mapCS = new MapType();

            MarkElement mo = new MarkElement(null, null);
            mo.setCenterLocation(loc);
            mo.setId("tracker-" + i);
            mo.setName(mo.getId());
            mapCS.addObject(mo);

            /*
            int j = 0;
            for (LocationType locMar : locArray)
            {
                MarkObject mo = new MarkObject(null, null);
                mo.setCenterLocation(locMar);
                mo.setId("tracker-" + i + "." + j);
                mo.setName(mo.getId());
                mapCS.addObject(mo);
                j++;
            }
            */

            addedMapsIds.add(mapCS.getId());
            mgp.addMap(mapCS);
            if (dataFeed.size() == i)
                this.cancel();
        }

        class DataFeed
        {
            public int trans1, trans2;
            public double dist1, dist2;
            public DataFeed(int trans1, int trans2,
                    double trans1ToVehicleDistance, double trans2ToVehicleDistance)
            {
                this.trans1 = trans1;
                this.trans2 = trans2;
                this.dist1 = trans1ToVehicleDistance;
                this.dist2 = trans2ToVehicleDistance;
            }
        }
    }

    /*
    private void presentResults()
    {
        String res = "";
        res += "Lat: " + lat;
        res += "    Lon: " + lon;
        res += "    Depth: " + t1Depth;
        res += "\nOffset north: " + offsetNorth;
        res += "    Offset east: " + offsetEast;
        res += "\nOffset distance: " + legacyOffsetDistance;
        res += "    Azimuth: " + legacyTheta + "\u00B0";
        getResultsTextArea().setText(res);
    }
    */

    /**
     * This method initializes rendererPanel
     *
     * @return javax.swing.JPanel
     */
    private JPanel getRendererPanel()
    {
        if (rendererPanel == null)
        {
            rendererPanel = new JPanel();
            rendererPanel.setLayout(new BorderLayout());
            missionRenderer= new MissionRenderer(null,
                    MapGroup.getMapGroupInstance(missionType),
                    MissionRenderer.R2D_AND_R3D1CAM);
            rendererPanel.add(missionRenderer, BorderLayout.CENTER);
        }
        return rendererPanel;
    }

    /**
     * This method initializes runButton
     *
     * @return javax.swing.JButton
     */
    private JButton getRunButton()
    {
        if (runButton == null)
        {
            runButton = new JButton();
            runButton.setText("Simulate tracker");
            runButton.addActionListener(new java.awt.event.ActionListener()
                {
                    public void actionPerformed(java.awt.event.ActionEvent e)
                    {
                        UpdateTask uTask = new UpdateTask();
                        uTask.init();
                        Timer timer = new Timer("Teste");
                        timer.scheduleAtFixedRate(uTask, 1000, 1000);
                    }
                });
        }
        return runButton;
    }

    /**
     * This method initializes controlTrackerPanel
     *
     * @return javax.swing.JPanel
     */
    private JPanel getControlTrackerPanel()
    {
        if (controlTrackerPanel == null)
        {
            controlTrackerPanel = new JPanel();
            controlTrackerPanel.add(getRunButton(), null);
            controlTrackerPanel.add(getResetButton(), null);
        }
        return controlTrackerPanel;
    }

    /**
     * This method initializes resetButton
     *
     * @return javax.swing.JButton
     */
    private JButton getResetButton()
    {
        if (resetButton == null)
        {
            resetButton = new JButton();
            resetButton.setText("Reset");
            resetButton.addActionListener(new java.awt.event.ActionListener()
                {
                    public void actionPerformed(java.awt.event.ActionEvent e)
                    {
                        lastKnownPos = new LocationType(locStart);
                        SystemPositionAndAttitude sv = new SystemPositionAndAttitude(lastKnownPos, 0., 0., 0.);
                        missionRenderer.setVehicleState(ve,sv);

                        for (String mapId : addedMapsIds)
                        {
                            mgp.removeMap(mapId);
                        }
                        addedMapsIds.clear();
                    }
                });
        }
        return resetButton;
    }

    public static void main(String[] args)
    {
        ConfigFetch.initialize();
        GuiUtils.setLookAndFeel();
        showFrame("Tracker teste");
    }
}
