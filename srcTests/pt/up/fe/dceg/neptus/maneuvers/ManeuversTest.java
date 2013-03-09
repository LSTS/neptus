/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by Hugo Dias
 * Oct 20, 2011
 */
package pt.up.fe.dceg.neptus.maneuvers;

import java.util.LinkedHashMap;

import org.junit.Assert;
import org.junit.Test;

import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.imc.IMCUtil;
import pt.up.fe.dceg.neptus.mp.Maneuver;
import pt.up.fe.dceg.neptus.mp.ManeuverFactory;
import pt.up.fe.dceg.neptus.mp.maneuvers.IMCSerialization;
import pt.up.fe.dceg.neptus.types.vehicle.VehiclesHolder;
import pt.up.fe.dceg.neptus.util.conf.ConfigFetch;

/**
 * @author ZP
 *
 */
public class ManeuversTest {

    @Test
    public void testImcSerialization() {
        ConfigFetch.initialize();
        ManeuverFactory fac = VehiclesHolder.getVehicleById("lauv-xtreme-2").getManeuverFactory();

        for (String man : fac.getAvailableManeuversIDs()) {
            Maneuver maneuver = fac.getManeuver(man);
            if (maneuver instanceof IMCSerialization) {
                System.out.println("now testing "+maneuver.getType());
                IMCSerialization ser = ((IMCSerialization) maneuver);
                IMCMessage random = ser.serializeToIMC();
                if (random.getTypeOf("custom") != null) {
                    LinkedHashMap<String, String> custom = random.getTupleList("custom");
                    IMCUtil.fillWithRandomData(random);
                    if (!custom.isEmpty())
                        random.setValue("custom", custom);
                }
                else {
                    IMCUtil.fillWithRandomData(random);
                }
                String before = random.asXml(true);
                System.out.println(before);
                ((IMCSerialization) maneuver).parseIMCMessage(random);
                System.out.println(maneuver.asXML());
                String after = ser.serializeToIMC().asXml(true);                
                Assert.assertEquals(before, after);                
            }
        }
    }

}
