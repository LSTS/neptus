/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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
 * Mar 11, 2014
 */
package pt.lsts.neptus.plugins.sunfish;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.IMCUtil;
import pt.lsts.neptus.comm.iridium.IridiumMessage;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.gui.tablelayout.TableLayout;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.util.ByteUtil;
import pt.lsts.neptus.util.GuiUtils;

/**
 * @author zp
 *
 */

public class IridiumMessageDebugger extends ConsolePanel {

    private static final long serialVersionUID = -1397409355043125958L;

    JTextField raw = new JTextField();
    JButton parse = new JButton(I18n.text("Parse"));
    JTabbedPane msgs = new JTabbedPane();

    public IridiumMessageDebugger(ConsoleLayout console) {
        super(console);
        setLayout(new TableLayout(new double[] {0.80, 0.20}, new double[] {24, TableLayout.FILL}));
        add(raw, "0,0");
        add(parse, "1,0");
        add(msgs, "0,1 1,1");

        parse.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                parse(raw.getText());                
            }
        });
    }

    public void parse(String text) {
        msgs.removeAll();
        try {
            byte[] data = ByteUtil.decodeHexString(text);
            IridiumMessage msg = IridiumMessage.deserialize(data);
            System.out.println(msg.getMessageType());
            for (IMCMessage m : msg.asImc()) {
                String html = IMCUtil.getAsHtml(m);

                JLabel lbl = new JLabel(html);
                JScrollPane scroll = new JScrollPane(lbl);
                msgs.addTab(m.getAbbrev(), scroll);                
            }
            msgs.invalidate();
            msgs.revalidate();
        }
        catch (Exception e) {
            GuiUtils.errorMessage(getConsole(), e);            
        }
    }

    @Override
    public void cleanSubPanel() {

    }

    @Override
    public void initSubPanel() {

    }

    public static void main(String[] args) {
        GuiUtils.testFrame(new IridiumMessageDebugger(null));        
    }
}
