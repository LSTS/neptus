/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by Margarida Faria
 * Jun 14, 2012
 */
package pt.up.fe.dceg.neptus.plugins.r3d;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.plugins.r3d.jme3.spacials.Water;

/**
 * Class used to check the memory usage of the 3D plugin
 * 
 * @author Margarida Faria
 * 
 */
public class PerformanceTest {
    public static long initialMem = 0;
    public static long maxMem = 0;
    private final static SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss");
    private static StringBuilder outBuilder;
    private static String file = "/home/meg/Desktop/log.txt";

    /**
     * Star for taking the initial measurement and update for the following
     * 
     * @author Margarida Faria
     * 
     */
    public enum PrintType {
        UPDATE, START;
    }

    /**
     * Takes the measurements at this instance and then prints it to the screen in the format [time] [type] [dimensions]
     * [Total memory (bytes)]
     * 
     * @param type Start or Update
     * @param dimensions the size of the canvas in use
     * @param file the path to the file in use
     */
    public static void printToLog(PrintType type, String dimensions) {
        // get current memory
        long totalMemory = Runtime.getRuntime().totalMemory();
        long delta = totalMemory - initialMem;
        // get current time
        Date date = new Date();
        // print only if it's using more memory
        if (delta > maxMem) {
            maxMem = delta;
            outBuilder = new StringBuilder();
            outBuilder.append(sdf.format(date));
            outBuilder.append(" ");
            outBuilder.append(dimensions);
            outBuilder.append(" ");
            outBuilder.append(type);
            outBuilder.append(" ");
            outBuilder.append(totalMemory);
            outBuilder.append(" ");
            outBuilder.append(delta);
            outBuilder.append("\n");
            print(outBuilder.toString());
        }
    }

    /**
     * Takes the measurements at this instance and then prints it to the screen in the format [time] [type] [dimensions]
     * [Total memory (bytes)]
     * 
     * @param type Start or Update
     * @param height of the canvas in use
     * @param width of the canvas in use
     * @param file the path to the file in use
     */
    public static void printToLog(PrintType type, int height, int width) {
        printToLog(type, "[" + height + ", " + width + "]");
    }

    /**
     * Print header when terrain definitions change.
     * 
     * @param terrainType
     * @param water
     * @param sky
     */
    public static void changedTerrainDefs(boolean terrainType, Water water, boolean sky) {
        maxMem = 0;
        print("\n-- ColorShader:" + terrainType + "   - " + water + " - show sky:" + sky + " --\n");
        printToLog(PrintType.UPDATE, "");
    }

    private static void print(String message) {
        try {
            // Create file
            FileWriter fstream = new FileWriter(file, true);
            BufferedWriter out = new BufferedWriter(fstream);
            out.write(message);
            // Close the output stream
            out.close();
        }
        catch (Exception e) {
            NeptusLog.pub().error(I18n.text("Error while writting ") + message + ": " + e.getMessage());
        }
    }

    /**
     * Get's initial memory size. Prints a line to signal the beginning of the log.
     */
    public static void initMem() {
        initialMem = Runtime.getRuntime().totalMemory();
        NeptusLog.pub().debug(I18n.text("initialMemory") + ":" + initialMem);
        outBuilder = new StringBuilder();
        outBuilder.append(sdf.format(new Date()));
        outBuilder.append(" ");
        outBuilder.append(PrintType.START);
        outBuilder.append(" ");
        outBuilder.append(initialMem);
        outBuilder.append("\n");
        print(outBuilder.toString());
    }
}
