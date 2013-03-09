/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by zp
 * Dec 12, 2011
 */
package pt.up.fe.dceg.neptus.mra.replay;

import java.awt.Graphics2D;
import java.util.Vector;

import javax.swing.JLabel;

import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.mra.importers.IMraLog;
import pt.up.fe.dceg.neptus.mra.importers.IMraLogGroup;
import pt.up.fe.dceg.neptus.renderer2d.LayerPriority;
import pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D;


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
    public boolean canBeApplied(IMraLogGroup source) {
        
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
