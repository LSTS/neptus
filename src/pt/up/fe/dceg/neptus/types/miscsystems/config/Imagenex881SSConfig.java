/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by pdias
 * 2010/06/25
 * $Id:: Imagenex881SSConfig.java 9615 2012-12-30 23:08:28Z pdias               $:
 */
package pt.up.fe.dceg.neptus.types.miscsystems.config;

import java.awt.Dialog.ModalityType;
import java.awt.Window;
import java.util.HashMap;

import org.dom4j.Element;
import org.dom4j.Node;

import pt.up.fe.dceg.neptus.imc.IMCDefinition;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.imc.SonarConfig;
import pt.up.fe.dceg.neptus.util.GuiUtils;

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
	 * pt.up.fe.dceg.neptus.types.miscsystems.config.OptionsConfiguration#load
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
		System.out.println(d.setSelectedFrequency(freq));
		System.out.println(d.setSelectedRange(range));
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
