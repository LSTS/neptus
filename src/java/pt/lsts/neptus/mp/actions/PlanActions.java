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
 * Author: Paulo Dias
 * 2010/06/27
 */
package pt.lsts.neptus.mp.actions;

import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import pt.lsts.imc.IMCMessage;

/**
 * @author pdias
 *
 */
public class PlanActions {
	protected LinkedList<PlanActionElementConfig> actionMsgs = new LinkedList<PlanActionElementConfig>();

	/**
	 * 
	 */
	public PlanActions() {
	}
	
	/**
	 * @return the actionMsgs
	 */
	public LinkedList<PlanActionElementConfig> getActionMsgs() {	  
		return actionMsgs;
	}

	/**
	 * @param nd
	 */
	public boolean load(Element nd) {
	    actionMsgs.clear();
	    
        List<?> lst = nd.selectNodes("./messages/child::*");
        for (Object obj : lst) {
            Element pl = (Element) obj;
            PlanActionElementConfig plcfg = new PlanActionElementConfig();
            plcfg.setXmlImcNode(pl);
            if (plcfg.message != null)
                actionMsgs.add(plcfg);
        }

        return true;
    }

    public Element asElement(String rootElementName) {
        return (Element) asDocument(rootElementName).getRootElement().detach();
    }
	
    public Document asDocument(String rootElementName) {
        Document document = DocumentHelper.createDocument();
		Element root = document.addElement(rootElementName);
        
		if (actionMsgs.size() > 0) {
			Element plActionsElement = root.addElement("messages");
			for (PlanActionElementConfig plcfg : actionMsgs) {
				Element ndcf = plcfg.getXmlNode();
//				NeptusLog.pub().info("<###>ActionsConfig _________________\n"+ndcf.asXML());
				if (ndcf != null) {
					plActionsElement.add(((Node) ndcf.clone()).detach());
				}
			}
		}
//		NeptusLog.pub().info("<###> "+document.asXML());
        
        return document;
    }

	/**
	 * @return
	 */
	public IMCMessage[] getAllMessages() {
		LinkedList<IMCMessage> msgs = new LinkedList<IMCMessage>();

		for (PlanActionElementConfig msgConfig : actionMsgs) {
			msgs.add(msgConfig.message);
		}
		
		return msgs.toArray(new IMCMessage[msgs.size()]);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		String actStr =  "Plan Actions: ";

		for (PlanActionElementConfig nm : actionMsgs) {
			actStr += " M[" + nm.message.getAbbrev() +
					"]";
		}
		return actStr;
	}

	/**
	 * @return
	 */
	public boolean isEmpty() {
		long count = getActionMsgs().size(); 
		return (count == 0);
	}
    
	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	@Override
	public Object clone() {
	    PlanActions clone = new PlanActions();
        for (PlanActionElementConfig am : actionMsgs)
            clone.actionMsgs.add((PlanActionElementConfig) am.clone());
	    return clone;
	}

    /**
     * @param actionsMessages
     */
    public void parseMessages(Vector<IMCMessage> actionsMessages) {
        // For now all are actionMsgs
        actionMsgs.clear();
        for (IMCMessage msg : actionsMessages) {
            PlanActionElementConfig paec = new PlanActionElementConfig();
            paec.setMessage(msg);
            actionMsgs.add(paec);
        }
    }
    
    /**
     * Clears all messages
     */
    public void clearMessages() {
        actionMsgs.clear();
    }
}
