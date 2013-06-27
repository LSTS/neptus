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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Jun 27, 2013
 */
package pt.up.fe.dceg.neptus.plugins.alignment;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.imc.EntityParameter;
import pt.up.fe.dceg.neptus.imc.EntityState;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.imc.SetEntityParameters;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty;
import pt.up.fe.dceg.neptus.plugins.Popup;
import pt.up.fe.dceg.neptus.plugins.SimpleSubPanel;
import pt.up.fe.dceg.neptus.util.ImageUtils;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.EntitiesResolver;

import com.google.common.eventbus.Subscribe;

/**
 * This panel will allow monitoring and alignment of some IMUs
 * @author zp
 */
@Popup(accelerator=KeyEvent.VK_I)
public class ImuAlignmentPanel extends SimpleSubPanel {

    private static final long serialVersionUID = -1330079540844029305L;
    protected JToggleButton enableImu;
    protected JButton doAlignment;
    protected JEditorPane status;

    @NeptusProperty(name="IMC Entity Label")
    public String imuEntityLabel = "CPU";

    @NeptusProperty(name="Square Side Length")
    public double squareSideLength = 50;

    protected ImageIcon greenLed = ImageUtils.getIcon("pt/up/fe/dceg/neptus/plugins/alignment/led_green.png");
    protected ImageIcon redLed = ImageUtils.getIcon("pt/up/fe/dceg/neptus/plugins/alignment/led_red.png");
    protected ImageIcon grayLed = ImageUtils.getIcon("pt/up/fe/dceg/neptus/plugins/alignment/led_none.png");


    public ImuAlignmentPanel(ConsoleLayout console) {
        super(console);        
        setLayout(new BorderLayout(3, 3));
        removeAll();
        JPanel top = new JPanel(new GridLayout(1, 0));
        enableImu = new JToggleButton(I18n.text("Enable IMU"), grayLed, false);
        enableImu.setEnabled(false);
        top.add(enableImu);

        enableImu.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                toggleImu(enableImu.isSelected());                
            }
        });

        doAlignment = new JButton(I18n.text("Do Alignment"));
        top.add(doAlignment);
        status = new JEditorPane("text/html", "<html><b>waiting for vehicle connection</b></html>");
        doAlignment.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                doAlignment();
            }
        });
        add(top, BorderLayout.NORTH);
        add(status, BorderLayout.CENTER);
    }

    public void doAlignment() {
        System.out.println("Do Alignment");
    }

    public void toggleImu(boolean newState) {
        System.out.println("Toggle IMU");
        Vector<EntityParameter> params = new Vector<>();
        params.add(new EntityParameter("Active", ""+newState));
        SetEntityParameters m = new SetEntityParameters(imuEntityLabel, params);
        send(m);
        m.dump(System.out);
    }

    @Override
    public void initSubPanel() {
        // TODO Auto-generated method stub

    }

    @Subscribe
    public void on(EntityState entityState) {

        if (getConsole().getMainSystem() == null)
            return;

        if (!entityState.getSourceName().equals(getConsole().getMainSystem()))
            return;

        String entityName = EntitiesResolver.resolveName(getConsole().getMainSystem(), (int)entityState.getSrcEnt());

        if (entityName == null)
            return;
        
        if (entityName.equals(imuEntityLabel)) {
            switch (entityState.getState()) {
                case NORMAL:
                    
                    enableImu.setIcon(greenLed);
                    enableImu.setSelected(true);
                    enableImu.setEnabled(true);
                    break;
                default:
                    enableImu.setIcon(redLed);
                    enableImu.setEnabled(true);
                    enableImu.setSelected(false);
                    break;
            }
        }
    }

    @Override
    public void cleanSubPanel() {
        // TODO Auto-generated method stub

    }

}
