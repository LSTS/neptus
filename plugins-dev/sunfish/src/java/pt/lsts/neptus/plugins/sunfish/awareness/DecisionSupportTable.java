/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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
 * Mar 24, 2014
 */
package pt.lsts.neptus.plugins.sunfish.awareness;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.swing.table.AbstractTableModel;

import pt.lsts.neptus.mystate.MyState;
import pt.lsts.neptus.util.DateTimeUtil;

/**
 * @author zp
 * 
 */
public class DecisionSupportTable extends AbstractTableModel {

    private static final long serialVersionUID = 1282602415878612300L;
    private double shipSpeed = 10, auvSpeed = 1.25, shipSafetyDistance = 3000;
    private final int colTagName = 0, colDistAuv = 1, colDistShip = 2, colAuvTime = 3, colAuvAndShipTime = 4,
            colAge = 5, colAccuracy = 6, colResponseTime = 7;
    private ArrayList<AssetPosition> tags;
    private AssetPosition auv;

    /**
     * @return the shipSpeed
     */
    public double getShipSpeed() {
        return shipSpeed;
    }

    /**
     * @param shipSpeed the shipSpeed to set
     */
    public void setShipSpeed(double shipSpeed) {
        this.shipSpeed = shipSpeed;
        fireTableDataChanged();
    }

    /**
     * @return the auvSpeed
     */
    public double getAuvSpeed() {
        return auvSpeed;
    }

    /**
     * @param auvSpeed the auvSpeed to set
     */
    public void setAuvSpeed(double auvSpeed) {
        this.auvSpeed = auvSpeed;
        fireTableDataChanged();
    }

    /**
     * @return the shipSafetyDistance
     */
    public double getShipSafetyDistance() {
        return shipSafetyDistance;
    }

    /**
     * @param shipSafetyDistance the shipSafetyDistance to set
     */
    public void setShipSafetyDistance(double shipSafetyDistance) {
        this.shipSafetyDistance = shipSafetyDistance;
        fireTableDataChanged();
    }

    public void setAssets(AssetPosition auv, List<AssetPosition> tags) {
        this.auv = auv;
        this.tags = new ArrayList<AssetPosition>(tags);
        fireTableStructureChanged();
    }

    @Override
    public int getColumnCount() {
        return 8;
    }

    @Override
    public int getRowCount() {
        return tags.size();
    }

    private long auvTime(AssetPosition tag) {
        double dist = tag.getLoc().getDistanceInMeters(auv.getLoc());
        return (long) ((dist / auvSpeed) * 1000.0);
    }

    private long shipAuvTime(AssetPosition tag) {
        double auvShipDist = auv.getLoc().getDistanceInMeters(MyState.getLocation());
        double auvTagDist = auv.getLoc().getDistanceInMeters(tag.getLoc());
        double time = auvShipDist / shipSpeed;
        auvTagDist -= shipSafetyDistance;
        auvTagDist = Math.min(shipSafetyDistance, auvTagDist);
        time += auvTagDist / shipSpeed;
        time += shipSafetyDistance / auvSpeed;
        return (long) (time * 1000.0);
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        AssetPosition tag = tags.get(rowIndex);

        switch (columnIndex) {
            case colTagName:
                return tag.getAssetName();
            case colDistAuv:
                return new Distance(tag.getLoc().getDistanceInMeters(auv.getLoc()));
            case colDistShip:
                return new Distance(tag.getLoc().getDistanceInMeters(MyState.getLocation()));
            case colAuvTime:
                return new EllapsedTime(auvTime(tag));
            case colAuvAndShipTime:
                return new EllapsedTime(shipAuvTime(tag));
            case colAge:
                return new EllapsedTime(tag.getAge());
            case colAccuracy:
                return tag.getAccuracy();
            case colResponseTime:
                return new EllapsedTime(tag.getAge()
                        + (Math.min(auvTime(tag), shipAuvTime(tag))));
            default:
                return "?";
        }
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        switch (columnIndex) {
            case colTagName:
                return String.class;
            case colDistAuv:
                return Distance.class;
            case colDistShip:
                return Distance.class;
            case colAuvTime:
                return EllapsedTime.class;
            case colAuvAndShipTime:
                return EllapsedTime.class;
            case colAge:
                return EllapsedTime.class;
            case colAccuracy:
                return String.class;
            case colResponseTime:
                return EllapsedTime.class;
            default:
                return String.class;
        }
    }

    @Override
    public String getColumnName(int column) {
        switch (column) {
            case colTagName:
                return "Tag";
            case colDistAuv:
                return "AUV Distance";
            case colDistShip:
                return "Ship Distance";
            case colAuvTime:
                return "AUV ETA";
            case colAuvAndShipTime:
                return "AUV+Ship ETA";
            case colAge:
                return "Age";
            case colAccuracy:
                return "Accuracy";
            case colResponseTime:
                return "Response Time";
            default:
                return "?";
        }
    }
    
    class EllapsedTime implements Comparable<EllapsedTime> {
        private long time;
        public EllapsedTime(long time) {
            this.time = time;
        }
        
        @Override
        public String toString() {
            return DateTimeUtil.milliSecondsToFormatedString(time);
        }
        
        @Override
        public int compareTo(EllapsedTime o) {
            return (int) (time - o.time);
        }
    }
    
    class Distance implements Comparable<Distance> {
        
        Double distanceMeters;
        
        public Distance(double dist) {
            this.distanceMeters = dist;
        }
        @Override
        public int compareTo(Distance o) {
            return distanceMeters.compareTo(o.distanceMeters);
        }
        
        @Override
        public String toString() {
            if (distanceMeters < 1000)
                return String.format(Locale.US, "%.2f m", distanceMeters);
            else
                return String.format(Locale.US, "%.2f km", distanceMeters/1000);
        }
    }
}
