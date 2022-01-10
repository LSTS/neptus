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
 * Author: keila
 * 26/05/2017
 */
package pt.lsts.neptus.plugins.groovy;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;

import groovy.lang.Binding;
import groovy.util.GroovyScriptEngine;
import pt.lsts.imc.VehicleState;
import pt.lsts.imc.dsl.Location;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.events.ConsoleEventPlanChange;
import pt.lsts.neptus.types.map.MapGroup;
import pt.lsts.neptus.types.map.MarkElement;
import pt.lsts.neptus.types.map.ParallelepipedElement;
import pt.lsts.neptus.types.mission.plan.PlanType;

public class GroovyEngine {

    // Collections used to make Map thread safe
    private Map<String, ImcSystem> vehicles = Collections.synchronizedMap(new HashMap<>());
    private Map<String, PlanType> plans = Collections.synchronizedMap(new HashMap<>());
    private Map<String, Location> locations = Collections.synchronizedMap(new HashMap<>());
    private Map<String, ParallelepipedElement> shapes = Collections.synchronizedMap(new HashMap<>());
    private Binding binds;
    private GroovyScriptEngine engine;
    private CompilerConfiguration config;
    private Thread runningThread;
    private ImportCustomizer customizer;
    private GroovyPanel console;
    private Writer scriptOutput;
    private PrintWriter ps;
    private StringBuffer buffer;

    public Thread getRunninThread() {
        return runningThread;

    }

    public GroovyEngine(GroovyPanel c) {
        this.console = c;
        setup();
        buffer = new StringBuffer();
        scriptOutput = new Writer() {

            @Override
            public void write(char[] cbuf) {
                buffer.append(cbuf);
            }

            @Override
            public void write(int c) {
                // console.appendOutput(String.valueOf((char)c));
                buffer.append((char) c);
            }

            @Override
            public void write(String str) {
                // System.out.println("Str: "+ str);
                // console.appendOutput(str);
                buffer.append(str);
            }

            @Override
            public void write(char[] cbuf, int off, int len) throws IOException {
                // System.out.println("Char buffer: "+ String.valueOf(cbuf, off, len));
                // console.appendOutput(String.valueOf(cbuf, off, len));
                buffer.append(cbuf, off, len);
            }

            @Override
            public void flush() throws IOException {
                console.appendOutput(buffer.toString());
                buffer = new StringBuffer();
                // System.out.println("Flushed!");

            }

            @Override
            public void close() throws IOException {
            }
        };
        ps = new PrintWriter(scriptOutput, true);

    }

    /**
     * 
     */
    private void setup() {

        binds = new Binding();
        binds.setVariable("console", console.getConsole());
        // updateBindings();
        config = new CompilerConfiguration();
        customizer = new ImportCustomizer();
        customizer.addImports("pt.lsts.imc.net.IMCProtocol", "pt.lsts.neptus.types.coord.LocationType");
        customizer.addStarImports("pt.lsts.imc", "pt.lsts.imc.dsl", "pt.lsts.neptus.types.map"); // this.getClass().classLoader.rootLoader.addURL(new
                                                                                                 // File("file.jar").toURL())
        customizer.addImport("Plan", PlanScript.class.getName());
        customizer.addStaticImport(PlanScript.class.getName(), "midpoint");
        customizer.addStaticImport(PlanScript.class.getName(), "convertToLocation");
        customizer.addStaticImport(PlanScript.class.getName(), "fromLocation");
        customizer.addStaticImport(PlanScript.class.getName(), "midpoints");
        customizer.addStaticStars(MapScript.class.getName());
        MapScript.getInstance().setConsole(console.getConsole());
        config.addCompilationCustomizers(customizer);

        try {
            // Description/notification: "Place your groovy scripts in the folder script of the plugin"
            this.engine = new GroovyScriptEngine(".");// new GroovyScriptEngine("conf/groovy/scripts/");

            this.engine.setConfig(this.config);

        }
        catch (IOException e) {
            // e.printStackTrace();
        }
    }

    public void stopScript() {
        if (runningThread != null && runningThread.isAlive()) {
            runningThread.interrupt();
            NeptusLog.pub().warn("Stoped script execution in the groovy plugin.");

        }
    }

    public void runScript(String groovyScript) {

        runningThread = new Thread() {

            @Override
            public void run() {
                try {
                    updateBindings();
                    binds.setProperty("out", ps);
                    engine.run(groovyScript, binds);
                    console.disableStopButton();
                    stopScript();
                }
                catch (Exception e) { // CompilationFailedException | ResourceException | ScriptException
                    NeptusLog.pub().error("Exception Caught during execution of script: " + groovyScript, e);// e.printStackTrace();
                    console.appendOutput("Error: \n\t" + e.getMessage());
                    console.disableStopButton();
                    stopScript();

                }
                catch (ThreadDeath e) {
                    NeptusLog.pub().info("Exiting script execution: " + groovyScript);
                }
            }

        };
        runningThread.start();

    }

    private void updateBindings() {
        plans.clear();
        locations.clear();
        vehicles.clear();
        shapes.clear();

        for (ImcSystem v : ImcSystemsHolder.lookupActiveSystemVehicles()) {
            VehicleState state = ImcMsgManager.getManager().getState(v.getName()).last(VehicleState.class);
            if (state != null && state.getOpMode() == VehicleState.OP_MODE.SERVICE) {
                vehicles.put(v.getName(), v);
            }
        }
        binds.setVariable("vehicles", vehicles);

        for (PlanType p : console.getConsole().getMission().getIndividualPlansList().values())
            plans.put(p.getId(), p);

        // POI/MarkElement
        for (MarkElement mark : MapGroup.getMapGroupInstance(console.getConsole().getMission())
                .getAllObjectsOfType(MarkElement.class)) {
            if (!mark.obstacle) {
                Location loc = new Location(mark.getPosition().getLatitudeRads(),
                        mark.getPosition().getLongitudeRads());
                locations.put(mark.getId(), loc);
            }

        }
        for (ParallelepipedElement el : MapGroup.getMapGroupInstance(console.getConsole().getMission())
                .getAllObjectsOfType(ParallelepipedElement.class)) {
            shapes.put(el.getId(), el);
        }
        binds.setVariable("shapes", shapes);
        binds.setVariable("marks", locations);
        binds.setVariable("plans", plans);
    }

    /**
     * @param changedPlan
     */
    public void planChange(ConsoleEventPlanChange changedPlan) {
        if (changedPlan.getCurrent() == null) {
            if (!console.getConsole().getMission().getIndividualPlansList().containsKey(changedPlan.getOld().getId())) {
                // System.out.println("Plan "+changedPlan.getOld().getId()+" removed.");
                plans.remove(changedPlan.getOld().getId());
                binds.setVariable("plans", plans);

            }
            else {
                plans.put(changedPlan.getCurrent().getId(), changedPlan.getCurrent());
                binds.setVariable("plans", plans);
            }
        }

    }

    // /**
    // * @param e
    // */
    // public void vehicleStateChanged(ConsoleEventVehicleStateChanged e) {
    // switch (e.getState()) {
    // case SERVICE: //case CONNECTED
    // if (ImcSystemsHolder.getSystemWithName(e.getVehicle()).isActive()) {
    // //add new vehicle
    // if(!vehicles.containsKey(e.getVehicle())) {
    // vehicles.put(e.getVehicle(),ImcSystemsHolder.getSystemWithName(e.getVehicle()));
    // binds.setVariable("vehicles", vehicles);
    // //System.out.println("Added "+e.getVehicle()+" Size: "+vehicles.keySet().size());
    // }
    // }
    // break;
    // case ERROR:
    // case DISCONNECTED:
    // case BOOT:
    // case CALIBRATION:
    // case MANEUVER:
    // if(vehicles.containsKey(e.getVehicle())){
    // vehicles.remove(e.getVehicle());
    // binds.setVariable("vehicles", vehicles);
    // //System.out.println("Removed "+e.getVehicle()+" Size: "+vehicles.keySet().size());
    // }
    // break;
    //
    // default:
    // break;
    // }
    //
    // }

    /**
     * Add new updated plan to bind variable
     * 
     * @param planId of the plan
     */
    public void addPlan(String planId) {

        this.plans.put(planId, console.getConsole().getMission().getIndividualPlansList().get(planId));
    }

    /**
     * @return the plans
     */
    public HashMap<String, PlanType> getPlans() {
        return (HashMap<String, PlanType>) plans;
    }
}
