/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by 
 * 20??/??/??
 * $Id:: CommunicationsControlsProvider.java 9616 2012-12-30 23:23:22Z pd#$:
 */
package pt.up.fe.dceg.neptus.console.plugins;

import java.util.Vector;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.console.ConsoleSystem;
import pt.up.fe.dceg.neptus.gui.PropertiesEditor;
import pt.up.fe.dceg.neptus.gui.PropertiesProvider;
import pt.up.fe.dceg.neptus.types.comm.CommMean;
import pt.up.fe.dceg.neptus.util.comm.CommUtil;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

public class CommunicationsControlsProvider implements PropertiesProvider {

    ConsoleLayout console;

    public CommunicationsControlsProvider(ConsoleLayout cons) {
        console = cons;
    }

    public DefaultProperty[] getProperties() {

        Vector<DefaultProperty> props = new Vector<DefaultProperty>();

        for (ConsoleSystem j : console.getConsoleSystems().values()) {
            if (CommUtil.isProtocolSupported(j.getVehicleId(), CommMean.IMC))
                props.add(PropertiesEditor.getPropertyInstance(j.getVehicleId(), "IMC Communications", Boolean.class,
                        j.isNeptusCommunications(), true));
        }

        return props.toArray(new DefaultProperty[] {});
    }

    public void setProperties(Property[] properties) {

        for (Property p : properties) {
            for (ConsoleSystem j : console.getConsoleSystems().values()) {
                if (j.getVehicleId().equals(p.getName())) {

                    if (p.getCategory().equals("IMC Communications")) {
                        boolean imc =  (Boolean) p.getValue();
                        if(imc)
                            j.enableIMC();
                        else
                            j.disableIMC();
                    }
                }
            }
        }
    }

    public String getPropertiesDialogTitle() {
        return "Communications Panel";
    }

    public String[] getPropertiesErrors(Property[] properties) {
        return null;
    }
}
