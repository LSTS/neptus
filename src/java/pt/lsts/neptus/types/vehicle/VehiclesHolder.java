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
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
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
 * Author: Paulo Dias
 * 2005/01/15
 */
package pt.lsts.neptus.types.vehicle;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.lang.reflect.Constructor;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

import foxtrot.AsyncTask;
import foxtrot.AsyncWorker;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.manager.imc.ImcId16;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.types.comm.CommMean;
import pt.lsts.neptus.types.coord.CoordinateSystem;
import pt.lsts.neptus.types.coord.CoordinateSystemsHolder;
import pt.lsts.neptus.types.mission.MissionType;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.util.ConsoleParse;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.conf.ConfigFetch;

/**
 * @author Paulo Dias
 * 
 */
public class VehiclesHolder {
    private static LinkedHashMap<String, VehicleType> vehiclesList = new LinkedHashMap<String, VehicleType>();
    private static boolean vehiclesLoaded = false;

    private static LinkedHashMap<String, Hashtable<String, ConsoleLayout>> openConsolesByVehicle = new LinkedHashMap<String, Hashtable<String, ConsoleLayout>>();
    public static String CONSOLE_CLASS = "classname";
    public static String CONSOLE_XML = "xml-file";

    public VehiclesHolder() {
        loadVehicles();
    }

    public static boolean addVehicle(VehicleType vehicle) {
        if (vehiclesList.put(vehicle.getId(), vehicle) == null)
            return false;
        return true;
    }

    public static int size() {
        if (!vehiclesLoaded) {
            vehiclesLoaded = true;
            loadVehicles();
        }

        return vehiclesList.size();
    }

    /**
     * @return Returns the vehiclesList.
     */
    public static LinkedHashMap<String, VehicleType> getVehiclesList() {
        if (!vehiclesLoaded) {
            loadVehicles();
        }

        return vehiclesList;
    }

    /**
     * @return
     */
    public static String[] getVehiclesArray() {
        if (!vehiclesLoaded)
            loadVehicles();
        String[] list = new String[vehiclesList.values().size()];
        int i = 0;
        for (VehicleType v : vehiclesList.values()) {
            list[i] = v.getId();
            i++;
        }
        return list;

    }

    /**
     * @param id
     * @return
     */
    public static VehicleType getVehicleById(String id) {
        Object obj = getVehiclesList().get(id);
        if (obj == null)
            return null;
        return (VehicleType) obj;
    }

    /**
     * @param protocol
     * @return
     */
    public static List<VehicleType> getVehicleWithProtocol(String protocol) {
        LinkedList<VehicleType> list = new LinkedList<VehicleType>();
        LinkedHashMap<String, VehicleType> vehList = getVehiclesList();
        for (VehicleType veh : vehList.values()) {
            LinkedHashMap<String, CommMean> comms = veh.getCommunicationMeans();
            for (CommMean commMean : comms.values()) {
                for (String prot : commMean.getProtocols()) {
                    if (prot.indexOf(protocol) != -1) {
                        list.add(veh);
                        break;
                    }
                }
            }
        }
        return list;
    }

    public static List<VehicleType> getVehicleWithImc() {
        LinkedList<VehicleType> list = new LinkedList<VehicleType>();
        List<VehicleType> vehList = getVehicleWithProtocol(CommMean.IMC);
        for (VehicleType veh : vehList) {
            ImcId16 imcId = veh.getImcId();
            if (imcId != ImcId16.NULL_ID)
                list.add(veh);
        }
        return list;
    }

    public static VehicleType getVehicleWithImc(ImcId16 imcId) {
        List<VehicleType> vehList = getVehicleWithImc();
        for (VehicleType veh : vehList) {
            ImcId16 imcIdTmp = veh.getImcId();
            if (imcIdTmp == null)
                return null;
            if (imcIdTmp.equals(imcId))
                return veh;
        }
        return null;
    }

    /**
     * 
     */
    public static boolean loadVehicles() {
        if (vehiclesLoaded)
            return true;

        CoordinateSystemsHolder csh = new CoordinateSystemsHolder(ConfigFetch.getCoordinateSystemsConfigLocation());
        NeptusLog.pub().debug("CoordinateSystems #: " + csh.size());

        LinkedList<String> vehicleList = ConfigFetch.getVehiclesList();

        for (String hr : vehicleList) {
            VehicleType v = new VehicleType(ConfigFetch.resolvePath(hr));
            if (!v.isLoadOk())
                continue;
            if (v.getCoordinateSystem() == null) {
                CoordinateSystem cs = (CoordinateSystem) csh.getCoordinateSystemList()
                        .get(v.getCoordinateSystemLabel());
                if (cs != null)
                    v.setCoordinateSystem(cs);
            }
            VehiclesHolder.addVehicle(v);
        }
        vehiclesLoaded = true;
        NeptusLog.pub().debug("Vehicles #: " + VehiclesHolder.size());
        return vehiclesLoaded;
    }

    // ========= Open Consoles Control ==================================
    /**
     * @param vehicleId
     * @param console
     * @return
     */
    public static boolean addOpenConsole(String vehicleId, String consoleID, ConsoleLayout console) {
        Hashtable<String, ConsoleLayout> consolesTable = openConsolesByVehicle.get(vehicleId);
        if (consolesTable != null) {
            for (String name : consolesTable.keySet()) {
                if (name.equals(consoleID))
                    return false;
            }
        }
        else {
            consolesTable = new Hashtable<String, ConsoleLayout>();
            openConsolesByVehicle.put(vehicleId, consolesTable);
        }
        // NeptusLog.pub().info("<###>-->>>> " + consoleID + "    -----   " + console);
        consolesTable.put(consoleID, console);
        return true;
    }

    /**
     * @param vehicleId
     * @param consoleId
     * @return
     */
    public static boolean removeOpenConsole(String vehicleId, String consoleId) {
        Hashtable<String, ConsoleLayout> consolesTable = openConsolesByVehicle.get(vehicleId);
        if (consolesTable == null)
            return false;

        return (consolesTable.remove(consoleId) == null) ? false : true;
    }

    /**
     * @param vehicleId
     * @param consoleId
     * @return
     */
    public static boolean existsOpenConsole(String vehicleId, String consoleId) {
        Hashtable<String, ConsoleLayout> consolesTable = openConsolesByVehicle.get(vehicleId);
        if (consolesTable == null)
            return false;
        for (String name : consolesTable.keySet()) {
            if (name.equals(consoleId))
                return true;
        }
        return false;
    }

    /**
     * @param vehicleId
     * @param consoleId
     * @return
     */
    public static ConsoleLayout getOpenConsole(String vehicleId, String consoleId) {
        Hashtable<String, ConsoleLayout> consolesTable = openConsolesByVehicle.get(vehicleId);
        if (consolesTable == null)
            return null;
        return consolesTable.get(consoleId);
    }

    /**
     * @param vehicle
     * @param callerComponent
     * @see {@link #showConsole(VehicleType, PlanType, MissionType, Component)}
     */
    public static void showConsole(VehicleType vehicle, Component callerComponent) {
        final VehicleType vt = vehicle;
        final Component caller = callerComponent;

        AsyncWorker.post(new AsyncTask() {
            @Override
            public Object run() throws Exception {
                caller.setCursor(new Cursor(Cursor.WAIT_CURSOR));
                showConsole(vt, null, null, caller);
                return null;
            }

            @Override
            public void finish() {
                try {
                    getResultOrThrow();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                caller.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }
        });
    }

    /**
     * @param plan
     * @param mission
     * @param callerComponent
     * @see {@link #showConsole(VehicleType, PlanType, MissionType, Component)}
     */
    public static void showConsole(PlanType plan, MissionType mission, Component callerComponent) {
        final PlanType finalPlan = plan;
        final MissionType finalMission = mission;
        final Component finalCaller = callerComponent;

        AsyncWorker.post(new AsyncTask() {
            @Override
            public Object run() throws Exception {
                finalCaller.setCursor(new Cursor(Cursor.WAIT_CURSOR));
                showConsole(finalPlan.getVehicleType(), finalPlan, finalMission, finalCaller);
                return null;
            }

            @Override
            public void finish() {
                finalCaller.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }
        });
    }

    public static void showConsole(final VehicleType vehicle, MissionType mission, Component callerComponent) {

        final MissionType finalMission = mission;
        final Component finalCaller = callerComponent;

        AsyncWorker.post(new AsyncTask() {
            @Override
            public Object run() throws Exception {
                finalCaller.setCursor(new Cursor(Cursor.WAIT_CURSOR));
                showConsole(vehicle, null, finalMission, finalCaller);
                return null;
            }

            @Override
            public void finish() {
                finalCaller.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }
        });
    }

    /**
     * @param plan
     * @param mission
     * @param callerComponent
     * @param internalFrame
     */
    private static void showConsole(final VehicleType vehicle, PlanType plan, MissionType mission,
            Component callerComponent) {
        String consoleID = null, className = null, classType = null;

        if (vehicle.getConsoles().size() > 1) {
            // Filter the ones that are opened
            String[] consoles = vehicle.getConsoles().keySet().toArray(new String[] {});
            LinkedList<String> filteredCons = new LinkedList<String>();
            for (String cons : consoles) {
                // className = vehicle.getConsoles().get(cons);
                // if (!VehiclesHolder.existsOpenConsole(vehicle
                // .getId(), cons))
                filteredCons.add(cons);
            }
            if (filteredCons.size() == 0) {
                // JOptionPane.showInternalMessageDialog(
                // getMElemPanel(),
                // "All operational consoles are already open for this vehicle.");
                GuiUtils.errorMessage(callerComponent, "Warning",
                        "All operational consoles are already open for this vehicle.");
                return;
            }
            else if (filteredCons.size() > 1) {
                String console = (String) JOptionPane.showInputDialog(callerComponent,
                        "Choose one of the available consoles", "Select console", JOptionPane.QUESTION_MESSAGE,
                        new ImageIcon(), filteredCons.toArray(new String[] {}), filteredCons.iterator().next());
                if (console != null) {
                    consoleID = console;
                    className = vehicle.getConsoles().get(console);
                    classType = vehicle.getConsolesTypes().get(console);
                }
            }
            else {
                String console = filteredCons.iterator().next();
                consoleID = console;
                className = vehicle.getConsoles().get(console);
                classType = vehicle.getConsolesTypes().get(console);
            }

        }
        else {
            try {
                String vehCon = vehicle.getConsoles().keySet().iterator().next();
                consoleID = vehCon;
                className = vehicle.getConsoles().get(vehCon);
                classType = vehicle.getConsolesTypes().get(vehCon);
            }
            catch (Exception e) {
                className = null;
                NeptusLog.pub().debug(e.getMessage());
                GuiUtils.errorMessage(callerComponent, "Warning", "No operational "
                        + "console(s) configured for this vehicle.\n");
            }
        }

        if (className != null) {
            if (VehiclesHolder.existsOpenConsole(vehicle.getId(), consoleID)) {
                GuiUtils.errorMessage(callerComponent, "Warning", "This operational "
                        + "console is already open for this vehicle.\n" + "[" + className + "]");
                ConsoleLayout openConsole = VehiclesHolder.getOpenConsole(vehicle.getId(), consoleID);
                openConsole.setVisible(true);
                openConsole.requestFocus();
                return;
            }
            if (classType.equals(VehiclesHolder.CONSOLE_CLASS)) {
                try {
                    Class<?> clazz = Class.forName(className);
                    Constructor<?> cons = null;
                    ConsoleLayout console = null;
                    if ((mission != null) && (plan != null)) {
                        cons = clazz.getConstructor(new Class[] { MissionType.class, PlanType.class });
                        if (cons != null) {
                            console = (ConsoleLayout) cons.newInstance(new Object[] { mission, plan });
                        }
                    }
                    else {
                        cons = clazz.getConstructor(new Class[] {});
                        if (cons != null) {
                            console = (ConsoleLayout) cons.newInstance((new Object[] {}));
                        }

                    }
                    if (cons != null) {
                        final String consoleId = consoleID; // console.getConsoleID();
                        console.addWindowListener(new WindowAdapter() {
                            public void windowClosing(WindowEvent e) {
                                super.windowClosing(e);
                                VehiclesHolder.removeOpenConsole(vehicle.getId(), consoleId);
                            }
                        });
                        VehiclesHolder.addOpenConsole(vehicle.getId(), consoleID, console);
                    }
                }
                catch (Exception e) {
                    GuiUtils.errorMessage(callerComponent, e);
                    NeptusLog.pub().error(callerComponent, e);
                }
            }
            else if (classType.equals(VehiclesHolder.CONSOLE_XML)) {
                if (!new File(className).exists()) {
                    GuiUtils.errorMessage(callerComponent, I18n.text("Opening Console"),
                            I18n.textf("Console '%consoleFile' does not exist!", className));
                    NeptusLog.pub().error(callerComponent + ": Console '" + className + "' does not exist for vehicle '" +
                    		vehicle + "'!");
                }
                
                ConsoleLayout console = ConsoleParse.consoleLayoutLoader(ConfigFetch.resolvePath(className));

                final String consoleId = consoleID; // console.getConsoleID();
                console.addWindowListener(new WindowAdapter() {
                    public void windowClosing(WindowEvent e) {
                        super.windowClosing(e);
                        VehiclesHolder.removeOpenConsole(vehicle.getId(), consoleId);
                    }
                });
                if (mission != null)
                    console.setMission(mission);
                if (plan != null) {
                    console.setMainSystem(plan.getVehicle());
                    console.setPlan(plan);
                }
                else
                    console.setMainSystem(vehicle.getId());
                VehiclesHolder.addOpenConsole(vehicle.getId(), consoleID, console);
            }
        }
    }

    public boolean isOnlyOneVehicleAvailable() {
        if (!vehiclesLoaded)
            loadVehicles();
        return vehiclesList.size() == 1;
    }
}
