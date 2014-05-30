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
 * Author: José Pinto
 * 2010/02/18
 */
package pt.lsts.neptus.plugins.acoustic;

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

import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.gui.ToolbarButton;
import pt.lsts.neptus.mystate.MyState;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.IAbortSenderProvider;
import pt.lsts.neptus.plugins.NeptusMessageListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.renderer2d.ILayerPainter;
import pt.lsts.neptus.renderer2d.LayerPriority;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;

/**
 * @author zepinto
 * 
 */
@SuppressWarnings("serial")
@PluginDescription(author = "zp", name = "Acoustic Modem Ranger", description = "Allows to query acoustic modem for ranges to other devices", icon = "pt/lsts/neptus/plugins/acoustic/lbl.png")
@LayerPriority(priority = 40)
public class ModemRanger extends ConsolePanel implements ConfigurationListener, ActionListener,
        NeptusMessageListener, Renderer2DPainter, IAbortSenderProvider {

    @NeptusProperty(editable = false)
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
                ImageUtils.getIcon("pt/lsts/neptus/plugins/acoustic/lbl-show.png")) {
            public void actionPerformed(ActionEvent e) {
                String actionCmd = showAction.getValue(AbstractAction.SHORT_DESCRIPTION).toString();
                if (actionCmd.equals("Hide Modem Data")) {
                    showInRender = false;
                    showAction.putValue(AbstractAction.SHORT_DESCRIPTION, "Show Modem Data");
                    showAction.putValue(AbstractAction.SMALL_ICON,
                            ImageUtils.getIcon("pt/lsts/neptus/plugins/acoustic/lbl-hide.png"));
                }
                else if (actionCmd.equals("Show Modem Data")) {
                    showInRender = true;
                    showAction.putValue(AbstractAction.SHORT_DESCRIPTION, "Hide Modem Data");
                    showAction.putValue(AbstractAction.SMALL_ICON,
                            ImageUtils.getIcon("pt/lsts/neptus/plugins/acoustic/lbl-show.png"));
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
     * @see pt.lsts.neptus.plugins.SimpleSubPanel#init()
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
     * @see pt.lsts.neptus.plugins.SimpleSubPanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
        // TODO Auto-generated method stub
        
    }
}