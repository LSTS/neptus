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
 * Dec 14, 2012
 * $Id:: SystemConfiguration.java 10065 2013-03-04 12:40:58Z pdias              $:
 */
package pt.up.fe.dceg.neptus.plugins.params;

import java.awt.BorderLayout;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.console.plugins.MainVehicleChangeListener;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.plugins.NeptusMessageListener;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.Popup;
import pt.up.fe.dceg.neptus.plugins.Popup.POSITION;
import pt.up.fe.dceg.neptus.plugins.SimpleSubPanel;
import pt.up.fe.dceg.neptus.plugins.params.SystemProperty.Scope;
import pt.up.fe.dceg.neptus.plugins.params.SystemProperty.Visibility;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcMsgManager;

/**
 * @author zp
 * @author jqcorreia
 * @author pdias
 */
@SuppressWarnings("serial")
@PluginDescription(name="System Configuration")
@Popup(accelerator='Z', pos=POSITION.CENTER, width=600, height=600)
public class SystemConfiguration extends SimpleSubPanel implements NeptusMessageListener, MainVehicleChangeListener {

    private SystemConfigurationEditorPanel systemConfEditor;
        
    public SystemConfiguration(ConsoleLayout console) {
        super(console);
    }

    @Override
    public void initSubPanel() {
        initialize();
    }

    private void initialize() {
        removeAll();
        
        systemConfEditor = new SystemConfigurationEditorPanel(getMainVehicleId(), Scope.GLOBAL, Visibility.USER, true,
                true, ImcMsgManager.getManager());
        
        setLayout(new BorderLayout());
        add(systemConfEditor);

        revalidate();
        repaint();
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.plugins.SimpleSubPanel#mainVehicleChangeNotification(java.lang.String)
     */
    @Override
    public void mainVehicleChangeNotification(String id) {
        systemConfEditor.setSystemId(id);
    }
        

    @Override
    public void cleanSubPanel() {
    }

    @Override
    public String[] getObservedMessages() {
        return new String[] { "EntityParameters" };
    }

    @Override
    public void messageArrived(IMCMessage message) {
        SystemConfigurationEditorPanel.updatePropertyWithMessageArrived(systemConfEditor, message);
    }
}
