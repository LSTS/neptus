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
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
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
 * Author: José Correia
 * Apr 26, 2012
 */
package pt.lsts.neptus.plugins.messages;

import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.lsf.IMCMessagePanel;

/**
 * @author jqcorreia
 *
 */
@SuppressWarnings("serial")
public class InspectorMessagePanel extends JPanel {

    IMCMessagePanel mpanel = new IMCMessagePanel();
    
    JLabel lblTitle = new JLabel();
    JLabel lblTimestamp = new JLabel();
    
    long lastTimestamp = 0;
    float lastFreq = 0;
    
    public InspectorMessagePanel() {
        super();
        setLayout(new MigLayout());
        setSize(300, 300);
        add(lblTitle, "split");
        add(lblTimestamp, "wrap");
        add(mpanel,"wrap");
    }
    
    public void setTitle(String title) {
        lblTitle.setText(title);
    }
    public void setDeltaTime(long time)
    {
        lblTimestamp.setText("\u2206t " + time + "ms (" + lastFreq +"Hz)");
    }
    public void setMessage(IMCMessage message) {
        mpanel.setMessage(message);
        lastFreq = 1f/((System.currentTimeMillis() - lastTimestamp)/1000f);
        lastTimestamp = System.currentTimeMillis();
    }
    public void update()
    {
        setDeltaTime((System.currentTimeMillis() - lastTimestamp));
    }
}
