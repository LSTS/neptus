/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by pdias
 * 16 de Mai de 2012
 */
package pt.up.fe.dceg.neptus.util.lbl;

import java.util.LinkedList;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.types.coord.CoordinateSystem;
import pt.up.fe.dceg.neptus.types.coord.CoordinateUtil;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.types.map.TransponderElement;
import pt.up.fe.dceg.neptus.util.MathMiscUtils;

/**
 * @author pdias
 * 
 */
public class LBLTriangulationHelper {

    /**
     * Ordered transponders to use
     */
    private final LinkedList<TransponderElement> transponders = new LinkedList<TransponderElement>();
    private final LinkedList<CoordinateSystem> coordSystemsList = new LinkedList<CoordinateSystem>();
    private final LinkedList<Double> distanciesList = new LinkedList<Double>();

    private double[] distances = { Double.NaN, Double.NaN, Double.NaN };
    private double[] distancesPrev = { Double.NaN, Double.NaN, Double.NaN };

    private final LocationType lastKnownPos = new LocationType();
    private long lastCalcPosTimeMillis = -1;

    private LocationType start = new LocationType();

    /**
     * @param transpondersArray Ordered transponders to use
     * @param start Start location to disambiguate the side of the baseline
     * @throws Exception
     */
    public LBLTriangulationHelper(TransponderElement[] transpondersArray,
            LocationType start) throws Exception {
        reset(transpondersArray, start);
    }

    /**
     * 
     */
    private void initialize() {
        calcCoordinateSystems(transponders, coordSystemsList, distanciesList);
        resetDistArray();
    }

    /**
     * Used to reset the values and transponders list.
     * @throws Exception 
     * 
     */
    public void reset(TransponderElement[] transpondersArray,
            LocationType start) throws Exception {
        if (transpondersArray == null || transpondersArray.length == 1)
            throw new Exception("Number of Transponders should be grater than 1!");
        for (TransponderElement transponderElement : transpondersArray) {
            this.transponders.add(transponderElement);
        }
        this.start = start;
        initialize();
    }

    /**
     * Used to reset the values.
     */
    public void reset() {
        initialize();
    }

    private void resetDistArray() {
        // Init distances array
        int ts = transponders.size();
        if (ts > 0) {
            distances = new double[ts];
            distancesPrev = new double[ts];
            for (int i = 0; i < distances.length; i++) {
                distances[i] = Double.NaN;
                distancesPrev[i] = Double.NaN;
            }
        }
        lastKnownPos.setLocation(start);
    }

    /**
     * @return the lastKnownPos
     */
    public LocationType getLastKnownPos() {
        return new LocationType(lastKnownPos);
    }

    /**
     * This will update the last known position to be used. 
     */
    public void resetLastKnownPos(LocationType lastKnownPos, long lastKnownPosMillis) {
        this.lastKnownPos.setLocation(lastKnownPos);
        this.lastCalcPosTimeMillis = lastKnownPosMillis;
    }

    /**
     * @return the time millis of calculation
     */
    public long getLastKnownPosMillis() {
        return lastCalcPosTimeMillis;
    }
    
    /**
     * Used for a valid range.
     * @param id Of the transponder
     * @param range the range in meters
     * @param timeStampMillis the timestamp in millis of the range
     * @return the calculated location or null if the calculation was not possible.
     */
    public LocationType updateRangeAccepted(long id, double range, long timeStampMillis) {

        // playSound((int) id, true);
        LocationType loc = null;
        try {
            int nTrans = transponders.size();
            int indOrigin = (int) id;
            if (indOrigin == -1) {
                NeptusLog.pub().debug(this + "\nTransponder " + id + " not found in list!");
                return null;
            }
            int indPrevOrigin = (indOrigin - 1) % nTrans;
            if (indPrevOrigin == -1)
                indPrevOrigin = nTrans - 1;
            loc = updatePosition(indPrevOrigin, indOrigin, distances[indPrevOrigin], range /*distances[indOrigin]*/,
                    coordSystemsList.get(indPrevOrigin), distanciesList.get(indPrevOrigin), lastKnownPos, start,
                    timeStampMillis);
        }
        catch (Exception e) {
            NeptusLog.pub().error(e.getMessage(), e);
        }

        // LBLRangeLabel lb = beacons.get("" + id);
        // if (lb == null) {
        // lb = new LBLRangeLabel("" + id);
        // beacons.put("" + id, lb);
        // holder.add(lb);
        // }
        // lb.setRange(range);
        // lb.setTimeStampMillis(timeStampMillis);
        // lb.setAccepted(true, null);

        int idx = Integer.parseInt(("" + id).replaceAll("ch", ""));
        // paintRange(idx, range, true, null);
        distancesPrev[idx] = distances[idx];
        distances[idx] = range;
        return loc;
    }

    public LocationType updateRange(long id, double range, long timeStampMillis, String reason) {
        if (reason.length() == 0) {
            return updateRangeAccepted(id, range, timeStampMillis);
        }
        else {
            return updateRangeRejected(id, range, timeStampMillis, reason);
        }
    }

    /**
     * Used for an invalid range.
     * @param id Of the transponder
     * @param range the range in meters
     * @param timeStampMillis the timestamp in millis of the range
     * @return the calculated location or null if the calculation was not possible.
     */
    public LocationType updateRangeRejected(long id, double range, long timeStampMillis, String reason) {

        // playSound((int) id, false);
        LocationType loc = null;

        try {
            int nTrans = transponders.size();
            int indOrigin = (int) id;
            int indPrevOrigin = (indOrigin - 1) % nTrans;
            if (indPrevOrigin == -1)
                indPrevOrigin = nTrans - 1;
            loc = updatePosition(indPrevOrigin, indOrigin, distances[indPrevOrigin], distances[indOrigin],
                    coordSystemsList.get(indPrevOrigin), distanciesList.get(indPrevOrigin), lastKnownPos, start,
                    timeStampMillis);
        }
        catch (NumberFormatException e) {
            e.printStackTrace();
        }

        // LBLRangeLabel lb = beacons.get("" + id);
        //
        // if (lb == null) {
        // lb = new LBLRangeLabel("" + id);
        // beacons.put("" + id, lb);
        // holder.add(lb);
        // }
        // lb.setRange(range);
        // lb.setTimeStampMillis(timeStampMillis);
        // lb.setAccepted(false, reason);

        int idx = Integer.parseInt(("" + id).replaceAll("ch", ""));
        // paintRange(idx, range, false, reason);
        distances[idx] = Double.NaN;
        return loc;
    }

    /**
     * @param transpondersList A Transponder list ordered.
     * @param coordSystemsList This will be used as output, it will be cleared.
     * @param distanciesList This will be used as output, it will be cleared.
     */
    private void calcCoordinateSystems(LinkedList<TransponderElement> transpondersList,
            LinkedList<CoordinateSystem> coordSystemsList, LinkedList<Double> distanciesList) {
        coordSystemsList.clear();
        distanciesList.clear();

        // Calculate the CoordinateSystems
        for (int i = 0; i < transpondersList.size(); i++) {
            LocationType t1 = transpondersList.get(i).getCenterLocation();
            LocationType t2;
            if (i < (transpondersList.size() - 1))
                t2 = transpondersList.get(i + 1).getCenterLocation();
            else
                t2 = transpondersList.getFirst().getCenterLocation();
            double[] res = t1.getOffsetFrom(t2);
            CoordinateUtil.cartesianToCylindricalCoordinates(res[0], res[1], res[2]);
            double distance = t1.getHorizontalDistanceInMeters(new LocationType(t2));
            double xyAngle = t1.getXYAngle(new LocationType(t2));
            CoordinateSystem cs = new CoordinateSystem();
            cs.setLocation(t1);
            cs.setYaw(Math.toDegrees(xyAngle - Math.PI / 2));
            cs.setId(t1.getId() + t2.getId());
            cs.setName(cs.getId());
            coordSystemsList.add(cs);
            distanciesList.add(Double.valueOf(distance));
        }
    }

    private LocationType updatePosition(int trans1, int trans2, double trans1ToVehicleDistance,
            double trans2ToVehicleDistance, CoordinateSystem cs, double distance, LocationType lastKnownPos,
            LocationType start, long timeStampMillis) {
        try {
            LocationType[] locArray = calculate(trans1, trans2, trans1ToVehicleDistance, trans2ToVehicleDistance, cs,
                    distance);
            if (locArray == null) {
                NeptusLog.pub().info("LBL Range updatePosition" + "\nInvalid fix for calculation!!");
                return null;
            }
            LocationType loc = fixLocationWithLastKnown(locArray, lastKnownPos, start);
            lastKnownPos.setLocation(loc);
            lastCalcPosTimeMillis = timeStampMillis;
            return loc;
        }
        catch (Exception e) {
            NeptusLog.pub().error(e.getMessage(), e);
            return null;
        }
    }

    private LocationType[] calculate(int trans1, int trans2, double trans1ToVehicleDistance,
            double trans2ToVehicleDistance, CoordinateSystem cs, double distance) {
        // CoordinateSystem cs = coordSystemsList.get(trans1);
        // double distance = distanciesList.get(trans1);

        double da1 = trans1ToVehicleDistance;
        double db1 = trans2ToVehicleDistance;
        double paY = 0;
        double pbY = distance;

        String lat = cs.getLatitude();
        String lon = cs.getLongitude();
        double yawHR = cs.getYaw();
        double[] cyl = CoordinateUtil.sphericalToCylindricalCoordinates(cs.getOffsetDistance(), cs.getAzimuth(),
                cs.getZenith());
        double legacyOffsetDistance = MathMiscUtils.round(cyl[0], 3);
        double legacyTheta = MathMiscUtils.round(Math.toDegrees(cyl[1]), 3);
        double legacyOffsetNorth = cs.getOffsetNorth();
        double legacyOffsetEast = cs.getOffsetEast();

        double t1Depth = 0;
        double daH1 = Math.sqrt(Math.pow(da1, 2) - Math.pow(t1Depth, 2));
        double dbH1 = Math.sqrt(Math.pow(db1, 2) - Math.pow(t1Depth, 2));
        double offsetY = (Math.pow(daH1, 2) - Math.pow(dbH1, 2) + Math.pow(pbY, 2) - Math.pow(paY, 2))
                / (2 * pbY - 2 * paY);
        double offsetX = Math.sqrt(Math.pow(daH1, 2) - Math.pow(offsetY - paY, 2));

//        System.out.println("\n....... offsetX= " + offsetX + "    offsetY= " + offsetY);
        if (Double.isNaN(offsetX) || Double.isNaN(offsetY))
            return null;

        double[] offsetsIne = CoordinateUtil.bodyFrameToInertialFrame(offsetX, offsetY, 0, 0, 0, Math.toRadians(yawHR));
        double offsetNorth = MathMiscUtils.round(offsetsIne[0], 3) + legacyOffsetNorth;
        double offsetEast = MathMiscUtils.round(offsetsIne[1], 3) + legacyOffsetEast;

        double[] offsetsIne2 = CoordinateUtil.bodyFrameToInertialFrame(-offsetX, offsetY, 0, 0, 0,
                Math.toRadians(yawHR));
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

        LocationType[] locArray = { loc, loc2 };

        return locArray;
    }

    private LocationType fixLocationWithLastKnown(LocationType[] newLocArray, LocationType lasKnownLoc,
            LocationType startPos) {
        // FIXME hardcoded
//        lasKnownLoc = startPos;

        LocationType fixedLoc = new LocationType();
        LocationType newLoc = new LocationType(newLocArray[0]);
        LocationType helperLoc = new LocationType(newLocArray[1]);

        double newLocDist = lasKnownLoc.getDistanceInMeters(newLoc);
        double lasKnownLocDist = lasKnownLoc.getDistanceInMeters(helperLoc);
        if (newLocDist <= lasKnownLocDist) {
            fixedLoc = newLoc;
//            System.out.println(newLocDist + " & " + lasKnownLocDist);
        }
        else {
            fixedLoc = helperLoc;
//            System.out.println("\nTrocou!! " + newLocDist + " & " + lasKnownLocDist);
        }
//        System.out.println("\n    " + newLoc.getDebugString());
//        System.out.println("\n    " + helperLoc.getDebugString());

        return fixedLoc;
    }

    // /**
    // *
    // */
    // private static TransponderElement[] orderTransponders(TransponderElement[] transList) {
    // ArrayList<TransponderElement> tal = new ArrayList<TransponderElement>();
    // tal.addAll(Arrays.asList(transList));
    // // Let us order the beacons in alphabetic order (case insensitive)
    // Collections.sort(tal, new Comparator<TransponderElement>() {
    // @Override
    // public int compare(TransponderElement o1, TransponderElement o2) {
    // return o1.getId().compareToIgnoreCase(o2.getId());
    // }
    // });
    // return tal.toArray(new TransponderElement[tal.size()]);
    // }

    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub

    }
}
