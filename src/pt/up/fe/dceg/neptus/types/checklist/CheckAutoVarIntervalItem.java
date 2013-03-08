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
 * $Id:: CheckAutoVarIntervalItem.java 9616 2012-12-30 23:23:22Z pdias    $:
 */
package pt.up.fe.dceg.neptus.types.checklist;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import pt.up.fe.dceg.neptus.NeptusLog;
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
     * @see pt.up.fe.dceg.neptus.types.XmlOutputMethods#asDocument(java.lang.String)
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
                   System.out.println(nd.getText());
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
