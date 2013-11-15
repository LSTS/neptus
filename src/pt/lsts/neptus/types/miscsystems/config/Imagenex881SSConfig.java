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
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
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
 * Author: Paulo Dias
 * 2010/06/25
 */
package pt.lsts.neptus.types.miscsystems.config;

import java.awt.Dialog.ModalityType;
import java.awt.Window;
import java.util.HashMap;

import org.dom4j.Element;
import org.dom4j.Node;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.SonarConfig;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

/**
 * @author pdias
 * 
 */
public class Imagenex881SSConfig extends OptionsConfiguration {

	// <message id="135" name="Sidescan Configuration" abbrev="SidescanConfig"
	// source="vehicle" used-by="*" flags="periodic">
	// <description>
	// This message contains high-level runtime configuration parameters for
	// Side-scan sonars.
	// </description>
	// <field name="Frequency" abbrev="frequency" type="uint8_t"
	// unit="Enumerated" prefix="FREQ">
	// <description>
	// Operating frequency.
	// </description>
	// <enum id="0" name="High Frequency" abbrev="HIGH">
	// <description/>
	// </enum>
	// <enum id="1" name="Low Frequency" abbrev="LOW">
	// <description/>
	// </enum>
	// </field>
	// <field name="Range" abbrev="range" type="uint16_t" unit="m">
	// <description>
	// Operating range.
	// </description>
	// </field>
	// </message>

	// protected IMCMessage message =
	// IMCDefinition.getInstance().create("SidescanConfig");

    private String[] freqType = { "HIGH", "LOW" };
    private HashMap<String, String[]> freqValues = new HashMap<String, String[]>() {
        /**
         * 
         */
        private static final long serialVersionUID = 1L;

        {
            put("HIGH", new String[] { "15", "30" });
            put("LOW", new String[] { "15", "30", "60", "90", "120" });
        }
    };
    private String name = "";            
    
	/**
	 * 
	 */
	public Imagenex881SSConfig() {
		message = IMCDefinition.getInstance().create(SonarConfig.class);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * pt.lsts.neptus.types.miscsystems.config.OptionsConfiguration#load
	 * (org.dom4j.Element)
	 */
	@Override
	public void load(Element configurationXMLElement) {
		super.load(configurationXMLElement);
		
		freqValues.clear();
        Node node = configurationXMLElement.selectSingleNode("//id");
        node = configurationXMLElement.selectSingleNode("//name");
        if (node != null)
            name = node.getText();
        node = configurationXMLElement.selectSingleNode("//option");
		String freqTypeStr = node.selectSingleNode("./@values").getText();
		freqType = freqTypeStr.toUpperCase().split("[ ,]");
        int i = 0;

        for (String val1 : node.selectSingleNode("./option/@values").getText().split("[|]")) {
            String[] freqV1 = val1.split("[, ]");
            try {
                freqValues.put(freqType[i++], freqV1);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
	}

	@Override
	public DefaultProperty[] getProperties() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getPropertiesDialogTitle() {
		return "Properties for " + name;
	}

	@Override
	public String[] getPropertiesErrors(Property[] properties) {
		return null;
	}

	@Override
	public void setProperties(Property[] properties) {

	}

	@Override
	public IMCMessage createNewMessage() {
		IMCMessage msg = super.createNewMessage();

		msg.setValue("frequency", 0);
		msg.setValue("range", 30);
		return msg;
	}

	@Override
	public boolean showDialog(IMCMessage message, Window parent) {
		String freq = message.getString("frequency").toUpperCase();
		String range = message.getValue("range").toString();
        SideScanConfigPanel d = new SideScanConfigPanel(parent, name, freqType, freqValues,
                freq, range);
		d.setTitle(name);
		GuiUtils.centerParent(d, parent);
		NeptusLog.pub().info("<###> "+d.setSelectedFrequency(freq));
		NeptusLog.pub().info("<###> "+d.setSelectedRange(range));
		d.setModalityType(ModalityType.DOCUMENT_MODAL);
		d.setVisible(true);
		if (!d.isUserCanceled()) {
		    int freqTE = 0;
		    if ("MEDIUM".equalsIgnoreCase(d.getSelectedFrequency()))
		        freqTE = 1;
		    else if ("LOW".equalsIgnoreCase(d.getSelectedFrequency()))
                freqTE = 2;
			message.setValue("frequency", freqTE);
			message.setValue("range", Integer.parseInt(d.getSelectedRange()));
			return false;
		}
		else
			return true;
	}
}
