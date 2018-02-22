/*
 * Copyright (c) 2004-2018 Universidade do Porto - Faculdade de Engenharia
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
 * Author: keila
 * Feb 5, 2018
 */
package pt.lsts.neptus.plugins.sunfish.iridium.feedback;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;

import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.util.GuiUtils;

@PluginDescription(name = "Iridium Communications Status",description="Iridium Communications Feedback Panel")
@Popup(pos = Popup.POSITION.BOTTOM_RIGHT, width=355, height=215)
public class IridiumStatus extends ConsolePanel {
    
    private static final long serialVersionUID = 1L;
    private IridiumStatusTableModel iridiumCommsStatus;
    private JTable table; 
    private JScrollPane scroll;
    
    /**
     * @param console
     */
    public IridiumStatus(ConsoleLayout console) {
        super(console);
     
    }
    
    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsolePanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
        iridiumCommsStatus.clear();
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsolePanel#initSubPanel()
     */
    @Override
    public void initSubPanel() {
        removeAll();
        setLayout(new BorderLayout());
        
        iridiumCommsStatus =  new IridiumStatusTableModel();

        //JButton filter =  new JButton("Filter");
        table = new JTable(iridiumCommsStatus){
            private static final long serialVersionUID = 1L;

            @Override
            public String getToolTipText(MouseEvent event) {
                java.awt.Point p = event.getPoint();
                int column = columnAtPoint(p);
                if(column == IridiumStatusTableModel.STATUS){
                    int row = rowAtPoint(p);
                    return iridiumCommsStatus.getToolTipText(row);
                }
                return super.getToolTipText();
            }
        };
        scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(350, 200));
        
        add(scroll,BorderLayout.CENTER);
        //add(filter,BorderLayout.SOUTH);
        
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    displayMessage();
                }
            }
        });
      }
    
    public void displayMessage(){
        try {
            String msg = iridiumCommsStatus.getMessageData(table.getSelectedRow());
            JTextArea data = new JTextArea();
            data.setEditable(false);
            data.setOpaque(true);
            data.setMaximumSize(new Dimension(500,350));
            data.setText(msg);
            JScrollPane jscroll = new JScrollPane(data); 
            jscroll.setPreferredSize(new Dimension(400,400));
            String title = "Iridium Message Data";
            JOptionPane.showMessageDialog(this, jscroll, title, JOptionPane.PLAIN_MESSAGE);          
        }
        catch (Exception ex) {
            GuiUtils.errorMessage(getConsole(), ex);
        }
    }
}
