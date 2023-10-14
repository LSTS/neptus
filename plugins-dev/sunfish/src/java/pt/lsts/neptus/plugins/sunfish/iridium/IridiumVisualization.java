/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
 * Apr 30, 2014
 */
package pt.lsts.neptus.plugins.sunfish.iridium;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.JScrollPane;

import org.apache.commons.codec.binary.Hex;
import org.jdesktop.swingx.JXTable;

import pt.lsts.imc.IridiumMsgRx;
import pt.lsts.imc.IridiumMsgTx;
import pt.lsts.imc.lsf.LsfIterator;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.iridium.IridiumMessage;
import pt.lsts.neptus.mra.MRAPanel;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.mra.visualizations.SimpleMRAVisualization;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.util.GuiUtils;


/**
 * @author zp
 *
 */
@PluginDescription(active=false)
public class IridiumVisualization extends SimpleMRAVisualization {

    IridiumMessagesTableModel tableModel = new IridiumMessagesTableModel();
    
    public IridiumVisualization(MRAPanel panel) {
        super(panel);
    }
    
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Override
    public Type getType() {
        return Type.VISUALIZATION;
    }
    

    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        return source.getLsfIndex().containsMessagesOfType("IridiumMsgTx", "IridiumMsgRx");        
    }

    @Override
    public JComponent getVisualization(IMraLogGroup source, double timestep) {
        
        LsfIterator<IridiumMsgTx> itTx = source.getLsfIndex().getIterator(IridiumMsgTx.class);
        while(itTx.hasNext()) {
            try {
                tableModel.addTransmission(itTx.next());
            }
            catch (Exception e) {
                NeptusLog.pub().error(e);
            }
        }
        
        LsfIterator<IridiumMsgRx> itRx = source.getLsfIndex().getIterator(IridiumMsgRx.class);
        while(itRx.hasNext()) {
            try {
                tableModel.addReception(itRx.next());
            }
            catch (Exception e) {
                NeptusLog.pub().error(e);
            }
        }
        final JXTable table = new JXTable(tableModel);
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    try {
                        IridiumMessage msg = IridiumMessage.deserialize(Hex.decodeHex(tableModel.getMessageData(table.getSelectedRow()).toCharArray()));
                        mraPanel.loadVisualization(new IridiumHtmlVisualization(msg), true);
                    }
                    catch (Exception ex) {
                        GuiUtils.errorMessage(mraPanel, ex);
                    }
                }
            }
        });
        
        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2) {
                    String data = tableModel.getMessageData(table.getSelectedRow());
                    try {
                        IridiumMessage.deserialize(Hex.decodeHex(data.toCharArray()));
                        
                    }
                    catch (Exception e) {
                        NeptusLog.pub().error(e);
                    }
                }
            }
        });
        return new JScrollPane(table);
    }
    
}
