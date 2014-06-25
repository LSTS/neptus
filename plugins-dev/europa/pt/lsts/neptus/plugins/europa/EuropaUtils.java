/*
 * Copyright (c) 2004-2014 Universidade do Porto - Faculdade de Engenharia
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
 * https://www.lsts.pt/neptus/licence.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: zp
 * Jun 25, 2014
 */
package pt.lsts.neptus.plugins.europa;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;
import org.mozilla.javascript.edu.emory.mathcs.backport.java.util.Arrays;

import psengine.PSEngine;
import psengine.PSLanguageExceptionList;
import psengine.PSSolver;
import psengine.PSStringList;
import pt.lsts.neptus.NeptusLog;

/**
 * @author zp
 *
 */
public class EuropaUtils {

    private static final String modelDir = new File("conf/nddl").getAbsolutePath(); 

    public static String clearVarName(String varName) {
        return varName.replaceAll("[^a-zA-Z0-9_]", "_");
    }

    public static PSEngine createPlanner(String model) {
        try {
            EuropaUtils.loadLibrary("System_o");

            final PSEngine europa = PSEngine.makeInstance();
            europa.start();

            String nddlInclude = modelDir;

            if (model.contains("/")) {
                String[] folders = model.split("/");
                String folder = "";
                for (int i = 0; i < folders.length; i++) {
                    folder += File.separator+folders[i];
                    if (new File(modelDir+folder).isDirectory())
                        nddlInclude+= ":"+modelDir+folder;
                }
            }

            europa.getConfig().setProperty("nddl.includePath", nddlInclude);
            europa.executeScript("nddl", modelDir + File.separator+ model, true);

            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    europa.shutdown();
                }
            });            
            return europa;            
        }
        catch (Exception e) {
            NeptusLog.pub().error(e);
            return null;
        }
    }

    public static void eval(PSEngine engine, String nddl) throws PSLanguageExceptionList {
        engine.executeScript("nddl", nddl, false);
    }

    public static void dumpNddlException(PSLanguageExceptionList e) {
        for (int i = 0; i < e.getExceptionCount(); i++)
            System.err.println(e.getException(i).getFileName() + " (" + e.getException(i).getLine() + "): "
                    + e.getException(i).getMessage());
    }

    public static PSSolver createSolver(PSEngine engine, int endHorizon) {

        String plannerConfig = modelDir+File.separator+"PlannerConfig.xml";
        PSSolver solver = engine.createSolver(plannerConfig);
        solver.configure(0, endHorizon);
        engine.getConfig().readFromXML(modelDir+File.separator+"NDDL.cfg", true);
        return solver;
    }

    public static boolean failed(PSSolver solver) {
        return (solver.isExhausted() || solver.isTimedOut());
    }

    public static boolean complete(PSSolver solver) {
        System.out.println(solver.getFlaws().size()+" flaws are still left");
        return solver.getFlaws().size() == 0;
    }

    public static boolean succeeded(PSSolver solver) {
        return complete(solver) && !failed(solver);
    }

    public static boolean step(PSSolver solver) {
        System.out.println("step "+solver.getStepCount());
        if (failed(solver))
            return false;
        else {
            solver.step();
            return (!complete(solver) && !failed(solver));
        }
    }

    public static void printFlaws(PSSolver solver) {
        System.out.println("Flaws: ");
        PSStringList fls = solver.getFlaws();
        for (int j = 0; j < fls.size(); j++)
            System.out.println("  - " + fls.get(j));
        System.out.println();
    }

    @SuppressWarnings("unchecked")
    public static String loadLibrary(String lib) throws Exception {
        String lookFor = System.mapLibraryName(lib);
        Vector<String> path = new Vector<>();
        if (System.getenv("EUROPA_HOME") != null && new File(System.getenv("EUROPA_HOME")).isDirectory())
            path.add(System.getenv("EUROPA_HOME") + File.separator + "lib");
        path.addAll(Arrays.asList(System.getProperty("java.library.path").split(File.pathSeparator)));
        path.addAll(Arrays.asList(System.getenv("LD_LIBRARY_PATH").split(File.pathSeparator)));

        for (String s : path)
            if (new File(s, lookFor).exists()) {
                String library = new File(s, lookFor).getAbsolutePath();
                NeptusLog.pub().info("native library loaded from "+library+".");
                System.load(library);
                return s;
            }
        throw new FileNotFoundException("Library "+System.mapLibraryName(lib)+" was not found in "+StringUtils.join(path, File.pathSeparator));
    }

    public static void main(String[] args) {
        System.out.println(clearVarName("lau0v_sxplore-1"));
        //EuropaUtils.createPlanner("neptus/auv_model.nddl");
    }
}
