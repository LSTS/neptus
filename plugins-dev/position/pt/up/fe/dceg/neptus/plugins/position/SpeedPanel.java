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
 * 2009/06/03
 */
package pt.up.fe.dceg.neptus.plugins.position;

import java.awt.BorderLayout;
import java.awt.Color;
import java.text.DecimalFormat;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.plugins.ConfigurationListener;
import pt.up.fe.dceg.neptus.plugins.NeptusMessageListener;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.SimpleSubPanel;
import pt.up.fe.dceg.neptus.plugins.update.IPeriodicUpdates;

/**
 * @author zp
 * @author pdias
 */
@SuppressWarnings("serial")
@PluginDescription(name = "Vehicle Speed", author = "ZP and Paulo Dias", icon = "pt/up/fe/dceg/neptus/plugins/position/position.png", description = "Shows the Vehicle's speed")
public class SpeedPanel extends SimpleSubPanel implements IPeriodicUpdates, ConfigurationListener,
        NeptusMessageListener {

    private DisplayPanel displaySpeed;
    private DecimalFormat formatter = new DecimalFormat("0.00");
    private long lastUpdate = 0;

    @NeptusProperty(name = "Update interval", description = "Interval between updates in milliseconds")
    public long millisBetweenUpdates = 100;

    @NeptusProperty(name = "True or Indicated Speed", description = "true for True and false for Indicated Speed")
    public boolean isTrueSpeed = true;

    @NeptusProperty(name = "SI Unit or knots", description = "true for m/s and false for knot")
    public boolean isSIUnit = true;

    @NeptusProperty(name = "Font Size", description = "The font size. Use '0' for automatic.")
    public int fontSize = DisplayPanel.DEFAULT_FONT_SIZE;

    protected final double m_sToKnotConv = 1.94384449244;

    private String partSpeedTxt = " speed ";

//    @Override
//    public String[] variablesToListen() {
//        return new String[] { "EstimatedState.vx", "EstimatedState.vy", "EstimatedState.vz", "EstimatedState.u",
//                "EstimatedState.v", "EstimatedState.w" };
//    }

    public SpeedPanel(ConsoleLayout console) {
        super(console);
        initialize();
    }

    protected void initialize() {
        displaySpeed = new DisplayPanel((isTrueSpeed ? "true" : "ind.") + partSpeedTxt
                + (isSIUnit ? "(m/s)" : "(knot)"));
        displaySpeed.setFontSize(fontSize);
        setLayout(new BorderLayout());
        add(displaySpeed, BorderLayout.CENTER);
    }

    @Override
    public void propertiesChanged() {
        displaySpeed.setFontSize(fontSize);
        invalidate();
        revalidate();
    }

    @Override
    public long millisBetweenUpdates() {
        return 500;
    }

    boolean connected = true;

    @Override
    public boolean update() {

        if (connected && System.currentTimeMillis() - lastUpdate > 3000) {
            displaySpeed.setFontColor(Color.red.darker());
            connected = false;
        }

        if (!connected && System.currentTimeMillis() - lastUpdate < 3000) {
            displaySpeed.setFontColor(Color.black);
            connected = true;
        }
        return true;
    }

    @Override
    public void cleanSubPanel() {
    }

    @Override
    public String[] getObservedMessages() {
        return new String[] { "EstimatedState" };
    }

    @Override
    public void messageArrived(IMCMessage message) {
        double u, v, w;
        
        if (isTrueSpeed) {
            u = message.getDouble("vx");
            v = message.getDouble("vy");
            w = message.getDouble("vz");
        }
        else {
            u = message.getDouble("u");
            v = message.getDouble("v");
            w = message.getDouble("w");
        }

        double res = Math.sqrt(u * u + v * v + w * w);
        res = (isSIUnit ? res : res * m_sToKnotConv);
        displaySpeed.setText(formatter.format(res));
        displaySpeed.setTitle((isTrueSpeed ? "true" : "ind.") + partSpeedTxt + (isSIUnit ? "(m/s)" : "(knot)"));
        lastUpdate = System.currentTimeMillis();
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.plugins.SimpleSubPanel#initSubPanel()
     */
    @Override
    public void initSubPanel() {
        // TODO Auto-generated method stub
        
    }
}
