/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by jqcorreia
 * Apr 26, 2012
 */
package pt.up.fe.dceg.neptus.plugins.messages;

import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.imc.lsf.IMCMessagePanel;

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
