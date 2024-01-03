/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: Manuel R.
 * May 17, 2018
 */
package pt.lsts.neptus.plugins.dms;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JOptionPane;

import com.google.common.eventbus.Subscribe;

import pt.lsts.imc.DmsDetection;
import pt.lsts.imc.EstimatedState;
import pt.lsts.neptus.colormap.ColormapOverlay;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.console.ConsoleLayer;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.NeptusMenuItem;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.renderer2d.LayerPriority;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * @author Manuel R.
 *
 */
@PluginDescription(name="DMS Layer", icon="images/orangeled.png")
@LayerPriority(priority=-5)
public class DMSLayer extends ConsoleLayer {

    private static final int DMS_CHANNELS = 16;
    private ArrayList<Map<LocationType, Double>> channels = new ArrayList<>();
    private boolean cancelled = false;
    //Channel - DMS Channel to use for ColorMap Overlay
    private int selectedChannel = 1;
    private int previousChannel = 1;
    private ColormapOverlay overlay = new ColormapOverlay("DMS Layer", 1, false, 0);

    @Subscribe
    public void consume(DmsDetection dms) {
        if (msgFromMainVehicle(dms.getSourceName())) {
            cancelled = false;
            EstimatedState es = ImcMsgManager.getManager().getState(getConsole().getMainSystem()).last(EstimatedState.class);
            if (es == null)
                return;
            LocationType loc = new LocationType(Math.toDegrees(es.getLat()), Math.toDegrees(es.getLon()));
            loc.translatePosition(es.getX(), es.getY(), es.getZ());
            loc.convertToAbsoluteLatLonDepth();

            for (int i=0 ; i < DMS_CHANNELS; i++)
                channels.get(i).put(loc, getChannelValue(dms, i+1));

            overlay = new ColormapOverlay("DMS Layer", 1, false, 0);

            for (Entry<LocationType, Double> x : channels.get(selectedChannel-1).entrySet()) {
                overlay.addSample(x.getKey(), x.getValue());
                if (cancelled)
                    return;
            }
        }
    }

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        super.paint(g, renderer);
        if (cancelled)
            return;

        if (overlay != null)
            overlay.paint((Graphics2D) g.create(), renderer);

    }

    @Override
    public void initLayer() {
        for (int i=0; i < DMS_CHANNELS; i++) {
            Map<LocationType, Double> ch = Collections.synchronizedMap(new LinkedHashMap<LocationType, Double>());
            channels.add(ch);
        }
    }

    @NeptusMenuItem("Advanced>DMS>Set Channel")
    public void setChannel() {
        try {
            int ch = Integer.parseInt((String) JOptionPane.showInputDialog(getConsole(), I18n.text("Select channel"),
                    I18n.text("DMS"), JOptionPane.QUESTION_MESSAGE, null, null, selectedChannel));

            if (ch <= 0 || ch > DMS_CHANNELS) {
                JOptionPane.showMessageDialog(getConsole(),
                        I18n.text("Not a valid channel. (Between 1 and "+DMS_CHANNELS+")"),
                        I18n.text("Error"),
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (selectedChannel != previousChannel) 
                cancelled = true;

            previousChannel = selectedChannel;
            selectedChannel = ch;

        }
        catch (NumberFormatException e) {
            selectedChannel = 1;
            JOptionPane.showMessageDialog(getConsole(),
                    I18n.text("Not a valid channel. (Between 1 and "+DMS_CHANNELS+")"),
                    I18n.text("Error"),
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void cleanLayer() {
        channels.clear();
    }

    private double getChannelValue(DmsDetection o, int channel) {
        String append = "";
        if (channel < 10)
            append = "0";
        return o.getFloat("ch"+append+channel);
    }

    private boolean msgFromMainVehicle(String src) {
        return (src.equals(getConsole().getMainSystem()));
    }

    @Override
    public boolean userControlsOpacity() {
        return false;
    }

}
