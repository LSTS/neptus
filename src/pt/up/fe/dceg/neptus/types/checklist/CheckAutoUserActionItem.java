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
 * $Id:: CheckAutoUserActionItem.java 9616 2012-12-30 23:23:22Z pdias     $:
 */
package pt.up.fe.dceg.neptus.types.checklist;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import pt.up.fe.dceg.neptus.NeptusLog;

public class CheckAutoUserActionItem extends CheckAutoSubItem{
	
	private String action = null;

	public CheckAutoUserActionItem(String xml){
		super();
		setSubItemType("userAction");
        load (xml);
    }
	
	public CheckAutoUserActionItem(){
        super();
        subItemType = "userAction";
    }

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	@Override
	public Document asDocument(String rootElementName) {
		Document document = DocumentHelper.createDocument();
		Element root = document.addElement( rootElementName );

		root.addAttribute("checked", Boolean.toString(isChecked()));
		root.setText(getAction());
        
		return document;
	}

	@Override
	public boolean load(String xml) {
		String fileAsString = xml;
		try {
			
			doc = DocumentHelper.parseText(fileAsString);
			this.setChecked(Boolean.parseBoolean(doc.selectSingleNode(
					"/userAction/@checked").getText()));
			
			Node nd = doc.selectSingleNode("/userAction");
			if (nd != null)
				this.setAction(nd.getText());

		} catch (DocumentException e) {
			NeptusLog.pub().error(this, e);
			return false;
		}
		return true;
	}
	
}
