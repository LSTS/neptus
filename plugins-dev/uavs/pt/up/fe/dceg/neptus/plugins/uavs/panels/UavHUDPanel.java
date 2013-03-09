/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by sergioferreira
 * 9 de Abr de 2012
 */
package pt.up.fe.dceg.neptus.plugins.uavs.panels;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.Hashtable;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.plugins.NeptusMessageListener;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.SimpleSubPanel;
import pt.up.fe.dceg.neptus.plugins.uavs.UavPaintersBag;
import pt.up.fe.dceg.neptus.plugins.uavs.interfaces.IUavPainter;
import pt.up.fe.dceg.neptus.plugins.uavs.painters.background.UavVirtualHorizonPainter;
import pt.up.fe.dceg.neptus.plugins.uavs.painters.foreground.UavHUDInfoPainter;

/**
 * @author sergiofereira
 * @version 0.1
 * @category UavPanel
 */
@PluginDescription(name = "Uav HUD Panel", icon = "pt/up/fe/dceg/neptus/plugins/uavs/wbutt.png", author = "sergioferreira")
public class UavHUDPanel extends SimpleSubPanel implements NeptusMessageListener {

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
        return new String[] { "EstimatedState" };
    }

    @Override
    public void messageArrived(IMCMessage message) {

        indicatedSpeed = Math.sqrt(Math.pow(message.getDouble("u"), 2) + Math.pow(message.getDouble("v"), 2)
                + Math.pow(message.getDouble("w"), 2));

        args.put("indicatedSpeed", indicatedSpeed);
        // FIXME Now working with positive altitude; correct painter
        args.put("altitude", -(message.getInteger("height")));
        args.put("roll", Math.toDegrees(message.getDouble("phi")));
        args.put("pitch", Math.toDegrees(message.getDouble("theta")));

        tmpVar = Math.toDegrees(message.getDouble("psi"));

        if (tmpVar < 0)
            tmpVar = 360 + tmpVar;

        args.put("yaw", tmpVar);

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
