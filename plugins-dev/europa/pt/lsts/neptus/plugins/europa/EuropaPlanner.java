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

import psengine.PSConstraint;
import psengine.PSConstraintList;
import psengine.PSEngine;
import psengine.PSLanguageExceptionList;
import psengine.PSObject;
import psengine.PSObjectList;
import psengine.PSPlanDatabaseClient;
import psengine.PSPlanDatabaseListener;
import psengine.PSSolver;
import psengine.PSToken;
import psengine.PSTokenList;
import psengine.PSVarValue;
import psengine.PSVariable;
import psengine.PSVariableList;

/**
 * @author zp
 * 
 */
public class EuropaPlanner {

    private static final File models = new File("conf/nddl");
    
    public static void main(String args[]) throws Exception {

        EuropaUtils.loadLibrary("System_o");
        EuropaUtils.loadLibrary("Neptus");
        final PSEngine europa = makePSEngine("o");
        europa.start();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                europa.shutdown();
            }
        });

        europa.getConfig().setProperty("nddl.includePath", ".:" + models.getAbsolutePath() + "/neptus");
        europa.addPlanDatabaseListener(new PSPlanDatabaseListener() {
            @Override
            public void notifyMerged(PSToken token) {
                System.out.println(token.getEntityKey() + " got merged with " + token.getActive().getEntityKey());
            }

            @Override
            public void notifyDeactivated(PSToken token) {
                System.out.println(token.getEntityKey() + " got deactivated");
            }

            @Override
            public void notifyActivated(PSToken token) {
                System.out.println(token.getEntityKey() + " got activated");
            }

            @Override
            public void notifySplit(PSToken token) {
                System.out.println(token.getEntityKey() + " got split");
            }

            @Override
            public void notifyRejected(PSToken token) {
                System.out.println(token.getEntityKey() + " got rejected");
            }
        });

        try {
            String res = europa
                    .executeScript("nddl", models.getAbsolutePath() + "/neptus/auv_model.nddl", true/* isFile */);

            europa.executeScript("nddl", "Auv xtreme1 = new Auv();", false);

            // A task starting at (0.0, 0.0) with a 2km length and same exit as the entry
            europa.executeScript("nddl", "Task t_01032433 = new Task(0.0,0.0,0.0,0.0,2000.0);", false);

            PSPlanDatabaseClient pdb = europa.getPlanDatabaseClient();
            pdb.close();

            // Here I am going to set the AUV initial location
            PSObject xtreme1 = europa.getObjectByName("xtreme1"), xt1_pos;
            // Stupid europa require the full name of the attribute prefixed with the object name ...
            PSVariable var = xtreme1.getMemberVariable(xtreme1.getEntityName() + ".position");
            // I need this to make sure that the location will ba associeted to xtreme1
            xt1_pos = PSObject.asPSObject(var.getSingletonValue().asObject());

            // Create my factual position
            PSToken tok = pdb.createToken("Position.Pos", false, true);
            // specify that it is the position of xtreme1
            tok.getParameter("object").specifyValue(xt1_pos.asPSVarValue());
            // specify that this happens at the tick 0
            tok.getStart().specifyValue(PSVarValue.getInstance(0));

            // Make the vehicle starts at (-0.1, 0.02) which is 11km from (0.0,0.0)
            tok.getParameter("latitude").specifyValue(PSVarValue.getInstance(-0.1));
            tok.getParameter("longitude").specifyValue(PSVarValue.getInstance(0.02));
            tok.getParameter("depth").specifyValue(PSVarValue.getInstance(0.0));

            // Now I create s survey goal
            PSToken g_tok = pdb.createToken("Auv.Execute", true, false);
            String tok_name = g_tok.getEntityName();
            // If I wanted to force it to xtreme1 I would do:
            // g_tok.getParameter("object").specifyValue(xtreme1.asPSVarValue());
            // that task is the only one I defined
            g_tok.getParameter("task").specifyValue(europa.getObjectByName("t_01032433").asPSVarValue());
            // the speed is 1.5m/s
            g_tok.getParameter("speed").specifyValue(PSVarValue.getInstance(1.5));

            europa.executeScript("nddl", tok_name+".start < 1000;", false);
            
            runSolver(europa);

        }
        catch (PSLanguageExceptionList e) {
            for (int i = 0; i < e.getExceptionCount(); i++)
                System.err.println(e.getException(i).getFileName() + " (" + e.getException(i).getLine() + "): "
                        + e.getException(i).getMessage());
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        // try {
        // String res = europa.executeScript("nddl", models.getAbsolutePath()+"/Light/Light-initial-state.nddl", true/*
        // isFile */);
        //
        // // Testing creation of goal programatically
        // //
        // // PSToken tok = europa.getPlanDatabaseClient().createToken("LightBulb.Off", true, false);
        // // tok.getStart().specifyValue(PSVarValue.getInstance(10));
        // // System.out.println(tok.getEntityKey());
        // // System.out.println(tok.getTokenState());
        // // // europa.getPlanDatabaseClient().reject(tok);
        // runSolver(europa);
        // //System.out.println(tok.getTokenState());
        // }
        // catch (PSLanguageExceptionList e) {
        // for (int i = 0; i < e.getExceptionCount(); i++)
        // System.err.println(e.getException(i).getFileName() + " (" + e.getException(i).getLine() + "): "
        // + e.getException(i).getMessage());
        // }
        europa.shutdown();
    }

    /*
     * debugMode = "g" for debug, "o" for optimized
     */
    static PSEngine makePSEngine(String debugMode) {
        PSEngine psEngine;
        //LibraryLoader.loadLibrary("System_" + debugMode);
        psEngine = PSEngine.makeInstance();

        return psEngine;
    }

    static void runSolver2(PSEngine europa) {
        String plannerConfig = models.getAbsolutePath() + "/neptus/PlannerConfig.xml";
        int startHorizon = 0, endHorizon = 100;

        PSSolver solver = europa.createSolver(plannerConfig);
        solver.configure(startHorizon, endHorizon);

        europa.getConfig().readFromXML(models.getAbsolutePath() + "/neptus/NDDL.cfg", true);
        int maxSteps = 1000;
        int i = 0;
        for (i = 0; !solver.isExhausted() && !solver.isTimedOut() && i < maxSteps; i = solver.getStepCount()) {
            solver.step();
            System.out.println("Step");
            if (solver.getFlaws().size() == 0) {
                System.out.println("No flaws");

                PSObjectList objs = europa.getObjects();
                for (int j = 0; j < objs.size(); j++) {
                    PSObject obj = objs.get(j);
                    System.out.println(obj.getEntityType() + "." + obj.getEntityName() + ":");
                    PSTokenList tokens = obj.getTokens();
                    for (int k = 0; k < tokens.size(); k++) {
                        // System.out.println("\t"+tokens.get(k).getFullTokenType()+"."+tokens.get(k).);
                    }

                }
                System.out.println(europa.planDatabaseToString());

                break; // we're done!
            }

            if (solver.isExhausted())
                debugMsg("Solver was exhausted after " + i + " steps");
            else if (solver.isTimedOut())
                debugMsg("Solver timed out after " + i + " steps");
            else
                debugMsg("Solver finished after " + i + " steps");
        }
    }

    static void runSolver(PSEngine europa) {
        String plannerConfig = models.getAbsolutePath() + "/neptus/PlannerConfig.xml";
        int startHorizon = 0, endHorizon = 100;

        PSSolver solver = europa.createSolver(plannerConfig);
        solver.configure(startHorizon, endHorizon);

        europa.getConfig().readFromXML(models.getAbsolutePath() + "/neptus/NDDL.cfg", true);
        int maxSteps = 1000;
        int i = 0;
        for (i = 0; !solver.isExhausted() && !solver.isTimedOut() && i < maxSteps; i = solver.getStepCount()) {
//            System.out.println("Flaws: ");
//            PSStringList fls = solver.getFlaws();
//            for (int j = 0; j < fls.size(); j++)
//                System.out.println("  - " + fls.get(j));
//            System.out.println();

            solver.step();
            // System.out.println();
            // System.out.println(europa.planDatabaseToString());
            // System.out.println();
            System.out.println("Step");
            if (solver.getFlaws().size() == 0) {
                System.out.println("No flaws");

                PSObjectList objs = europa.getObjects();
                for (int j = 0; j < objs.size(); j++) {
                    PSObject obj = objs.get(j);
                    System.out.println(obj.getEntityType() + "." + obj.getEntityName() + ":");
                    PSTokenList tokens = obj.getTokens();
                    for (int k = 0; k < tokens.size(); k++) {
                        // System.out.println("\t"+tokens.get(k).getFullTokenType()+"."+tokens.get(k).);
                        PSToken cur = tokens.get(k);

                        PSVariable start = cur.getStart();
                        PSConstraintList cstrs = start.getConstraints();
                        System.out.println(cur.toString() + ".start(" + start.getEntityKey() + "):");
                        for (int l = 0; l < cstrs.size(); l++) {
                            PSConstraint c = cstrs.get(l);

                            System.out.print("\t" + c.getEntityName() + "(");
                            PSVariableList args = c.getVariables();
                            for (int m = 0; m < args.size(); m++) {
                                PSVariable tmp = args.get(m);
                                if (m > 0)
                                    System.out.print(", ");
                                System.out.print(tmp.getEntityName() + "(" + tmp.getEntityKey() + ")");
                                if (tmp.isSingleton()) {
                                    System.out.print(" = {" + tmp.getSingletonValue() + "}");
                                }
                                else if (tmp.isInterval()) {
                                    System.out.print(" = [" + tmp.getLowerBound() + ", " + tmp.getUpperBound() + "]");
                                }
                            }
                            System.out.println(")");
                        }
                    }

                }
                System.out.println(europa.planDatabaseToString());

                break; // we're done!
            }

            if (solver.isExhausted())
                debugMsg("Solver was exhausted after " + i + " steps");
            else if (solver.isTimedOut())
                debugMsg("Solver timed out after " + i + " steps");
            else
                debugMsg("Solver finished after " + i + " steps");
        }
    }

    static void debugMsg(String msg) {
        System.out.println(msg);
    }
}
