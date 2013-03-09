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
 * Author: José Pinto
 * Jan 2, 2013
 */
package pt.up.fe.dceg.neptus.util.llf;

import java.awt.Color;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;

import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.imc.IMCUtil;
import pt.up.fe.dceg.neptus.mra.importers.IMraLogGroup;
import pt.up.fe.dceg.neptus.mra.visualizations.MRAVisualization;
import pt.up.fe.dceg.neptus.util.ImageUtils;

/**
 * @author zp
 *
 */
public class MessageHtmlVisualization implements MRAVisualization {

    protected IMCMessage message;
    protected SimpleDateFormat fmt = new SimpleDateFormat("HH:mm:ss.SSS");
    protected JScrollPane scroll;
    
    public MessageHtmlVisualization(final IMCMessage message) {
        this.message = message;
        
        JLabel lbl = new JLabel(IMCUtil.getAsHtml(message));
        lbl.setBackground(Color.white);
        lbl.setOpaque(true);
        lbl.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) {
                    JPopupMenu popup = new JPopupMenu();
                    popup.add(I18n.text("Copy HTML to clipboard")).addActionListener(
                            new ActionListener() {

                                @Override
                                public void actionPerformed(ActionEvent e) {
                                    StringSelection selection = new StringSelection(IMCUtil
                                            .getAsHtml(message));
                                    Toolkit.getDefaultToolkit().getSystemClipboard()
                                            .setContents(selection, null);
                                }
                            });

                    popup.show(e.getComponent(), e.getX(), e.getY());
                }
            };
        });

        scroll = new JScrollPane(lbl);
    }
    
    public void onHide() {
        
    };
    
    public void onShow() {
    }
    
    @Override
    public boolean supportsVariableTimeSteps() {
        return false;
    }
    
    @Override
    public String getName() {
        return message.getAbbrev() + "[" + fmt.format(message.getDate()) + "]";
    }
    
    @Override
    public ImageIcon getIcon() {
        return ImageUtils.getIcon("images/menus/view.png");
    }
    
    @Override
    public Double getDefaultTimeStep() {
        return null;
    }
    
    @Override
    public JComponent getComponent(IMraLogGroup source, double timestep) {
        return scroll;
    }
    
    public Type getType() {
        return Type.TABLE;
    }
    
    public void onCleanup() {
        
    }
    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        return true;
    }

}
