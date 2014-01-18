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
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
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
 * Author: Christian Fuchs
 * 15.10.2012
 */
package pt.lsts.neptus.plugins.uavs.panels;

import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.Hashtable;

import javax.swing.JLabel;

import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.plugins.NeptusMessageListener;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.uavs.UavPaintersBag;
import pt.lsts.neptus.plugins.uavs.interfaces.IUavPainter;
import pt.lsts.neptus.plugins.uavs.painters.background.UavAirspeedCoverLayerPainter;
import pt.lsts.neptus.plugins.uavs.painters.foreground.UavRulerPainter;
import pt.lsts.neptus.plugins.uavs.painters.foreground.UavVehicleAirspeedPainter;

/**
 * @author Christian Fuchs
 * @version 0.2
 * @category UavPanel Neptus panel which allows the console operator see the current airspeed of the currently active
 *           UAV.
 */

@PluginDescription(name = "Uav Airspeed Panel", icon = "pt/lsts/neptus/plugins/uavs/planning.png", author = "Christian Fuchs")
public class UavAirspeedPanel extends ConsolePanel implements NeptusMessageListener {

    private static final long serialVersionUID = 1L;

    // active main vehicle's current indicated airspeed
    private double indicatedAirspeed;

    // active main vehicle's maximum airspeed
    // Needed to set the panels speed range
    private final double MAX_AIRSPEED = 30;

    // arguments passed to each layer in the painting phase, in order to provide them with necessary data to allow
    // rendering
    private Hashtable<String, Object> args;

    // different layers to be painted on top of the panel's draw area
    private UavPaintersBag layers;

    // structure used to house the pixels per mark and marking grade for drawing purposes
    private Point pixelsPerMark_markGrade_Pair;

    // predetermined base height for each ruler mark
    public static final int MARK_MIN_HEIGHT = 4;
    
    //JLabel that shows the title of the panel
    JLabel title;
    
    //space left at the bottom of the panel to put the title
    private final int TITLE_SPACE = 30;
    
//------Setters and Getters------//
    
    //Layers
    private void setLayers(UavPaintersBag layers) {
        this.layers = layers;
    }

    public UavPaintersBag getLayers() {
        return layers;
    }

    // PixelsPerMark_markGrade_Pair
    private void setPixelsPerMark_markGrade_Pair(Point point) {
        this.pixelsPerMark_markGrade_Pair = point;
    }

    // Args
    private void setArgs(Hashtable<String, Object> args) {
        this.args = args;
    }

    // -------End of Setters and Getters------//

    public UavAirspeedPanel(ConsoleLayout console) {
        super(console);

        // clears all the unused initializations of the standard SimpleSubPanel
        removeAll();
    }

    // listener object which allows the panel to tap into the various IMC messages
    // start of IMC message stuff
    public String[] getObservedMessages() {
        return new String[] { "EstimatedState" };
    }

    @Override
    public void messageArrived(IMCMessage message) {
        indicatedAirspeed = Math.sqrt(Math.pow(message.getDouble("u"), 2) + Math.pow(message.getDouble("v"), 2)
                + Math.pow(message.getDouble("w"), 2));
        args.put("indicatedAirspeed", indicatedAirspeed);
        repaint();
    }

    // end of IMC message stuff

    // Method adds a new painter layer to the UavPainterBag
    public void addLayer(String name, Integer priority, IUavPainter layer, int cacheMillis) {
        this.layers.addPainter(name, layer, priority, cacheMillis);
    }

    // Method that computes the number of pixels per marking bar in speed ruler
    private void determinePixelsPerMark(Point ret, int height, int rulerMax, int minMarkHeight) {

        rulerMax = updateRulerMax(rulerMax);

        ret.x = 0;
        ret.y = 1;
        int i = 0;

        while ((ret.x = height / (rulerMax / ret.y)) < 2 * minMarkHeight) {
            switch (i % 2) {
                case 0:
                    ret.y *= 5;
                    break;
                default:
                    ret.y *= 2;
                    break;
            }
            i++;
        }
    }

    // Method that computes the maximum airspeed on the ruler
    // Makes sure the maximum covers at least the current airspeed but at max 1.2 * the maximum airspeed
    // Set MAX_AIRSPEED to change the limit
    private int updateRulerMax(int ret) {

        if (ret > 1.2 * MAX_AIRSPEED) {
            ret = ((Double) (1.2 * MAX_AIRSPEED)).intValue();
        }

        if (ret < indicatedAirspeed) {
            ret = ((Double) indicatedAirspeed).intValue();
        }

        if (ret % 10 != 0) {
            ret = ((Number) (Math.floor(ret / 10) * 10 + 10)).intValue();
        }
        return ret;
    }

    // prepare the arguments for the UavVehicleAirspeedPainter
    // must have fields "vehicles" with value VehicleID and Airspeed
    // the airspeed is multiplied by 10 first so that the drawing is more accurate
    private void prepareArgs() {
        args.put("markInfo", pixelsPerMark_markGrade_Pair);

        Hashtable<String, Integer> singleUav = new Hashtable<String, Integer>();
        singleUav.put(this.getMainVehicleId(), ((Double) (indicatedAirspeed * 10)).intValue());

        args.put("vehicles", singleUav);
    }

    @Override
    public void initSubPanel() {

        setLayers(new UavPaintersBag());
        setArgs(new Hashtable<String, Object>());
        setPixelsPerMark_markGrade_Pair(new Point());

        // sets up all the layers used by the panel
        addLayer("Cover Layer Painter", 1, new UavAirspeedCoverLayerPainter(), 0);
        addLayer("Ruler Painter", 2, new UavRulerPainter(5), 0);
        addLayer("Uav Vehicle Airspeed Painter", 3, new UavVehicleAirspeedPainter(), 0);
        
        //creates the title label
        title = new JLabel("IAS [m/s]", JLabel.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD));
        add(title);
        title.setBounds(0, this.getHeight() - TITLE_SPACE, this.getWidth(), TITLE_SPACE);
    }

    @Override
    public void cleanSubPanel() {
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // updates the UavRulerPainter's maximum airspeed
        determinePixelsPerMark(pixelsPerMark_markGrade_Pair,this.getHeight() - TITLE_SPACE,((Number)(Math.floor((this.getHeight() - TITLE_SPACE)/100)*100)).intValue(),MARK_MIN_HEIGHT);

        prepareArgs();

        synchronized (layers) {
            for (IUavPainter layer : layers.getPostRenderPainters()) {
                Graphics2D gNew = (Graphics2D) g.create();
                layer.paint(gNew, this.getWidth(), this.getHeight() - TITLE_SPACE, args);
                title.setBounds(0, this.getHeight() - TITLE_SPACE, this.getWidth(), TITLE_SPACE);
                gNew.dispose();
            }
        }
    }

}
