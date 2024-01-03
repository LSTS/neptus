/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
 * Jun 25, 2014
 */
package pt.lsts.neptus.plugins.europa;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;

import Jama.Matrix;
import psengine.PSEngine;
import psengine.PSLanguageExceptionList;
import psengine.PSSolver;
import psengine.PSStringList;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.loader.helper.CheckJavaOSArch;
import pt.lsts.neptus.util.FileUtil;

/**
 * @author zp
 *
 */
public class EuropaUtils {

    private static final String modelDir = new File("conf/nddl").getAbsolutePath(); 

    public static String clearVarName(String varName) {
        return varName.replaceAll("[^a-zA-Z0-9_]", "_");
    }

    public static PSEngine createPlanner(String logDir) throws Exception {
        try {
            EuropaUtils.loadLibrary("System_o");

            final PSEngine europa = PSEngine.makeInstance();
            europa.start();
            
            // Inject information that can be used by Neptus module to locate files
            europa.getConfig().setProperty("neptus.cfgPath", modelDir);
            europa.getConfig().setProperty("neptus.logDir", logDir);
            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    europa.shutdown();
                }
            });            
            return europa;            
        }
        catch (PSLanguageExceptionList e) {
            dumpNddlException(e);
            throw e;
        }
    }
    
    public static String getModel(String relativePath) {
        return FileUtil.getFileAsString(new File(modelDir + File.separator + relativePath));
    }
    
    public static void loadModel(PSEngine engine, String nddlFile) throws PSLanguageExceptionList {
        String nddlInclude = modelDir;
        
        if( nddlFile.contains("/") ) {
            String[] folders = nddlFile.split("/");
            String folder = "";
            
            for (int i = 0; i < folders.length; i++) {
                folder += File.separator+folders[i];
                if (new File(modelDir+folder).isDirectory())
                    nddlInclude+= ":"+modelDir+folder;
            }
        }
       engine.getConfig().setProperty("nddl.includePath", nddlInclude);
       engine.executeScript("nddl", modelDir + File.separator + nddlFile, true);        
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
        return solver.getFlaws().size() == 0;
    }

    public static boolean succeeded(PSSolver solver) {
        return complete(solver) && !failed(solver);
    }

    public static boolean step(PSSolver solver) {
        if (failed(solver))
            return false;
        else {
            solver.step();
            return (!complete(solver) && !failed(solver));
        }
    }

    public static String printFlaws(PSSolver solver) {
        String flaws = "";
        PSStringList fls = solver.getFlaws();
        for (int j = 0; j < fls.size(); j++)
            flaws +=("  - " + fls.get(j)+"\n");
        return flaws;
    }

    protected static String locateLibrary(String lib) throws Exception {
        String lookFor = System.mapLibraryName(lib);
        Vector<String> path = new Vector<>();
        
        switch (CheckJavaOSArch.getOs()) {
            case "linux-x64":
                path.add(new File("libJNI/europa/x64").getAbsolutePath());
                break;
            default:
                break;
        }
        
        String ldPath = System.getenv("LD_LIBRARY_PATH"), europa = System.getenv("EUROPA_HOME");
        // Check for explicit info about europa location
        if( europa==null ) {
            // Attempt to get PLASMA_HOME instead
            europa = System.getenv("PLASMA_HOME");
        }     
        if (europa != null && new File(europa).isDirectory())
            path.add(europa + File.separator + "lib");
        // Get the java library path where jnilibs are expected to be
        path.addAll(Arrays.asList(System.getProperty("java.library.path").split(File.pathSeparator)));
        // finally add LD_LIBRARY_PATH if it exists
        if( ldPath != null ) {        
            path.addAll(Arrays.asList(ldPath.split(File.pathSeparator)));
        }
        
        // Now iterate through all these paths to locate the library
        for (String s : path) {
            File f = new File(s, lookFor);
            if ( f.exists() ) {
                // found it => return the fully qualified path
                return f.getAbsolutePath();
            }
        }
        // If we reach this point the library was nowhere to be found
        throw new FileNotFoundException("Library "+System.mapLibraryName(lib)+" was not found in "
                    +StringUtils.join(path, File.pathSeparator));
    }

    public static String loadLibrary(String lib) throws Exception {
        // Loook for it
        String library = locateLibrary(lib);
        
        NeptusLog.pub().info("native library loaded from "+library+".");
        // load the library directly
        System.load(library);
        return library;
    }
    
    /**
     * Given n points (x0,y0)...(xn-1,yn-1), the following methid computes the polynomial factors of the n-1't degree
     * polynomial passing through the n points.
     * 
     * Example: Passing in three points (2,3) (1,4) and (3,7) will produce the results [2.5, -8.5, 10] which means that
     * the points is on the curve y = 2.5x² - 8.5x + 10.
     * 
     * @author <a href="mailto:info@geosoft.no">GeoSoft</a>
     */
    public static double[] findPolynomialFactors(double[] x, double[] y) throws RuntimeException {
        int n = x.length;

        double[][] data = new double[n][n];
        double[] rhs = new double[n];

        for (int i = 0; i < n; i++) {
            double v = 1;
            for (int j = 0; j < n; j++) {
                data[i][n - j - 1] = v;
                v *= x[i];
            }

            rhs[i] = y[i];
        }

        // Solve m * s = b

        Matrix m = new Matrix(data);
        Matrix b = new Matrix(rhs, n);

        Matrix s = m.solve(b);

        return s.getRowPackedCopy();
    }

    public static String loadModule(PSEngine europa, String lib) throws Exception {
        // Look for it
        String library = locateLibrary(lib);
        
        NeptusLog.pub().info("europa native extension library loaded from "+library+".");
        // add the extension to the europa engine this will:
        //   - load the library
        //   - call a special function that will create the module class
        //   - initialize the module class for this engine attaching all the extensions
        europa.loadModule(library);
        return library;
    }
    
    public static void main(String[] args) {
        double x[] = new double[] {0.7, 1.1, 1.3}, y[] = new double[] {28449,18104,15319};
        double sol[] = findPolynomialFactors(x, y);
        
        for (int i = 0; i < sol.length; i++) {
            System.out.println(sol[i]);
            System.out.println((sol[0] * 1.2*1.2)+(sol[1] * 1.2)+sol[2]+" == "+y[i]);
        }
        
        //System.out.println(clearVarName("lau0v_sxplore-1"));
        //EuropaUtils.createPlanner("neptus/auv_model.nddl");
    }
}
