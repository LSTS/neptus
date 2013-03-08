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
 * $Id:: CheckAutoUserLogItem.java 9616 2012-12-30 23:23:22Z pdias        $:
 */
package pt.up.fe.dceg.neptus.types.checklist;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import pt.up.fe.dceg.neptus.NeptusLog;

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
