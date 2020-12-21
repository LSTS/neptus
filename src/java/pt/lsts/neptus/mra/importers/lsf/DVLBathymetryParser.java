/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
 * Laboratório de Sistemas e Tecnologia Subaquática (LSTS)
 * All rights reserved.
 * Rua Dr. Roberto Frias s/n, sala I203, 4200-465 Porto, Portugal
 *
 * This file is part of Neptus, Command and Control Framework.
 *
 * Commercial Licence Usage
 * Licencees holding valid commercial Neptus licences may use this file
 * in accordance with the commercial licence agreement provided with the
 * Software or, alternatively, in accordance with the terms contained in a
 * written agreement between you and Universidade do Porto. For licensing
 * terms, conditions, and further information contact lsts@fe.up.pt.
 *
 * Modified European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the Modified EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://github.com/LSTS/neptus/blob/develop/LICENSE.md
 * and http://ec.europa.eu/idabc/eupl.html.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: zp
 * Jan 14, 2014
 */
package pt.lsts.neptus.mra.importers.lsf;

import java.util.Vector;

import javax.swing.JFileChooser;
import javax.vecmath.Point3d;

import pt.lsts.imc.DeviceState;
import pt.lsts.imc.Distance;
import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.lsf.LsfIndex;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.mra.api.BathymetryInfo;
import pt.lsts.neptus.mra.api.BathymetryParser;
import pt.lsts.neptus.mra.api.BathymetryPoint;
import pt.lsts.neptus.mra.api.BathymetrySwath;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.renderer3d.Util3D;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * @author zp
 * 
 */
public class DVLBathymetryParser implements BathymetryParser {

    private LsfIndex idx;
    private long first, last;
    private BathymetryInfo info;
    private Vector<Integer> beamIds = new Vector<>();

    int curIdx = 0;

    public DVLBathymetryParser(IMraLogGroup source) {
        this.idx = source.getLsfIndex();

        initialize();
    }

    public DVLBathymetryParser(LsfIndex index) {
        this.idx = index;

        initialize();
    }

    @Override
    public long getFirstTimestamp() {
        return first;
    }

    @Override
    public long getLastTimestamp() {
        return last;
    }

    @Override
    public synchronized BathymetryInfo getBathymetryInfo() {

        if (info.totalNumberOfPoints == -1) {

        }
        return info;
    }

    private synchronized void initialize() {
        IMCMessage fstm = idx.getMessage(idx.getFirstMessageOfType("Distance"));
        IMCMessage lstm = idx.getMessage(idx.getLastMessageOfType("Distance"));

        beamIds.add(idx.getEntityId("DVL Beam 0"));
        beamIds.add(idx.getEntityId("DVL Beam 1"));
        beamIds.add(idx.getEntityId("DVL Beam 2"));
        beamIds.add(idx.getEntityId("DVL Beam 3"));

        if (fstm != null)
            first = fstm.getTimestampMillis();
        if (lstm != null)
            last = lstm.getTimestampMillis();

        info = new BathymetryInfo();

        // LocationType topLeft, bottomRight;
        double minLat = Double.MAX_VALUE, maxLat = -Double.MAX_VALUE, minLon = Double.MAX_VALUE, maxLon = -Double.MAX_VALUE;
        double maxAlt = 0, maxDepth = 0;

        for (EstimatedState s : idx.getIterator(EstimatedState.class, 1000)) {
            LocationType loc = IMCUtils.parseLocation(s).convertToAbsoluteLatLonDepth();
            minLat = Math.min(minLat, loc.getLatitudeDegs());
            minLon = Math.min(minLon, loc.getLongitudeDegs());
            maxLat = Math.max(maxLat, loc.getLatitudeDegs());
            maxLon = Math.max(maxLon, loc.getLongitudeDegs());
            maxAlt = Math.max(maxAlt, s.getAlt());
            maxDepth = Math.max(maxDepth, s.getDepth());
        }

        if (maxAlt > 110) {
            maxAlt = 50;
        }
        double margin = Math.cos(Math.toRadians(22.5)) * maxAlt;

        LocationType topLeft = new LocationType(maxLat, minLon);
        LocationType bottomRight = new LocationType(minLat, maxLon);

        topLeft.translatePosition(margin, -margin, 0);
        bottomRight.translatePosition(-margin, margin, 0);

        info.topLeft = topLeft.convertToAbsoluteLatLonDepth();
        info.bottomRight = bottomRight.convertToAbsoluteLatLonDepth();
        info.maxDepth = (float) maxDepth;
        for (Distance d : idx.getIterator(Distance.class)) {
            if (beamIds.contains((int) d.getSrcEnt()))
                info.totalNumberOfPoints++;
        }
        // System.out.println("#points: "+info.totalNumberOfPoints);
    }

    @Override
    public BathymetrySwath getSwathAt(long timestamp) {
        int cur = curIdx;
        curIdx = idx.advanceToTime(0, timestamp / 1000.0);
        if (curIdx == -1) {
            curIdx = cur;
            return null;
        }
        else
            return nextSwath();
    }

    @Override
    public BathymetrySwath nextSwath() {
        return nextSwath(1.0);
    }

    @Override
    public BathymetrySwath nextSwath(double prob) {

        int[] msgs = new int[4];
        double distances[] = new double[4];
        double angles[] = new double[4];
        DeviceState devState = null;

        int count = 0;
        while (count < 4 && curIdx != -1 && curIdx < idx.getNumberOfMessages()) {
            curIdx = idx.getNextMessageOfType(Distance.ID_STATIC, curIdx);
            int eid = idx.entityOf(curIdx);
            int bid = beamIds.indexOf(eid);
            if (bid != -1 && msgs[bid] == 0) {
                msgs[bid] = curIdx;
                count++;
                try {
                    if (devState == null)
                        devState = idx.getMessage(curIdx, Distance.class).getLocation().firstElement();
                }
                catch (Exception e) {
                    NeptusLog.pub().error(e);
                }
            }
        }

        if (count < 4)
            return null;

        try {
            for (int i = 0; i < 4; i++) {
                Distance d = idx.getMessage(msgs[i], Distance.class);
                distances[i] = d.getValue();
                angles[i] = Math.PI + Math.PI / 2 * i;// d.getLocation().firstElement().getTheta();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        IMCMessage state = idx.getMessageAtOrAfter("EstimatedState", msgs[0], idx.timeOf(msgs[0]));

        if (state == null)
            return null;
        BathymetryPoint[] data = new BathymetryPoint[4];
        SystemPositionAndAttitude pose = IMCUtils.parseState(state);
        for (int i = 0; i < 4; i++) {
            float[] offsets = getOffsets(distances[i], angles[i], pose);
            data[i] = new BathymetryPoint(offsets[0], offsets[1], offsets[2]);
        }

        return new BathymetrySwath(state.getTimestampMillis(), IMCUtils.parseState(state), data);

    }

    @Override
    public void rewind() {
        curIdx = 0;
    }

    @Override
    public boolean getHasIntensity() {
        return false;
    }

    static float[] getOffsets(double range, double angle, SystemPositionAndAttitude pose) {
        // init vector as front looking beam vector
        Point3d pt0 = new Point3d(Math.cos(22) * range, 0, range);
        // rotate to correct beam angle
        if (angle != 0)
            pt0 = Util3D.setTransform(pt0, 0, 0, angle);

        // apply pose rotation
        // pt0 = Util3D.setTransform(pt0, pose.getRoll(), pose.getPitch(), Math.PI/2+pose.getYaw());
        pt0 = Util3D.setTransform(pt0, pose.getRoll(), pose.getPitch(), pose.getYaw() + Math.PI / 2);
        // translate using depth
        return new float[] { (float) pt0.x, (float) pt0.y, (float) (pt0.z + pose.getPosition().getDepth()) };

    }

    public static void main(String[] args) throws Exception {
        JFileChooser chooser = new JFileChooser();
        chooser.showOpenDialog(null);
        LsfIndex idx = new LsfIndex(chooser.getSelectedFile());
        DVLBathymetryParser parser = new DVLBathymetryParser(idx);
        BathymetrySwath swath = parser.nextSwath();
        while (swath != null) {
            for (BathymetryPoint bp : swath.getData()) {
                System.out.println(bp.north + ", " + bp.east + ", " + bp.depth);
            }
            swath = parser.nextSwath();
            System.out.println();
        }
    }
}
