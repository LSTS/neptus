/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
 * Author: José Pinto
 * Oct 17, 2012
 */
package pt.lsts.neptus.plugins.noptilus;

import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Vector;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.plugins.noptilus.LandmarkUtils.LANDMARK;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;

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
