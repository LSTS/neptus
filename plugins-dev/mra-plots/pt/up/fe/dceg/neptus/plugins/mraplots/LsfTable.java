/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
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
 * Author: zp
 * Jun 5, 2012
 */
package pt.up.fe.dceg.neptus.plugins.mraplots;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.FileInputStream;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;

import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.HighlighterFactory;

import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.imc.IMCDefinition;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.imc.IMCUtil;
import pt.up.fe.dceg.neptus.imc.lsf.LsfIndex;
import pt.up.fe.dceg.neptus.mra.MRAPanel;
import pt.up.fe.dceg.neptus.mra.importers.IMraLogGroup;
import pt.up.fe.dceg.neptus.mra.visualizations.SimpleMRAVisualization;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.util.ImageUtils;

/**
 * @author zp
 * 
 */
@PluginDescription(name = "Raw Messages", icon="pt/up/fe/dceg/neptus/plugins/mraplots/msg_inspector.png")
public class LsfTable extends SimpleMRAVisualization {
    /**
     * @param panel
     */
    public LsfTable(MRAPanel panel) {
        super(panel);
    }

    private static final long serialVersionUID = -8135678083471983681L;
    protected LsfIndex index;
    protected JXTable table;

    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        return source.getFile("Data.lsf") != null && source.getFile("IMC.xml") != null;
    }

    @Override
    public JComponent getVisualization(IMraLogGroup source, double timestep) {
        try {
            if (index == null)
                index = new LsfIndex(source.getFile("Data.lsf"), new IMCDefinition(new FileInputStream(
                        source.getFile("IMC.xml"))));
            
            if (table == null) {
                table = new JXTable(new LsfIndexTableModel(index));
                table.setHighlighters(HighlighterFactory.createAlternateStriping());
                table.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        if (e.getClickCount() == 2) {
                            if (table.getSelectedRow() != -1 && e.getClickCount() == 2) {
                                final IMCMessage msg = index.getMessage(table.getSelectedRow());
                                JLabel lbl = new JLabel(IMCUtil.getAsHtml(msg));
                                lbl.setBackground(Color.white);
                                lbl.setOpaque(true);
                                JScrollPane scroll = new JScrollPane(lbl);
    
                                Component parent = table;
                                while (parent != null && !(parent instanceof MRAPanel))
                                    parent = parent.getParent();
                                if (parent instanceof MRAPanel) {
//                                    ((MRAPanel) parent).getTabPane().addTab(
//                                            msg.getAbbrev() + "[" + table.getValueAt(table.getSelectedRow(), 0) + "]",
//                                            ImageUtils.getIcon("images/menus/view.png"), scroll);
//                                    ((MRAPanel) parent).getTabPane().setSelectedIndex(
//                                            ((MRAPanel) parent).getTabPane().getTabCount() - 1);
                                }
                                else {
                                    JFrame tmp = new JFrame(msg.getAbbrev() + "["
                                            + table.getValueAt(table.getSelectedRow(), 0) + "]");
                                    tmp.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                                    tmp.setContentPane(scroll);
                                    tmp.setSize(400, 400);
                                    tmp.setIconImage(ImageUtils.getImage("images/menus/view.png"));
                                    tmp.setVisible(true);
                                }
                            }
                        }
                    }
                });
            }
            return new JScrollPane(table);
        }
        catch (Exception e) {
            return new JLabel(I18n.text("ERROR")+": " + e.getMessage());
        }
    }
    public Type getType() {
        return Type.TABLE;
    }
}
