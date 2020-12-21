/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
 * Author: 
 * 20??/??/??
 */
package pt.lsts.neptus.types.checklist;

import org.dom4j.Document;
import org.dom4j.Element;

import pt.lsts.neptus.types.XmlInputMethods;
import pt.lsts.neptus.types.XmlOutputMethods;

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
	 * @see pt.lsts.neptus.types.XmlOutputMethods#asXML()
	 */
	public String asXML() {
		String rootElementName = subItemType;
		return asXML(rootElementName);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pt.lsts.neptus.types.XmlOutputMethods#asXML(java.lang.String)
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
	 * @see pt.lsts.neptus.types.XmlOutputMethods#asElement()
	 */
	public Element asElement() {
		String rootElementName = subItemType;
		return asElement(rootElementName);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * pt.lsts.neptus.types.XmlOutputMethods#asElement(java.lang.String)
	 */
	public Element asElement(String rootElementName) {
		return (Element) asDocument(rootElementName).getRootElement().detach();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pt.lsts.neptus.types.XmlOutputMethods#asDocument()
	 */
	public Document asDocument() {
		String rootElementName = subItemType;
		return asDocument(rootElementName);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * pt.lsts.neptus.types.XmlOutputMethods#asDocument(java.lang.String)
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
