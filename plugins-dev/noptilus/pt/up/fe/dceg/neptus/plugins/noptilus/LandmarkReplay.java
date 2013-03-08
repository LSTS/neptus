/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by zp
 * Oct 17, 2012
 * $Id:: LandmarkReplay.java 9615 2012-12-30 23:08:28Z pdias                    $:
 */
package pt.up.fe.dceg.neptus.plugins.noptilus;

import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Vector;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.plugins.noptilus.LandmarkUtils.LANDMARK;
import pt.up.fe.dceg.neptus.renderer2d.Renderer2DPainter;
import pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D;
import pt.up.fe.dceg.neptus.types.coord.LocationType;

/**
 * @author noptilus
 */
public class LandmarkReplay implements Renderer2DPainter {

    protected BufferedReader replayReader = null;
    protected int numMarks, lineno;
    protected LANDMARK[] state;
    protected StateRenderer2D renderer = null;
    protected Vector<LocationType> locations = new Vector<LocationType>();
    protected Thread replayThread = null;
        
    public LandmarkReplay(File positions, File states, long timestepMillis) throws Exception {

        LandmarkUtils.loadLandmarks(positions, locations);
        numMarks = locations.size();
        state = new LANDMARK[numMarks];
        for (int i = 0 ; i < numMarks; i++)
            state[i] = LANDMARK.UNKNOWN;        

        if (states != null) {
            replayReader = new BufferedReader(new FileReader(states));
            String firstLine = replayReader.readLine();
            String[] parts = firstLine.split("\\s+");        
            lineno = 1;

            updateState(parts);        
            replayThread = startReplay(timestepMillis);
        }        
    }
    
    public void cleanup() {
        if (replayThread != null)
            replayThread.interrupt();
    }

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        this.renderer = renderer;

        for (int i = 0; i < locations.size(); i++) {
            g.setColor(state[i].getColor());
            Point2D pt = renderer.getScreenPosition(locations.get(i));
            g.draw(new Line2D.Double(pt.getX()-1, pt.getY()-1, pt.getX()+1, pt.getY()+1));
            g.draw(new Line2D.Double(pt.getX()+1, pt.getY()-1, pt.getX()-1, pt.getY()+1));
        }        
    }

    protected void updateState(String[] parts) {
        int j = 0;
        for (int i = 0; i < parts.length; i++)
            if (!parts[i].isEmpty())
                state[j++] = LandmarkUtils.getLandmarkState(Integer.parseInt(parts[i]));
    }

    public Thread startReplay(final long timestepMillis) {
        Thread t = new Thread("Landmark replay thread") {            
            public void run() {
                String line = null;

                try { line = replayReader.readLine(); }
                catch (Exception e) {
                    e.printStackTrace();
                }

                while (line != null) {
                    line = line.replaceAll("\\s+", "\t");
                    lineno++;
                    String[] parts = line.split("\\t");
                    updateState(parts);    

                    if (renderer != null)
                        renderer.repaint();

                    try { 
                        Thread.sleep(timestepMillis);
                        line = replayReader.readLine(); 
                    }
                    catch (InterruptedException e) {
                        NeptusLog.pub().info("replay stopped");
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };
        };
        t.start();
        return t;
    }
}
