/*
 * Copyright (c) 2004-2016 Universidade do Porto - Faculdade de Engenharia
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
 * European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * http://ec.europa.eu/idabc/eupl.html.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: Manuel R.
 * 19/08/2016
 */
package pt.lsts.neptus.console.plugins.planning.overview;

import java.awt.Color;
import java.util.ArrayList;

import javax.swing.table.AbstractTableModel;

import pt.lsts.neptus.mp.Maneuver;
import pt.lsts.neptus.mp.Maneuver.SPEED_UNITS;
import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.mp.maneuvers.LocatedManeuver;
import pt.lsts.neptus.mp.maneuvers.ManeuverWithSpeed;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.PlanUtil;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.util.DateTimeUtil;
import pt.lsts.neptus.util.GuiUtils;

/**
 * @author Manuel R.
 *
 */
public class PlanTableModel extends AbstractTableModel {
    private static final long serialVersionUID = 1L;
    public static final Color INIT_MANEUVER_COLOR = new Color(0x7BBD87);
    public static final Color UNREACH_MANEUVER_COLOR = new Color(0x94959C);
    public static final Color SELECTED_MANEUVER_COLOR = new Color(0xCECECE);
    private PlanType plan;
    private ArrayList<ExtendedManeuver> list = new ArrayList<>();
    private static final int COLUMN_INDEX          = 0;
    private static final int COLUMN_LABEL          = 1;
    private static final int COLUMN_TYPE           = 2;
    private static final int COLUMN_LOCATION       = 3;
    private static final int COLUMN_DEPTH_ALTITUDE = 4;
    private static final int COLUMN_SPEED          = 5;
    private static final int COLUMN_DURATION       = 6;
    private static final int COLUMN_DISTANCE       = 7;
    private static final int COLUMN_PAYLOAD        = 8;

    private String[] columnNames = {
            "#",
            "Label",
            "Type",
            "Location",
            "Depth/Altitude (m)",
            "Speed",
            "Est. Duration",
            "Distance (m)",
            "Payload"
    };

    public PlanTableModel(PlanType plan) {
        this.plan = plan;

    }

    @Override
    public int getRowCount() {
        return list.size();
    }

    public int getManeuverIndex(Maneuver man) {
        for (int i=0; i < list.size(); i++) {
            ExtendedManeuver m = list.get(i);
            if (m.maneuver.equals(man))
                return i;
        }
        return -1;
    }

    public Maneuver getManeuver(int index) {
        return list.get(index).maneuver;
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (list.isEmpty() || rowIndex >= getRowCount())
            return null;
        
        Object returnValue = null;
        ExtendedManeuver man = list.get(rowIndex);

        switch (columnIndex) {
            case COLUMN_INDEX:
                returnValue = man.index;
                break;
            case COLUMN_LABEL:
                returnValue = man.maneuver.getId();
                break;
            case COLUMN_TYPE:
                returnValue = man.maneuver.getType();
                break;
            case COLUMN_LOCATION:
                returnValue = man.getManeuverLocation();
                break;
            case COLUMN_DEPTH_ALTITUDE:
                returnValue = man.getManeuverLocation().getZ() + " " + man.getManeuverLocation().getZUnits().name();
                break;
            case COLUMN_SPEED:
                returnValue = man.getSpeedString();
                break;
            case COLUMN_DURATION:
                returnValue = man.getDuration();
                break;
            case COLUMN_DISTANCE:
                returnValue = man.getDistance();
                break;
            case COLUMN_PAYLOAD:
                returnValue = man.getPayload();
                break;

            default:
                throw new IllegalArgumentException("Invalid column index");
        }
        return returnValue;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (list.isEmpty())
            return Object.class;

        switch (columnIndex) {
            case 0:
                return String.class;
            case 1:
                return String.class;
            case 2:
                return String.class;
            case 3:
                return LocationType.class;
            case 4:
                return String.class;
            case 5:
                return String.class;
            case 6:
                return String.class;
            case 7:
                return String.class;
            case 8:
                return String.class;
            default:
                return Object.class;
        }
    }

    public Color getRowColour(int row, boolean isSelected) {
        if (list.isEmpty() || row >= getRowCount() )
            return null;

        if (isSelected)
            return SELECTED_MANEUVER_COLOR;
        else
            return list.get(row).getColor();
    }

    @Override
    public String getColumnName(int columnIndex) {
        return columnNames[columnIndex];
    }

    public void updateTable(PlanType plan) {
        this.plan = plan;

        fireTableDataChanged();
    }

    private class ExtendedManeuver {
        private Maneuver maneuver;
        private ManeuverLocation maneuverLoc;
        private String index;
        private double speed;
        private String speedStr;
        private String duration;
        private String distance;
        private String payload;
        private Color color;
        
        public ExtendedManeuver(Maneuver man, String index) {
            this.maneuverLoc = ((LocatedManeuver) man).getManeuverLocation();
            double speed = -1;
            SPEED_UNITS speedUnits = null;
            if (index.equals("0")) {
                this.distance = "0";
                this.duration = "0s";
            }

            if (man instanceof ManeuverWithSpeed) {
                speed = ((ManeuverWithSpeed) man).getSpeed();
                speedUnits = ((ManeuverWithSpeed) man).getSpeedUnits();
                speedStr = speed + " "+speedUnits.getString();
            }

            this.maneuver = man;
            this.index = index;
            this.speed = speed;
        }

        public ManeuverLocation getManeuverLocation() {
            return maneuverLoc;
        }

        public String getSpeedString() {
            return speedStr;
        }

        public Maneuver getManeuver() {
            return maneuver;
        }

        public String getIndex() {
            return index;
        }

        public void setIndex(String index) {
            this.index = index;
        }

        public String getDuration() {
            return duration;
        }

        public String getDistance() {
            return distance;
        }

        public String getPayload() {
            return payload;
        }

        public void setDistanceAndDuration(String dist, String duration) {
            this.distance = dist;
            this.duration = duration;
        }
        
        public void calcDurationAndDistance(Maneuver inRelationTo) {

            ManeuverLocation prevMan = ((LocatedManeuver) inRelationTo).getManeuverLocation();
            distance = GuiUtils.getNeptusDecimalFormat(0).format(maneuverLoc.getDistanceInMeters(prevMan));

            try {
                duration = DateTimeUtil.milliSecondsToFormatedString((long)(PlanUtil.getEstimatedDelay(prevMan, maneuver) * 1000), true);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        public Color getColor() {
            return color;
        }

        public void setColor(Color color) {
            this.color = color;
        }

    }

}
