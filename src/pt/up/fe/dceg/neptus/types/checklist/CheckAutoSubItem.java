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
 */
package pt.up.fe.dceg.neptus.types.checklist;

import org.dom4j.Document;
import org.dom4j.Element;

import pt.up.fe.dceg.neptus.types.XmlInputMethods;
import pt.up.fe.dceg.neptus.types.XmlOutputMethods;

public abstract class CheckAutoSubItem implements XmlOutputMethods,
		XmlInputMethods {

	public String subItemType = "NOT_DEFINED";

	protected Document doc; // usado no load

	protected boolean checked = false;

	public boolean isChecked() {
		return checked;
	}

	public void setChecked(boolean isChecked) {
		this.checked = isChecked;
	}
	
	public String getSubItemType() {
		return subItemType;
	}

	public void setSubItemType(String subItemType) {
		this.subItemType = subItemType;
	}
	
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see pt.up.fe.dceg.neptus.types.XmlOutputMethods#asXML()
	 */
	public String asXML() {
		String rootElementName = subItemType;
		return asXML(rootElementName);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pt.up.fe.dceg.neptus.types.XmlOutputMethods#asXML(java.lang.String)
	 */
	public String asXML(String rootElementName) {
		String result = "";
		Document document = asDocument(rootElementName);
		result = document.asXML();
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pt.up.fe.dceg.neptus.types.XmlOutputMethods#asElement()
	 */
	public Element asElement() {
		String rootElementName = subItemType;
		return asElement(rootElementName);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * pt.up.fe.dceg.neptus.types.XmlOutputMethods#asElement(java.lang.String)
	 */
	public Element asElement(String rootElementName) {
		return (Element) asDocument(rootElementName).getRootElement().detach();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pt.up.fe.dceg.neptus.types.XmlOutputMethods#asDocument()
	 */
	public Document asDocument() {
		String rootElementName = subItemType;
		return asDocument(rootElementName);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * pt.up.fe.dceg.neptus.types.XmlOutputMethods#asDocument(java.lang.String)
	 */

	@Override
	public boolean isLoadOk() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean load(Element elem) {
		// TODO Auto-generated method stub
		return false;
	}

}
