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
 * Author: José Pinto
 * Dec 12, 2011
 */
package pt.lsts.neptus.mra.replay;

import java.awt.Graphics2D;
import java.util.Vector;

import javax.swing.JLabel;

import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.importers.IMraLog;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.renderer2d.LayerPriority;
import pt.lsts.neptus.renderer2d.StateRenderer2D;


/**
 * @author zp
 *
 */
@LayerPriority(priority=100)
public class TrexReplay implements LogReplayLayer {

    JLabel label = new JLabel();
    
    @Override
    public String getName() {
        return I18n.text("TREX replay");
    }
    
    @Override
    public void cleanup() {
    }

    @Override
    public boolean canBeApplied(IMraLogGroup source, Context context) {
        
//        IMraLog log = source.getLog("LogBookEntry");
//        
//        if (log == null)
//            return false;
//        
//        IMCMessage msg = log.nextLogEntry();
//        
//        while (msg != null) {
//            if (msg.getSrc() == 65000)
//                return true;
//            else
//                msg = log.nextLogEntry();
//        }
//        
        return false;
    }

    Vector<String> messages = new Vector<String>();
    Vector<Double> timestamps = new Vector<Double>();
    @Override
    public void parse(IMraLogGroup source) {
        IMraLog log = source.getLog("LogBookEntry");

        IMCMessage m;
        while ((m = log.nextLogEntry()) != null) {            
            if (m.getSrc() == 65000) {
                messages.add(m.getString("text"));
                timestamps.add(m.getTimestamp());
            }       
        }
    }

    @Override
    public String[] getObservedMessages() {
        return new String[] {"LogBookEntry"};
    }

    protected double currentTime = 0;
    
    @Override
    public void onMessage(IMCMessage message) {
        currentTime = message.getTimestamp();
    }
    
    

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        int curIndex;
        
        for (curIndex = 0; curIndex < timestamps.size(); curIndex++)
            if (timestamps.get(curIndex) > currentTime)
                break;
        
        String text = "<html>";
        for (int i = curIndex-1; i > 0 && i > curIndex-6; i--) {
            text +=messages.get(i)+"<br/>";
        }
        label.setText(text+"</html>");
        
        label.setBounds(0,0, renderer.getWidth()-10, 100);
        g.translate(5, renderer.getHeight()-150);
        label.paint(g);
    }
    
    
    @Override
    public boolean getVisibleByDefault() {
        return true;
    }

}
