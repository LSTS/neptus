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
 * Author: Sérgio Ferreira
 * 9 de Abr de 2012
 */
package pt.lsts.neptus.plugins.uavs.panels;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.Hashtable;

import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.plugins.NeptusMessageListener;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.uavs.UavPaintersBag;
import pt.lsts.neptus.plugins.uavs.interfaces.IUavPainter;
import pt.lsts.neptus.plugins.uavs.painters.background.UavVirtualHorizonPainter;
import pt.lsts.neptus.plugins.uavs.painters.foreground.UavHUDInfoPainter;
import pt.lsts.neptus.util.MathMiscUtils;

//change to Engineer console

/**
 * @author sergiofereira
 * @version 0.1
 * @category UavPanel
 */
@PluginDescription(name = "Uav HUD Panel", icon = "pt/lsts/neptus/plugins/uavs/wbutt.png", author = "sergioferreira")
public class UavHUDPanel extends ConsolePanel implements NeptusMessageListener {

    private static final long serialVersionUID = 1L;

    // different layers to be painted on top of the panel's draw area
    private UavPaintersBag layers;

    // arguments passed to each layer in the painting phase, in order to provide them with necessary data to allow
    // rendering
    private Hashtable<String, Object> args;

    // active main vehicle's current indicated speed
    private double indicatedSpeed;

    // active main vehicle's current heading angle
    private double tmpVar;
    
    public UavHUDPanel(ConsoleLayout console) {
        super(console);

        // clears all the unused initializations of the standard SimpleSubPanel
        removeAll();
    }

    // ------Setters and Getters------//

    // Layers
    private void setLayers(UavPaintersBag layers) {
        this.layers = layers;
    }

    public UavPaintersBag getLayers() {
        return layers;
    }

    // Args
    private void setArgs(Hashtable<String, Object> args) {
        this.args = args;
    }

    // ------Implemented Interfaces------//

    // NeptusMessageListener_BEGIN
    @Override
    public String[] getObservedMessages() {
        return new String[] { "EstimatedState", "IndicatedSpeed" };
    }

    @Override
    public void messageArrived(IMCMessage message) {
//        indicatedSpeed = Math.sqrt(Math.pow(message.getDouble("u"), 2) + Math.pow(message.getDouble("v"), 2)
//                + Math.pow(message.getDouble("w"), 2));
        
        if (message.getAbbrev().equals("IndicatedSpeed")) {
            indicatedSpeed = message.getDouble("value");
            args.put("indicatedSpeed", MathMiscUtils.round(indicatedSpeed, 1));
        }
        else {
            args.put("altitude", (message.getInteger("height")) - (message.getInteger("z")));
            args.put("roll", Math.toDegrees(message.getDouble("phi")));
            args.put("pitch", Math.toDegrees(message.getDouble("theta")));

            tmpVar = Math.toDegrees(message.getDouble("psi"));

            if (tmpVar < 0)
                tmpVar = 360 + tmpVar;

            args.put("yaw", tmpVar);
        }

        repaint();
    }

    // NeptusMessageListener_END

    // ------Specific Methods------//

    /**
     * Method adds a new painter layer to the UavPainterBag
     * 
     * @param Name Name of the painter to be added to the UavPainterBag
     * @param Priority Drawing priority, the higher the number, the higher the priority.
     * @param Layer UavPainter responsible for painting the specific layer
     * @param CacheMillis Refresh rate in mili-seconds *
     * @return Void
     */
    public void addLayer(String name, Integer priority, IUavPainter layer, int cacheMillis) {
        this.layers.addPainter(name, layer, priority, cacheMillis);
    }

    @Override
    public void cleanSubPanel() {
    }

    @Override
    public void initSubPanel() {
        setLayers(new UavPaintersBag());
        setArgs(new Hashtable<String, Object>());

        // sets up all the layers used by the panel
        addLayer("Virtual Horizon Painter", 1, new UavVirtualHorizonPainter(), 0);
        addLayer("Uav HUDInfo Painter", 2, new UavHUDInfoPainter(), 0);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        synchronized (layers) {
            for (IUavPainter layer : layers.getPostRenderPainters()) {
                Graphics2D gNew = (Graphics2D) g.create();
                layer.paint(gNew, this.getWidth(), this.getHeight(), args);
                gNew.dispose();
            }
        }
    }
}
