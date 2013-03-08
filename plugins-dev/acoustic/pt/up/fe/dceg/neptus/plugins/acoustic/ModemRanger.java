/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by zepinto
 * 2010/02/18
 * $Id:: ModemRanger.java 9615 2012-12-30 23:08:28Z pdias                       $:
 */
package pt.up.fe.dceg.neptus.plugins.acoustic;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.text.DecimalFormat;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.gui.PropertiesEditor;
import pt.up.fe.dceg.neptus.gui.ToolbarButton;
import pt.up.fe.dceg.neptus.imc.IMCDefinition;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.mystate.MyState;
import pt.up.fe.dceg.neptus.plugins.ConfigurationListener;
import pt.up.fe.dceg.neptus.plugins.IAbortSenderProvider;
import pt.up.fe.dceg.neptus.plugins.NeptusMessageListener;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.PluginUtils;
import pt.up.fe.dceg.neptus.plugins.SimpleSubPanel;
import pt.up.fe.dceg.neptus.renderer2d.ILayerPainter;
import pt.up.fe.dceg.neptus.renderer2d.LayerPriority;
import pt.up.fe.dceg.neptus.renderer2d.Renderer2DPainter;
import pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.ImageUtils;

/**
 * @author zepinto
 * 
 */
@SuppressWarnings("serial")
@PluginDescription(author = "zp", name = "Acoustic Modem Ranger", description = "Allows to query acoustic modem for ranges to other devices", icon = "pt/up/fe/dceg/neptus/plugins/acoustic/lbl.png")
@LayerPriority(priority = 40)
public class ModemRanger extends SimpleSubPanel implements ConfigurationListener, ActionListener,
        NeptusMessageListener, Renderer2DPainter, IAbortSenderProvider {

    @NeptusProperty(hidden = true)
    public int targetId = 0;

    @NeptusProperty(name = "Name of the modems gateway")
    public String gatewayModemName = "benthos-mgateway";

    // @NeptusProperty(name="Draw Ranges in Map")
    public final boolean drawRanges = true;

    private boolean showInRender = true;

    @NeptusProperty(name = "Use My Location", description = "Use My Location for the modem location. Don't forget to set it first.")
    public boolean useMyLocation = true;

    @NeptusProperty(name = "Local Modem Location in Map")
    public LocationType modemLocation = new LocationType();

    @NeptusProperty(name = "Time to clear (ms)", description = "Number of milliseconds to wait until range is erased from the map")
    public long timeToClear = 3000;

    private long lastRangeMillis = 0;
    private double lastRangeDistance = 0;

    Vector<String> ranges = new Vector<String>();

    private AbstractAction showAction;

    private String popupHTML = "<html>(no ranges)</html>";
    private JFormattedTextField targetText = new JFormattedTextField(new DecimalFormat("###"));
    private JLabel bottomLabel = new JLabel(popupHTML);

    private boolean initCalled = false;;

    public ModemRanger(ConsoleLayout console) {
        super(console);

        showAction = new AbstractAction("Hide Modem Data",
                ImageUtils.getIcon("pt/up/fe/dceg/neptus/plugins/acoustic/lbl-show.png")) {
            public void actionPerformed(ActionEvent e) {
                String actionCmd = showAction.getValue(AbstractAction.SHORT_DESCRIPTION).toString();
                if (actionCmd.equals("Hide Modem Data")) {
                    showInRender = false;
                    showAction.putValue(AbstractAction.SHORT_DESCRIPTION, "Show Modem Data");
                    showAction.putValue(AbstractAction.SMALL_ICON,
                            ImageUtils.getIcon("pt/up/fe/dceg/neptus/plugins/acoustic/lbl-hide.png"));
                }
                else if (actionCmd.equals("Show Modem Data")) {
                    showInRender = true;
                    showAction.putValue(AbstractAction.SHORT_DESCRIPTION, "Hide Modem Data");
                    showAction.putValue(AbstractAction.SMALL_ICON,
                            ImageUtils.getIcon("pt/up/fe/dceg/neptus/plugins/acoustic/lbl-show.png"));
                }
            }
        };
        showAction.putValue(AbstractAction.SHORT_DESCRIPTION, "Hide Modem Data");

        removeAll();
        setToolTipText(popupHTML);
        setLayout(new BorderLayout());
        add(targetText, BorderLayout.CENTER);
        JButton range = new JButton("range");
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
        panel.add(range);
        panel.add(new ToolbarButton(showAction));
        add(panel, BorderLayout.EAST);
        range.addActionListener(this);
        add(bottomLabel, BorderLayout.SOUTH);
        targetText.setText("" + targetId);
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.up.fe.dceg.neptus.plugins.SimpleSubPanel#init()
     */
    @Override
    public void initSubPanel() {

        if (initCalled)
            return;
        initCalled = true;

        addMenuItem("Tools>" + PluginUtils.getPluginName(this.getClass()) + ">Settings", null, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                PropertiesEditor.editProperties(ModemRanger.this, getConsole(), true);
            }
        });
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        IMCMessage msg = IMCDefinition.getInstance().create("AcousticRange", "address", targetText.getValue());
        send(gatewayModemName, msg);
        targetId = Integer.valueOf(targetText.getText());
    }

    @Override
    public boolean sendAbortRequest() {
        IMCMessage msg = IMCDefinition.getInstance().create("Abort");
        send(gatewayModemName, msg);
        return true;
    }

    @Override
    public boolean sendAbortRequest(String system) throws Exception {
        throw new Exception("Not implemented");
    }

    @Override
    public String[] getObservedMessages() {
        return new String[] { "AcousticRangeReply" };
    }

    @Override
    public void messageArrived(IMCMessage message) {

        if (message.getAbbrev().equals("AcousticRangeReply")) {
            if (ranges.size() > 10)
                ranges.remove(0);

            lastRangeDistance = message.getDouble("range");

            Object addr = message.getValue("address");
            Object status = message.getValue("status");
            String range = GuiUtils.getNeptusDecimalFormat(1).format(lastRangeDistance);
            lastRangeMillis = System.currentTimeMillis();
            String r = addr + ": " + range + " (" + status + ")";
            ranges.add(r);
            popupHTML = "";
            for (int i = 0; i < ranges.size(); i++) {
                if (i == ranges.size() - 1)
                    popupHTML = "<html><font color='#000099'>" + ranges.get(i) + "</font><hr/>" + popupHTML;
                else
                    popupHTML = ranges.get(i) + "<br/>" + popupHTML;

                popupHTML = popupHTML + "</html>";
            }

            setToolTipText(popupHTML);
            bottomLabel.setText("<html>" + r + "</html>");
        }
    }

    @Override
    public void propertiesChanged() {

        Vector<ILayerPainter> renders = getConsole().getSubPanelsOfInterface(ILayerPainter.class);

        for (ILayerPainter str2d : renders) {
            // if(drawRanges)
            // str2d.addPostRenderPainter(this, "Acoustic Modem Ranges");
            // else
            // str2d.removePostRenderPainter(this);
            str2d.addPostRenderPainter(this, "Acoustic Modem Ranges");
        }
    }

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        if (!showInRender)
            return;
        if (System.currentTimeMillis() - lastRangeMillis < 3000) {
            double clear = (3000 - (System.currentTimeMillis() - lastRangeMillis)) / 3000.0;
            if (useMyLocation)
                modemLocation = MyState.getLocation();
            Point2D topLeft = renderer.getScreenPosition(modemLocation);
            double radius = lastRangeDistance * renderer.getZoom();
            g.setColor(new Color(0, 0, 0, (float) clear));
            g.setStroke(new BasicStroke(4));
            g.draw(new Ellipse2D.Double(topLeft.getX() - radius, topLeft.getY() - radius, radius * 2, radius * 2));
            g.setColor(new Color(1f, 0.5f, 0, (float) clear));
            g.setStroke(new BasicStroke(2));
            g.draw(new Ellipse2D.Double(topLeft.getX() - radius, topLeft.getY() - radius, radius * 2, radius * 2));
        }
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.plugins.SimpleSubPanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
        // TODO Auto-generated method stub
        
    }
}