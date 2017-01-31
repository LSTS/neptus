/*
 * Copyright (c) 2004-2016 Universidade do Porto - Faculdade de Engenharia
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
 * http://ec.europa.eu/idabc/eupl.html.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: Manuel Ribeiro
 * Dec 15, 2016
 */
package pt.lsts.neptus.plugins.uavs.panels;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

import javax.swing.BorderFactory;
import javax.swing.JLabel;

import com.google.common.eventbus.Subscribe;

import pt.lsts.imc.ApmStatus;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.events.ConsoleEventMainSystemChange;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.NeptusProperty.LEVEL;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.renderer2d.LayerPriority;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;

/**
 * @author Manuel Ribeiro
 *
 */
@SuppressWarnings("serial")
@PluginDescription(name = "UAV status overlay", author = "Manuel R.")
@LayerPriority(priority = 100)
public class UavStatusOverlay extends ConsolePanel implements Renderer2DPainter {

    private static final int FONT_SIZE = 3;
    private String color;
    private String status = new String();
    private String mainSysName;
    private long lastMsgReceived = 0;
    private JLabel lblToDraw = new JLabel();

    @NeptusProperty(name="Timeout period (ms)", description="Timeout to consider a status as old, in milliseconds")
    public long oldStatusTimeout = 5000;

    @NeptusProperty(name = "Overlay Location", userLevel = LEVEL.REGULAR)
    public OverlayLocation overlayLocation = OverlayLocation.TOP;

    public enum OverlayLocation {
        BOTTOM,
        TOP
    };

    public UavStatusOverlay(ConsoleLayout console) {
        super(console);
        mainSysName = console.getMainSystem();
    }

    @Subscribe
    public void on(ApmStatus rcvStatus) {

        switch (rcvStatus.getSeverity()) {
            case ALERT:
                //Not needed for now
                break;
            case CRITICAL: //arming_checks & common "Bad ? Health" messages
                setUavStatus(rcvStatus.getText(), "red");
                break;
            case DEBUG:
                //Not needed for now
                break;
            case EMERGENCY: //crash check
                setUavStatus(rcvStatus.getText(), "orange");
                break;
            case ERROR:
                setUavStatus(rcvStatus.getText(), "red");
                break;
            case INFO:
                setUavStatus(rcvStatus.getText(), "yellow");
                break;
            case NOTICE:
                setUavStatus(rcvStatus.getText(), "yellow");
                break;
            case WARNING:
                setUavStatus(rcvStatus.getText(), "orange");
                break;
        }

        lastMsgReceived = System.currentTimeMillis();
        NeptusLog.pub().info("[" + rcvStatus.getSeverity() + "] " + rcvStatus.getText() + " ("+mainSysName+")");
    }

    private void setUavStatus(String text, String color) {
        this.status = text;
        this.color = color;
    }

    private String getUavStatus() {
        String html = "<html><table>";
        html += "<tr><td align=center><font color="+color+" size="+FONT_SIZE+"><b>";
        html += status;
        html += "</b></font></td></tr>";
        html += "</table></html>";

        return html;
    }

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        long now = System.currentTimeMillis();

        if ((now - lastMsgReceived) > oldStatusTimeout)
            return;

        AffineTransform old = g.getTransform();
        lblToDraw.setText(getUavStatus());
        lblToDraw.setOpaque(true);
        lblToDraw.setBackground(new Color(255,255,255,128));
        lblToDraw.setSize(lblToDraw.getPreferredSize());
        lblToDraw.setBorder(BorderFactory.createLineBorder(Color.black));

        Rectangle2D stringBounds = g.getFontMetrics().getStringBounds(status, g);

        int[] offsets = getOffsets(renderer, stringBounds);

        g.translate(offsets[0], offsets[1]);
        lblToDraw.paint(g);
        g.setTransform(old);
    }


    /** Calculate offsets for label based on @NeptusPropery overlayLocation 
     * index 0 - X offset
     * index 1 - Y offset
     * @return the offsets for the label
     */
    private int[] getOffsets(StateRenderer2D renderer, Rectangle2D stringBounds) {
        int[] offsets = new int[2];
        switch (overlayLocation) {
            case TOP:
                offsets[0] = (int) (renderer.getWidth() - 30 - stringBounds.getWidth()) / 2;
                offsets[1] = 10;
                break;
            case BOTTOM:
                offsets[0] = (int) (renderer.getWidth() - 30 - stringBounds.getWidth()) / 2;
                offsets[1] = (int) (renderer.getHeight() - 50 - stringBounds.getHeight());
                break;
            default:
                offsets[0] = (int) (renderer.getWidth() - 30 - stringBounds.getWidth()) / 2;
                offsets[1] = 10;
                break;
        }

        return offsets;
    }

    @Subscribe
    public void consume(ConsoleEventMainSystemChange ev) {
        mainSysName = ev.getCurrent();
    }

    @Override
    public void cleanSubPanel() {

    }

    @Override
    public void initSubPanel() {

    }
}
