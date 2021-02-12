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
 * Author: zp
 * Jan 30, 2014
 */
package pt.lsts.neptus.mra.replay;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import com.google.common.eventbus.Subscribe;

import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.comm.transports.ImcUdpTransport;
import pt.lsts.neptus.gui.PropertiesTable;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.util.GuiUtils;

/**
 * @author zp
 * 
 */
@PluginDescription(name = "Network replay", icon="pt/lsts/neptus/mra/replay/network-wireless.png")
public class NetworkReplay extends JPanel implements LogReplayPanel {

    private static final long serialVersionUID = -5150861961670234509L;
    private IMCDefinition d;
    private ImcUdpTransport trans = null;

    @NeptusProperty(name = "Destination IP", description = "IP address of the destination host")
    private String destination = "127.0.0.1";

    @NeptusProperty(name = "Destination Port", description = "Port of the destination host")
    private int port = 6001;

    @NeptusProperty(name = "Local Port")
    private int bindPort = 9199;
    

    @NeptusProperty(name = "Update IMC protocol", description = "If selected, all messages will be"
            + " reencoded to the latest IMC protocol (fields specific to the old protocol will be lost)")
    private boolean updateProtocol = false;

    @NeptusProperty(name = "Update timestamps", description = "If selected, old messages timestamps will be replaced with current time")
    private boolean updateTimestamps = false;
    
    @NeptusProperty(name = "Post locally (to Neptus)", description = "If selected, messages will be received only inside this Neptus instance")
    private boolean postLocally = false;
    
    
    @Override
    public String getName() {
        return "Network replay";
    }

    private PropertiesTable pt = new PropertiesTable();
    private JToggleButton active;

    public NetworkReplay() {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(400, 250));
        pt.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        add(pt, BorderLayout.CENTER);
        active = new JToggleButton("Active");
        active.setSelected(false);
        active.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                setActive(active.isSelected());
            }
        });
        JPanel bottom = new JPanel();
        bottom.add(active);
        add(bottom, BorderLayout.SOUTH);
        pt.editProperties(PluginUtils.wrapIntoAPlugInPropertiesProvider(this));
    }

    public void setActive(boolean act) {
        pt.setEnabled(!act);

        if (act) {
            try {
                if (updateProtocol)
                    trans = new ImcUdpTransport(bindPort, IMCDefinition.getInstance());
                else
                    trans = new ImcUdpTransport(bindPort, d);
            }
            catch (Exception e) {
                trans = null;
                pt.setEnabled(true);
                active.setSelected(false);
                GuiUtils.errorMessage("Could not start network replay",
                        e.getClass().getSimpleName() + " : " + e.getMessage());
                e.printStackTrace();
                return;
            }
        }
        else if (trans != null) {
            trans.stop();
            trans = null;
        }
    }

    @Override
    public boolean canBeApplied(IMraLogGroup source, Context context) {
        return true;
    }

    @Override
    public void parse(IMraLogGroup source) {
        d = source.getLsfIndex().getDefinitions();
    }

    @Override
    public String[] getObservedMessages() {
        return null;
    }

    @Override
    public void onMessage(IMCMessage message) {

    }

    @Subscribe
    public void on(IMCMessage m) {
        if (trans != null) {
            if (updateTimestamps)
                m.setTimestampMillis(System.currentTimeMillis());
            if (postLocally)
                ImcMsgManager.getManager().postInternalMessage(m.getSourceName(), m);
            else
                trans.sendMessage(destination, port, m);
        }
    }

    @Override
    public boolean getVisibleByDefault() {
        return false;
    }

    @Override
    public void cleanup() {
        if (trans != null)
            trans.stop();
    }

    @Override
    public JComponent getComponent() {
        return this;
    }

}
