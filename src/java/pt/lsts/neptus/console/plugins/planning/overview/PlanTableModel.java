/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
 * Author: Manuel R.
 * 19/08/2016
 */
package pt.lsts.neptus.console.plugins.planning.overview;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map.Entry;

import javax.swing.table.AbstractTableModel;

import pt.lsts.imc.EntityParameter;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.SetEntityParameters;
import pt.lsts.neptus.mp.Maneuver;
import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.mp.SpeedType;
import pt.lsts.neptus.mp.maneuvers.LocatedManeuver;
import pt.lsts.neptus.mp.maneuvers.ManeuverWithSpeed;
import pt.lsts.neptus.types.coord.CoordinateUtil;
import pt.lsts.neptus.types.map.PlanUtil;
import pt.lsts.neptus.types.mission.TransitionType;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.util.DateTimeUtil;
import pt.lsts.neptus.util.GuiUtils;

/**
 * @author Manuel Ribeiro
 *
 */
public class PlanTableModel extends AbstractTableModel {
    private static final long serialVersionUID = 1L;
    public static final Color INIT_MANEUVER_COLOR = new Color(0x7BBD87);
    public static final Color UNREACH_MANEUVER_COLOR = new Color(0xB7B7BA);
    public static final Color SELECTED_MANEUVER_COLOR = new Color(0x289CED);
    
    private PlanType plan;
    private ArrayList<ExtendedManeuver> list = new ArrayList<>();
    public static final int COLUMN_LABEL = 0;
    public static final int COLUMN_TYPE = 1;
    public static final int COLUMN_OUT_TRANS = 2;
    public static final int COLUMN_LOCATION = 3;
    public static final int COLUMN_DEPTH_ALTITUDE = 4;
    public static final int COLUMN_SPEED = 5;
    public static final int COLUMN_DURATION = 6;
    public static final int COLUMN_DISTANCE = 7;
    public static final int COLUMN_PAYLOAD = 8;

    private String[] columnNames = {
            "Label",
            "Type",
            "Out",
            "Location",
            "Depth/Altitude (m)",
            "Speed",
            "Est. Duration",
            "Distance (m)",
            "Payload"
    };

    public PlanTableModel(PlanType plan) {
        if (plan == null)
            return;
        this.plan = plan;
        sortManeuverList();
        fireTableDataChanged();
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

    public String getManeuverToString(int index) {
        return list.get(index).toString();
    }

    public String getManeuverLocation(int index) {
        return list.get(index).getManeuverLocation().getClipboardText();
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
            case COLUMN_LABEL:
                returnValue = man.maneuver.getId();
                break;
            case COLUMN_TYPE:
                returnValue = man.maneuver.getType();
                break;
            case COLUMN_OUT_TRANS:
                returnValue = man.getOutTransitions();
                break;
            case COLUMN_LOCATION:
                returnValue = CoordinateUtil.latitudeAsPrettyString(man.getManeuverLocation().getLatitudeDegs()) + ", "
                        + CoordinateUtil.longitudeAsPrettyString(man.getManeuverLocation().getLongitudeDegs());
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
                return String.class;
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
            case 9:
                return String.class;
            default:
                return Object.class;
        }
    }

    public Color getRowColour(int row, boolean isSelected) {
        if (list.isEmpty() || row >= getRowCount())
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
        sortManeuverList();

        fireTableDataChanged();
    }

    private void sortManeuverList() {
        Maneuver initial = plan.getGraph().getManeuver(plan.getGraph().getInitialManeuverId());
        list.clear();

        if (initial == null)
            return;

        @SuppressWarnings("unchecked")
        LinkedHashMap<String, TransitionType> trans = (LinkedHashMap<String, TransitionType>) plan.getGraph().getTransitions().clone();

        //add initial maneuver if exists
        list.add(new ExtendedManeuver(initial, "0"));

        Maneuver in = initial;
        ArrayList<Maneuver> visited = new ArrayList<>();
        visited.add(in);
        Iterator<Entry<String, TransitionType>> it = trans.entrySet().iterator();
        while (true) {
            while (it.hasNext()) {
                Entry<String, TransitionType> t = it.next();
                String source = t.getValue().getSourceManeuver();
                Maneuver srcManeuver = plan.getGraph().getManeuver(source);
                String dest = t.getValue().getTargetManeuver();
                Maneuver destManeuver = plan.getGraph().getManeuver(dest);

                if (source.equals(in.id)) {
                    if (!alreadyAdded(srcManeuver)) {

                        ExtendedManeuver e = new ExtendedManeuver(srcManeuver, "-1");
                        list.add(e);
                    }

                    ExtendedManeuver e = new ExtendedManeuver(destManeuver, "-1");
                    if (!alreadyAdded(destManeuver))
                        list.add(e);

                    if (!visited.contains(destManeuver)) 
                        visited.add(destManeuver);

                    it.remove();

                    if (allOutManeuvers(in, trans).isEmpty()) {
                        in = destManeuver;
                    }
                }

                if (allOutManeuvers(in, trans).isEmpty()) {
                    if (visited.isEmpty()) {
                        in = srcManeuver;
                    }
                    else {
                        in = visited.get(visited.size()-1);
                        visited.remove(visited.size()-1);
                    }
                }
            }

            if (trans.isEmpty())
                break;
            else
                it = trans.entrySet().iterator();
        }
        visited.clear();

        //check for missing not reachable maneuvers
        LinkedList<Maneuver> allNotReachable = notReachableManeuvers();
        for (Maneuver m : allNotReachable) {
            if (!alreadyAdded(m)) {
                ExtendedManeuver e = new ExtendedManeuver(m, "-1");
                list.add(e);
            }
        }

        LinkedList<Maneuver> unfeasible = new LinkedList<>();
        int index = 0;
        int aux = 0;
        int count = 0;
        for (ExtendedManeuver m : list) {
            if (m.maneuver.isInitialManeuver() && index == 0) {
                index = 0;
                index++;
                m.setColor(INIT_MANEUVER_COLOR);
            }
            else {
                count = hasMultipleOutTransTo(m.maneuver);
                if (count > 1) {
                    Maneuver src = getSourceManeuver(m.maneuver);
                    m.setIndex(index+"."+(aux));
                    m.calcDurationAndDistance(src);
                    if (allNotReachable.contains(src) || aux > 0) {
                        m.setColor(UNREACH_MANEUVER_COLOR);
                        unfeasible.add(m.maneuver);
                    }
                    aux++;

                    if (aux == count)
                        index++;
                }
                else {
                    m.setIndex(index+"");
                    Maneuver src = getSourceManeuver(m.maneuver);
                    if (src != null) {
                        m.calcDurationAndDistance(src);
                        m.setColor(Color.WHITE);
                    }
                    aux = 0;
                    index++;

                    if (allNotReachable.contains(m.maneuver) || allNotReachable.contains(src) || unfeasible.contains(src)) {
                        m.setColor(UNREACH_MANEUVER_COLOR);
                        m.setDistanceAndDuration("0", "0s");
                        unfeasible.add(m.maneuver);
                    }
                }
            }
        }
        trans.clear();
        unfeasible.clear();
        allNotReachable.clear();
    }

    private Maneuver getSourceManeuver(Maneuver m) {
        for (Entry<String, TransitionType> e : plan.getGraph().getTransitions().entrySet()) {
            if (e.getValue().getTargetManeuver().equals(m.getId()))
                return plan.getGraph().getManeuver(e.getValue().getSourceManeuver());
        }
        return null;
    }

    private boolean alreadyAdded(Maneuver srcManeuver) {
        for (ExtendedManeuver m : list) {
            if (m.maneuver.equals(srcManeuver))
                return true;
        }

        return false;
    }

    private LinkedList<Maneuver> allOutManeuvers(Maneuver m, LinkedHashMap<String, TransitionType> trans) {
        LinkedList<Maneuver> list = new LinkedList<>();
        for (Entry<String, TransitionType> e : trans.entrySet()) {
            if (e.getValue().getSourceManeuver().equals(m.id))
                list.add(plan.getGraph().getManeuver(e.getValue().getTargetManeuver()));
        }

        return list;
    }

    private LinkedList<Maneuver> notReachableManeuvers() {
        LinkedList<Maneuver> list = new LinkedList<>();
        for (Maneuver m : plan.getGraph().getAllManeuvers()) {
            if (!reachable(m) && !plan.getGraph().getInitialManeuverId().equals(m.getId()))
                list.add(m);
        }

        return list;
    }

    private boolean reachable(Maneuver m) {
        for (Entry<String, TransitionType> e : plan.getGraph().getTransitions().entrySet()) {
            if (e.getValue().getTargetManeuver().equals(m.getId()))
                return true;

        }

        return false;
    }

    private int hasMultipleOutTransTo(Maneuver m) {
        Maneuver top = null;
        int count = 0;
        for (TransitionType t : plan.getGraph().getTransitions().values()) {
            if (plan.getGraph().getManeuver(t.getTargetManeuver()).equals(m)) {
                top = plan.getGraph().getManeuver(t.getSourceManeuver());
                break;
            }
        }

        if (top != null) {
            for (TransitionType t : plan.getGraph().getTransitions().values())
                if (plan.getGraph().getManeuver(t.getSourceManeuver()).equals(top))
                    count++;
        }
        return count;
    }

    /**
     * Retrieve the payload of a given Maneuver
     *
     * @param m The maneuver to get the payload section from.
     * @return formated payload string for this maneuver
     */
    private String getPayloads(Maneuver m) {
        StringBuilder sb = new StringBuilder();
        String delim = "";

        if (m.getStartActions() == null)
            return sb.toString();

        IMCMessage[] msgs = m.getStartActions().getAllMessages();
        for (IMCMessage msg : msgs) {
            if (msg instanceof SetEntityParameters) {
                String payload = parse((SetEntityParameters) msg);
                if (payload != null) {
                    sb.append(delim).append(payload);
                    delim = ", ";
                }
            }
        }
        return sb.toString();
    }

    /**
     * Parses the payload of a given SetEntityParameters message
     *
     * @param msg The message to be parsed
     * @return formated payload string
     */
    private String parse(SetEntityParameters msg) {
        boolean active = false;
        StringBuilder sb = new StringBuilder();
        String payload = msg.getName();
        if (payload.equalsIgnoreCase("Sidescan"))
            payload = "SS";
        else if (payload.equalsIgnoreCase("Multibeam"))
            payload = "MB";
        else if (payload.equalsIgnoreCase("Camera"))
            payload = "CAM";
        else
            payload = payload.replaceAll("([a-z0$-/:-?{-~!^_`@#\" ]+)+", "");

        for (EntityParameter p : msg.getParams()) {
            if (p.getName().equals("Active")) {
                active = p.getValue().equalsIgnoreCase("true");
            }
            else {
                if (!sb.toString().isEmpty())
                    sb.append(" ");
                sb.append(p.getName().replaceAll("([a-z0$-/:-?{-~!^_`@#\" ]+)+", "")).append(":").append(p.getValue());
            }
        }

        if (active)
            return sb.toString().isEmpty() ? payload : payload + "{" + sb.toString() + "}";


        return null;
    }

    private class ExtendedManeuver {

        private Maneuver maneuver;
        private ManeuverLocation maneuverLoc;
        private String index;
        private SpeedType speed;
        private String speedStr;
        private String duration;
        private String distance;
        private String payload;
        private Color color;

        public ExtendedManeuver(Maneuver man, String index) {
            this.maneuverLoc = ((LocatedManeuver) man).getManeuverLocation();
            SpeedType speed = null;

            if (index.equals("0")) {
                this.distance = "0";
                this.duration = "0s";
            }

            if (man instanceof ManeuverWithSpeed) {
                speed = ((ManeuverWithSpeed) man).getSpeed();
                speedStr = speed.toString();
            }

            this.maneuver = man;
            this.index = index;
            this.speed = speed;
            this.payload = getPayloads(maneuver);
        }

        public String getOutTransitions() {
            StringBuilder str = new StringBuilder();
            String delim = "";

            LinkedList<Maneuver> list = allOutManeuvers(maneuver, plan.getGraph().getTransitions());
            for (Maneuver m : list) {
                str.append(delim).append(m.getId());
                delim = ", ";
            }
            return str.toString();
        }

        public ManeuverLocation getManeuverLocation() {
            return maneuverLoc;
        }

        public String getSpeedString() {
            return speedStr;
        }

        @SuppressWarnings("unused")
        public Maneuver getManeuver() {
            return maneuver;
        }

        @SuppressWarnings("unused")
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

        @Override
        public String toString() {
            return "[index=" + index + " maneuver=" + maneuver.getId() + ", maneuverLoc=" + maneuverLoc
                    + ", speedStr=" + speedStr + ", duration=" + duration + ", distance="
                    + distance + "m, payload=" + payload +"]";
        }
    }
}
