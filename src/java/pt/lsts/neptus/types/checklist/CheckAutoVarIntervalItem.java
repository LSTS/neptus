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
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import pt.lsts.neptus.NeptusLog;
/**
 * 
 * @author Rui Gonçalves
 *
 */
public class CheckAutoVarIntervalItem extends CheckAutoSubItem {
	private String varName = null;
	private String varPath = null;
	private Double varValue = null;

	private Double startInterval = null;
	private boolean startInclusion = true;

	private Double endInterval = null;
	private boolean endInclusion = true;

	private boolean outInterval = false;

	
	public CheckAutoVarIntervalItem(String xml){
		super();
		setSubItemType("variableTestRange");
        load (xml);
        
    }

	public CheckAutoVarIntervalItem(){
        super();
        subItemType = "variableTestRange";
    }
	
	
	public String getVarName() {
		return varName;
	}

	public void setVarName(String varName) {
		this.varName = varName;
	}

	public Double getStartInterval() {
		return startInterval;
	}

	public void setStartInterval(Double startInterval) {
		this.startInterval = startInterval;
	}

	public Double getEndInterval() {
		return endInterval;
	}

	public void setEndInterval(Double endInterval) {
		this.endInterval = endInterval;
	}

	public boolean isOutInterval() {
		return outInterval;
	}

	public void setOutInterval(boolean outInterval) {
		this.outInterval = outInterval;
	}
	
		
	public boolean isStartInclusion() {
		return startInclusion;
	}

	public void setStartInclusion(boolean startInclusion) {
		this.startInclusion = startInclusion;
	}

	public boolean isEndInclusion() {
		return endInclusion;
	}

	public void setEndInclusion(boolean endInclusion) {
		this.endInclusion = endInclusion;
	}
	
	public String getVarPath() {
		return varPath;
	}

	public void setVarPath(String varPath) {
		this.varPath = varPath;
	}

	public Double getVarValue() {
		return varValue;
	}

	public void setVarValue(Double varValue) {
		this.varValue = varValue;
	}
	


	   /* (non-Javadoc)
     * @see pt.lsts.neptus.types.XmlOutputMethods#asDocument(java.lang.String)
     */
    public Document asDocument(String rootElementName)
    {
        Document document = DocumentHelper.createDocument();
        Element root = document.addElement( rootElementName );
        
        
        
        root.addAttribute("checked", Boolean.toString(isChecked()));
      
        root.addElement("variableName").setText(getVarName());
        root.addElement("variablePath").setText(getVarPath());
        
        if(getVarValue() != null && !Double.isNaN(getVarValue()))
	        root.addElement("registedValue").setText(Double.toString(getVarValue()));
        
        Element intervalElem = root.addElement("interval"); 
        
        if(getStartInterval()!= null && !Double.isInfinite(getStartInterval()))
                intervalElem.addAttribute("start",Double.toString(getStartInterval()));
        if(getStartInterval()!= null && !Double.isInfinite(getEndInterval()))
        	intervalElem.addAttribute("end",Double.toString(getEndInterval()));
        	
       	intervalElem.addAttribute("startInclusion", Boolean.toString(isStartInclusion()));
       	intervalElem.addAttribute("endInclusion", Boolean.toString(isEndInclusion()));
       	intervalElem.addAttribute("complement", Boolean.toString(isOutInterval()));
        
        
        return document;
    }	
    
    public boolean load (String xml)
    {	               
    	String fileAsString = xml;
    	try
           {   //"/variableTestRange"
    		
               doc = DocumentHelper.parseText(fileAsString);
               this.setChecked(Boolean.parseBoolean(doc.selectSingleNode("/variableTestRange/@checked").getText()));
               Node nd = doc.selectSingleNode("/variableTestRange/variableName");
               if (nd != null)
                   this.setVarName(nd.getText());
               
               nd = doc.selectSingleNode("/variableTestRange/variablePath");
               if (nd != null) {
                   NeptusLog.pub().info("<###> "+nd.getText());
                   this.setVarPath(nd.getText());
               }
               nd = doc.selectSingleNode("/variableTestRange/registedValue");
               if (nd != null)
                   this.setVarValue(Double.parseDouble(nd.getText()));
                              
               nd = doc.selectSingleNode("/variableTestRange/interval");
               if (nd != null)
               {
            	   Node ndaux=nd.selectSingleNode("./@start");
            	   if(ndaux != null)
            		   this.setStartInterval(Double.parseDouble(ndaux.getText()));
            	   ndaux=nd.selectSingleNode("./@end");
            	   if(ndaux != null)
            		   this.setEndInterval(Double.parseDouble(ndaux.getText()));
            	   ndaux=nd.selectSingleNode("./@startInclusion");
            	   if(ndaux != null)
            		   this.setStartInclusion(Boolean.parseBoolean(ndaux.getText()));
            	   ndaux=nd.selectSingleNode("./@endInclusion");
            	   if(ndaux != null)
            		   this.setEndInclusion(Boolean.parseBoolean(ndaux.getText()));
            	   ndaux=nd.selectSingleNode("./@complement");
            	   if(ndaux != null)
            		   this.setOutInterval(Boolean.parseBoolean(ndaux.getText()));
               }
           } catch (DocumentException e)
           {
               NeptusLog.pub().error(this, e);
               return false;
           }
           return true;
    }
}
