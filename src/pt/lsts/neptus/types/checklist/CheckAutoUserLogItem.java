/*
 * Copyright (c) 2004-2015 Universidade do Porto - Faculdade de Engenharia
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
 * Author: 
 * 20??/??/??
 */
package pt.lsts.neptus.types.checklist;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import pt.lsts.neptus.NeptusLog;

public class CheckAutoUserLogItem extends CheckAutoSubItem{

	private String logRequest = null;
	private String logMessage = null;

	public CheckAutoUserLogItem(String xml){
		super();
		setSubItemType("userLog");
        load (xml);
    }
	
	public CheckAutoUserLogItem(){
        super();
        subItemType = "userLog";
    }

	public String getLogRequest() {
		return logRequest;
	}

	public void setLogRequest(String logRequest) {
		this.logRequest = logRequest;
	}
	
	public String getLogMessage() {
		return logMessage;
	}

	public void setLogMessage(String logMessage) {
		this.logMessage = logMessage;
	}

	@Override
	public Document asDocument(String rootElementName) {
		Document document = DocumentHelper.createDocument();
		Element root = document.addElement( rootElementName );

		root.addAttribute("checked", Boolean.toString(isChecked()));
		
		root.addElement("logRequest").setText(getLogRequest());
		root.addElement("logMessage").setText(getLogMessage());
	
		return document;
	}

	@Override
	public boolean load(String xml) {

		String fileAsString = xml;
		try {
			doc = DocumentHelper.parseText(fileAsString);
			this.setChecked(Boolean.parseBoolean(doc.selectSingleNode(
					"/userLog/@checked").getText()));
			Node nd = doc.selectSingleNode("/userLog/logRequest");
			if (nd != null)
				this.setLogRequest(nd.getText());

			nd = doc.selectSingleNode("/userLog/logMessage");
			if (nd != null)
				this.setLogMessage(nd.getText());

		} catch (DocumentException e) {
			NeptusLog.pub().error(this, e);
			return false;
		}
		return true;
	}

}
