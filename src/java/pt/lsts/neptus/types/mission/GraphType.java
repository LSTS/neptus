/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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
 * 16/Jan/2005
 */
package pt.lsts.neptus.types.mission;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Vector;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.mp.Maneuver;
import pt.lsts.neptus.mp.maneuvers.FollowPath;
import pt.lsts.neptus.types.XmlOutputMethods;
import pt.lsts.neptus.util.NameNormalizer;

/**
 * @author Paulo Dias
 * @author ZP
 */
public class GraphType implements XmlOutputMethods {

    protected static final String DEFAULT_ROOT_ELEMENT = "graph";

    //private Document doc;
    
    protected LinkedHashMap<String, Maneuver> maneuvers = new LinkedHashMap<String, Maneuver>();
    protected LinkedHashMap<String, TransitionType> transitions = new LinkedHashMap<String, TransitionType>();
    
    protected String initialManeuver = null;

    private boolean saveGotoSequenceAsTrajectory = false;
    
    public Vector<TransitionType> getExitingTransitions(Maneuver man) {
    	Vector<TransitionType> exitingTrans = new Vector<TransitionType>();
    	
    	for (TransitionType t : transitions.values()) {
    		if (t.sourceManeuver.equals(man.getId()))
    			exitingTrans.add(t);
    	}
    	
    	return exitingTrans;
    }
    
    public Vector<TransitionType> getIncomingTransitions(Maneuver man) {
    	Vector<TransitionType> incomingTrans = new Vector<TransitionType>();
    	
    	for (TransitionType t : transitions.values()) {
    		if (t.targetManeuver.equals(man.getId()))
    			incomingTrans.add(t);
    	}
    	
    	return incomingTrans;
    }
    
    
    /**
     * Null constructor
     */
    public GraphType() {
        super();
    }

    /**
     * @param xml
     */
    public GraphType(String xml) {
        super();
        load(xml);
    }

    /**
     * @param xml
     * @return
     */
    public boolean load(String xml) {
        try {
           Document  doc = DocumentHelper.parseText(xml);
            
            Object[] nodes  = doc.selectNodes("./graph/node").toArray();
            for (int i = 0; i < nodes.length; i++) {
                Element node = (Element) nodes[i];
                Maneuver man = Maneuver.createFromXML(node.asXML());
                if (man == null)
                    continue;
                addManeuver(man, false);
                if (man.isInitialManeuver()||initialManeuver == null) {
                    setInitialManeuver(man.getId());  
                }
            }
            
            Object[] edges = doc.selectNodes("/graph/edge").toArray();
            for (int i = 0; i < edges.length; i++) {
                Element edge = (Element) edges[i];
                
                String id = edge.selectSingleNode("./id").getText();
                String source = edge.selectSingleNode("./source").getText();
                String target = edge.selectSingleNode("./target").getText();
                String guard = edge.selectSingleNode("./guard").getText();
                if ("true".equalsIgnoreCase(guard))
                    guard = "ManeuverIsDone"; //FIXME For compatibility with old representation
                String actions = "";
                Node nda = edge.selectSingleNode("./actions");
                if (nda != null)
                    actions = nda.getText();
                addTransition(id, source, target, guard, actions);
            }
            

        } 
        catch (DocumentException e) {
            e.printStackTrace();
            return false;
        }
        //NeptusLog.pub().info("<###>the initial maneuver is "+getInitialManeuverId());
        return true;
    }

    public void addManeuver(Maneuver maneuver) {
        addManeuver(maneuver, true);
    }
    
    public void addManeuverAtEnd(Maneuver maneuver) {
        String lastManeuver = null;
        
        if (getLastManeuver() != null)
            lastManeuver = getLastManeuver().getId();
        addManeuver(maneuver);
        if (lastManeuver != null)
            addTransition(lastManeuver, maneuver.getId(), "true");
    }

    /**
     * Adds the given maneuver to this graph
     * If no maneuver has been added yet, then 
     * this maneuver is set to be the initial maneuver
     * @param maneuver The new maneuver to be added to the current graph
     */
    public void addManeuver(Maneuver maneuver, boolean clearInitialManeuverFlagIfOtherMAneuverIsSetToInitial) {
    	if (maneuver == null) {
    	    NeptusLog.pub().error("Maneuver is null, so not added!!");
    	    return;
    	}
        
        maneuvers.put(maneuver.getId(), maneuver);
        
        if (!clearInitialManeuverFlagIfOtherMAneuverIsSetToInitial)
            return;
        
        if (getInitialManeuverId() == null) {
        	maneuver.setInitialManeuver(true);
        	setInitialManeuver(maneuver.getId());
        }
        else {
        	maneuver.setInitialManeuver(false);
        }
    }
    
    public void removeManeuver(Maneuver maneuver) {
    	
    	if (getInitialManeuverId() != null && getInitialManeuverId().equals(maneuver.getId())) {
        	if (getFollowingManeuver(getInitialManeuverId()) != null) {
        		
        		setInitialManeuver(getFollowingManeuver(getInitialManeuverId()).getId());
        		getManeuver(getInitialManeuverId()).setInitialManeuver(true);
        	}        
        }
    	
    	for (TransitionType t : getExitingTransitions(maneuver)) {
    		transitions.remove(t.getId());
    	}

    	for (TransitionType t : getIncomingTransitions(maneuver)) {
    		transitions.remove(t.getId());
    	}
    	
    	maneuvers.remove(maneuver.getId());    	
    }
    
    
    /**
     * Returns the maneuver object identified by the string argument
     * or null if no such maneuver exists
     * @param maneuverID The string identifier of the maneuver to return
     * @return The identified maneuver or null if it doesn't exist
     */
    public Maneuver getManeuver(String maneuverID) {
        return (Maneuver) maneuvers.get(maneuverID);
    }
    

    /**
     * Adds a new transition to this graph (between two maneuver nodes)
     * @param sourceManeuverID The source node id
     * @param targetManeuverID The target node id
     * @param condition The transition condition (guard)
     */
    public TransitionType addTransition(String sourceManeuverID, String targetManeuverID, Object condition) {
        return addTransition(null, sourceManeuverID, targetManeuverID, condition);
    }

    public TransitionType addTransition(String sourceManeuverID, String targetManeuverID, Object condition, Object action) {
        return addTransition(null, sourceManeuverID, targetManeuverID, condition, action);
    }

    public TransitionType addTransition(String id, String sourceManeuverID, String targetManeuverID, Object condition) {
        return addTransition(id, sourceManeuverID, targetManeuverID, condition, "");
    }

    /**
     * Adds a new transition to this graph (between two maneuver nodes)
     * @param id ID of the transition
     * @param sourceManeuverID The source node id
     * @param targetManeuverID The target node id
     * @param condition The transition condition (guard)
     * @param action The transition action (outputs)
     */
    public TransitionType addTransition(String id, String sourceManeuverID, String targetManeuverID,
            Object condition, Object action) {
     
        // FIXME para ja as condicoes de transicao sao ignoradas 
        // e apenas existe uma condicao de saida de cada no...
        // No futuro isto tera de ser alterado!
    	
    	// Se já existir uma transição, esta é removida primeiro
    	removeTransition(sourceManeuverID, targetManeuverID);
    	
    	TransitionType tt = new TransitionType(sourceManeuverID, targetManeuverID);
    	if (id == null)
    	    tt.setId(NameNormalizer.getRandomID());
        else {
            if (id.equalsIgnoreCase(""))
                tt.setId(NameNormalizer.getRandomID());
            else
                tt.setId(id);
        }
    	ConditionType ct = new ConditionType();
    	ct.setCondition(condition.toString());
    	tt.setCondition(ct);
        ActionType at = new ActionType();
        at.setAction(action.toString());
        tt.setAction(at);
    	transitions.put(tt.getId(), tt);
        if (maneuvers.containsKey(sourceManeuverID)) {
            ((Maneuver) maneuvers.get(sourceManeuverID)).addTransition(targetManeuverID, "true");
    	}
        else {
            String error = "Error occured while adding transition from "+sourceManeuverID;
        	new Exception(error).printStackTrace();
            NeptusLog.pub().error(error)  ;
        }
        
        return tt;
    }
    
    public void removeTransition(TransitionType transition) {
        String sourceManeuver = transition.getSourceManeuver();
        String targetManeuver = transition.getTargetManeuver();
        
        TransitionType tt = transitions.remove(transition.id);
        if (tt == null)
            System.err.println("Tried to remove transition "+transition.id+" which doesn't exists");
        
        if (maneuvers.containsKey(sourceManeuver))
            maneuvers.get(sourceManeuver).removeTransition(targetManeuver);        
    }
    
    public void addTransition(TransitionType tt) {
        if (maneuvers.containsKey(tt.getSourceManeuver())) {
            maneuvers.get(tt.getSourceManeuver()).addTransition(tt.getTargetManeuver(), "true");
            transitions.put(tt.getId(), tt);
        }
        else {
            System.err.println("Transition from "+tt.getSourceManeuver()+" cannot be added because source maneuver does not exists.");
        }
    }
    
    public TransitionType removeTransition(String sourceManeuverID, String targetManeuverID) {    	
    	for (TransitionType tt : transitions.values()) {
    		if (tt.getSourceManeuver().equals(sourceManeuverID) && tt.getTargetManeuver().equals(targetManeuverID)) {
    			TransitionType removed = transitions.remove(tt.getId());
    			if (maneuvers.containsKey(sourceManeuverID))
    				((Maneuver) maneuvers.get(sourceManeuverID)).removeTransition(targetManeuverID);
    			//System.err.println("Removed the transition "+tt.getSourceManeuver()+"->"+tt.getTargetManeuver()+". Current no. of transitions: "+transitions.size());
    			return removed;
    		}
    	}    
    	return null;
    	
    }
    /**
     * Returns all the maneuvers that can be directly reached from the 
     * source maneuver given
     * @param sourceManeuverID The maneuver to test for transitions
     * @return An array of string with the reacheable maneuvers ids
     */
    public String[] getReacheableManeuvers(String sourceManeuverID) {
        if (maneuvers.get(sourceManeuverID) != null)
            return ((Maneuver) maneuvers.get(sourceManeuverID)).getReacheableManeuvers();
        else {
        	return null;
        }
    }
    
    /**
     * Returns the transition condition (guard) between the given maneuvers
     * Null is returned if a transition between the maneuvers doesn't exist
     * @param sourceManeuverID The maneuver id of the transition origin
     * @param targetManeuverID The maneuver id of the transition destiny
     * @return The condition object related with the transition
     */
    public Object getTransitionCondition(String sourceManeuverID, String targetManeuverID) {
        if (maneuvers.get(sourceManeuverID) != null)
            return ((Maneuver) maneuvers.get(sourceManeuverID)).getTransitionCondition(targetManeuverID);
        else
            return null;
    }
    
    public TransitionType getTransition(String sourceId, String targetId) {
        
        for (TransitionType tt : transitions.values()) {
            if (tt.getSourceManeuver().equals(sourceId) && tt.getTargetManeuver().equals(targetId))
                return tt;
        }
        return null;
    }
    
    /**
     * Returns an array with all the maneuvers in the current graph
     * @return An array with all the maneuvers in the current graph
     */
    public Maneuver[] getAllManeuvers() {        
    	return maneuvers.values().toArray(new Maneuver[] {});                
    }
    
    
    /**
     * Returns an array with all the maneuvers in the current graph
     * @return An array with all the maneuvers in the current graph
     */
    public TransitionType[] getAllEdges() {        
    	return transitions.values().toArray(new TransitionType[] {});
    }    
    
    public Maneuver[] getManeuversSequence(Maneuver startManeuver) {
    	Vector<Maneuver> mans = new Vector<Maneuver>();
    	
    	Vector<String> visitedIDs = new Vector<String>();

        String curID = startManeuver.getId();
        
        while (curID != null && !visitedIDs.contains(curID)) {
        	 visitedIDs.add(curID);
        	 mans.add(getManeuver(curID));        	
        	 String ways[] = getReacheableManeuvers(curID);
        	 if (ways == null)
        		 break;
        	 
             if (ways.length == 0)
                 curID = null;
             else if (ways.length == 1)
                 curID = ways[0];
             else {
            	 curID = null;
            	 for (int i = 0; i < ways.length; i++) {
            		 if (!visitedIDs.contains(ways[i])) {
            			 curID = ways[i];
            			 break;
            		 }
            	 }    	
             }
        }
        return mans.toArray(new Maneuver[] {});
    }
    
    public Maneuver[] getManeuversSequence() {    	
    	if (getInitialManeuverId() == null)
    		return new Maneuver[] {};
    	
    	return getManeuversSequence(getManeuver(getInitialManeuverId()));
    }
    
    public Maneuver getLastManeuver() {
    	Maneuver[] mans = getManeuversSequence();
    	if (mans.length > 0)
    		return mans[mans.length-1];
    	return null;
    }
    
    public Maneuver getFollowingManeuver(String maneuverID) {
    	String[] reachSet = getReacheableManeuvers(maneuverID);
    	
    	if (reachSet != null && reachSet.length > 0)
    		return maneuvers.get(reachSet[0]);
    	else 
    		return null;
    }
    
    public Maneuver[] getPreviousManeuvers(String maneuverID) {
    	//Maneuver[] seq = getManeuversSequence();
    	
    	if (!maneuvers.containsKey(maneuverID))
    		return new Maneuver[0];
    	
    	Vector<TransitionType> incomingTrans = getIncomingTransitions(maneuvers.get(maneuverID));
    	
    	Vector<Maneuver> prevMans = new Vector<Maneuver>();
    	
    	for (TransitionType t : incomingTrans) {
    		if (maneuvers.containsKey(t.getSourceManeuver()))
    			prevMans.add(maneuvers.get(t.getSourceManeuver()));
    	}
    	
    	return prevMans.toArray(new Maneuver[0]);
    }
    
    /**
     * @return
     */
    public String getGraphAsManeuversSeq() {
        Document document = DocumentHelper.createDocument();
        Element root = document.addElement("maneuvers");

        if (isSaveGotoSequenceAsTrajectory()) {
        	 Element manElemRoot = new FollowPath(this).asElement();
             Element idElem = (Element) manElemRoot.selectSingleNode("./id").detach();
             Element manElem = (Element) manElemRoot.selectSingleNode("./maneuver").detach();
             manElem.addAttribute("id", idElem.getText());           
             root.add(manElem);
             //System.err.println(FileUtil.getAsPrettyPrintFormatedXMLString(document));
             return document.asXML();
    	}

        Vector<String> visitedIDs = new Vector<String>();

        String curID = getInitialManeuverId();

        while (curID != null && !visitedIDs.contains(curID)) {
            Element manElemRoot = getManeuver(curID).asElement();
            Element idElem = (Element) manElemRoot.selectSingleNode("./id").detach();
            Element manElem = (Element) manElemRoot.selectSingleNode("./maneuver").detach();
            manElem.addAttribute("id", idElem.getText());
            //NeptusLog.pub().info("<###>ManElem:\n"+manElemRoot);
            root.add(manElem);

            visitedIDs.add(curID);
            String ways[] = getReacheableManeuvers(curID);
            if (ways.length > 1) {
                System.err.println("The function getGraphAsManeuverSequence() doesn't"
                        + " support more than one transitions for a node!\n"
                        + "Only the first node will be processed. ");
            }
            if (ways.length == 0)
                curID = null;
            else
                curID = ways[0];
        }

//        //TODO para teste
//        Element manElemRoot = root.addElement("maneuver");
//        manElemRoot.addAttribute("id", "testeFollowTraj");
//        manElemRoot.addElement("minTime").addText("0");
//        manElemRoot.addElement("maxTime").addText("10000");
//        
//        manElemRoot = manElemRoot.addElement("FollowTrajectory");
//        manElemRoot.addAttribute("kind", "automatic");
//	    Element finalPoint = manElemRoot.addElement("initialPoint");
//	    finalPoint.addAttribute("type", "pointType");
//	    LocationType lt = new LocationType();
//	    lt.translatePosition(2.0, 3.4, 1.1);
//	    Element point = lt.asElement("point");
//	    finalPoint.add(point);
//
//	    Element radTolerance = finalPoint.addElement("radiusTolerance");
//	    radTolerance.setText(String.valueOf(10.0));
//	   
//	    Element velocity = manElemRoot.addElement("velocity");
//	    velocity.addAttribute("tolerance", String.valueOf(10.0));
//	    velocity.addAttribute("type", "float");
//	    velocity.addAttribute("unit", "RPM");
//	    velocity.setText(String.valueOf(1000.0));
//	    
//	    Element trajectoryTolerance = manElemRoot.addElement("trajectoryTolerance");
//	    Element radiusTolerance = trajectoryTolerance.addElement("radiusTolerance");
//	    radiusTolerance.setText(String.valueOf(10.0));
//
//	    Element offsetsE = manElemRoot.addElement("offsets");
//	    Element offsE = offsetsE.addElement("offset");
//	    offsE.addElement("north-offset").setText("1");
//	    offsE.addElement("east-offset").setText("1");
//	    offsE.addElement("depth-offset").setText("1");
//
//	    offsE = offsetsE.addElement("offset");
//	    offsE.addElement("north-offset").setText("2.4");
//	    offsE.addElement("east-offset").setText("3.4");
//	    offsE.addElement("depth-offset").setText("3");
//
//	    offsE = offsetsE.addElement("offset");
//	    offsE.addElement("north-offset").setText("3");
//	    offsE.addElement("east-offset").setText("4");
//	    offsE.addElement("depth-offset").setText("5");
//
//        System.err.println(FileUtil.getAsPrettyPrintFormatedXMLString(document));

        //NeptusLog.pub().debug(document.asXML());
        //System.err.println(FileUtil.getAsPrettyPrintFormatedXMLString(document));
        return document.asXML();
    }
    
    
    

//    /**
//     * @deprecated
//     * @return
//     */
//    public String getGraphAsManeuversSeqOld()
//    {
//        Document document = DocumentHelper.createDocument();
//        Element root = document.addElement( "maneuvers" );
//        
//        Element nd = (Element)doc.selectSingleNode("/graph/node[@start='true']");
//        Element man =  (Element)nd.selectSingleNode("./maneuver");
//        root.add(man.createCopy().detach());
//        
//        XPath xpathSelector = DocumentHelper.createXPath("count(/graph/node)");
//        Number num = xpathSelector.numberValueOf(doc);
//        try
//        {
//            int nnodes = num.intValue();
//            NeptusLog.pub().debug(Integer.valueOf(nnodes));
//            
//            for (int i = 1; i < nnodes; i++)
//            {
//                Node nn = doc.selectSingleNode("/graph/edge[./source='" 
//                        + nd.selectSingleNode("./id").getText() + "']");
//                if (nn == null)
//                    break;
//                Element ed = (Element) nn;
//                String targetId = ed.selectSingleNode("./target").getText();
//                nd = (Element)doc.selectSingleNode("./graph/node[./id='"
//                        + targetId + "']");
//                man =  (Element)nd.selectSingleNode("./maneuver");
//                root.add(man.createCopy().detach());                
//            }
//        }
//        catch (NullPointerException e) 
//        {
//            //FIXME Temporary fix of bug#151 for ROV
//            return document.asXML();
//        }
//        catch (RuntimeException e)
//        {
//            e.printStackTrace();
//            return null;
//        }
//        
//        NeptusLog.pub().debug(document.asXML());
//        return document.asXML();
//    }
    

    
    /**
     * @return
     */
    public LinkedList<Maneuver> getGraphAsManeuversList() {
        LinkedList<Maneuver> ret = new LinkedList<Maneuver>();

        Vector<String> visitedIDs = new Vector<String>();

        String curID = getInitialManeuverId();

        while (curID != null && !visitedIDs.contains(curID)) {
            Maneuver man = getManeuver(curID);
            ret.add(man);
            visitedIDs.add(curID);
            String ways[] = getReacheableManeuvers(curID);
            
            if (ways != null && ways.length > 1) {
                System.err.println("The function getGraphAsManeuverSequence() doesn't"
                        + " support more than one transitions for a node!\n"
                        + "Only the first node will be processed. ");
            }
            if (ways == null || ways.length == 0)
                curID = null;
            else
                curID = ways[0];
        }

        return ret;
    }


    /* (non-Javadoc)
     * @see pt.lsts.neptus.types.XmlOutputMethods#asXML()
     */
    public String asXML()
    {
        String rootElementName = DEFAULT_ROOT_ELEMENT;
        return asXML(rootElementName);
    }

    
    /* (non-Javadoc)
     * @see pt.lsts.neptus.types.XmlOutputMethods#asXML(java.lang.String)
     */
    public String asXML(String rootElementName)
    {
        String result = "";        
        Document document = asDocument(rootElementName);
        result = document.asXML();
        return result;
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.types.XmlOutputMethods#asElement()
     */
    public Element asElement()
    {
        String rootElementName = DEFAULT_ROOT_ELEMENT;
        return asElement(rootElementName);
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.types.XmlOutputMethods#asElement(java.lang.String)
     */
    public Element asElement(String rootElementName)
    {
        return (Element) asDocument(rootElementName).getRootElement().detach();
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.types.XmlOutputMethods#asDocument()
     */
    public Document asDocument()
    {
        String rootElementName = DEFAULT_ROOT_ELEMENT;
        return asDocument(rootElementName);
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.types.XmlOutputMethods#asDocument(java.lang.String)
     */
    public Document asDocument(String rootElementName) {
    	    	
        Document document = DocumentHelper.createDocument();
        Element root = document.addElement( rootElementName );
               
        Maneuver[] mans = getAllManeuvers();
        
        for (int i = 0; i < mans.length; i++) {
        	//NeptusLog.pub().info("<###> "+mans[i].getId());
        	root.add(mans[i].asElement("node"));
        }
        
        TransitionType[] edges = getAllEdges();
        
        for (int i = 0; i < edges.length; i++) {
        	root.add(edges[i].asElement());
        }
		
        //NeptusLog.pub().info("<###>Graph-----------------\n"+document.asXML());
        return document;
        
        
        
        //TODO Fix this
        //if (doc != null)
        //Element graph = (Element)doc.selectSingleNode("/graph").detach();
        //document.add(graph);

    }
    
    public Document asDocument2(String rootElementName)
    {
    	NeptusLog.pub().info("<###>Save as trajectory!");
        Document document = DocumentHelper.createDocument();
        Element root = document.addElement( rootElementName );
        
        FollowPath trajManeuver = new FollowPath(this);
        
        root.add(trajManeuver.asElement("node"));
        
        return document;

    }
    /**
     * Return the identifier of the initial maneuver (state) of the graph
     * @return The id of the first maneuver
     */
    public String getInitialManeuverId() {
        if (maneuvers.containsKey(initialManeuver))
        	return initialManeuver;
        else
        	return null;
    }
    
    
    /**
     * Changes the initial maneuver to be the maneuver with the given identifier
     */
    public void setInitialManeuver(String initialManeuverId) {
        
    	if (initialManeuver != null && !initialManeuver.equals(initialManeuverId) && maneuvers.get(initialManeuver) != null)
        	maneuvers.get(initialManeuver).setInitialManeuver(false);
        
    	if (maneuvers.get(initialManeuverId) != null)
    		maneuvers.get(initialManeuverId).setInitialManeuver(true);
    	
    	this.initialManeuver = initialManeuverId;
    }

	public boolean isSaveGotoSequenceAsTrajectory() {
		return saveGotoSequenceAsTrajectory;
	}

	public void setSaveGotoSequenceAsTrajectory(boolean saveGotoSequenceAsTrajectory) {
		this.saveGotoSequenceAsTrajectory = saveGotoSequenceAsTrajectory;
	}
	
	public GraphType clone() {
	    
	    GraphType graph = new GraphType();
	    for (Maneuver m : maneuvers.values()) {
	        graph.addManeuver((Maneuver)m.clone());
	    }
	    for (TransitionType t : transitions.values()) {
//            graph.addTransition(t.getSourceManeuver(), t.getTargetManeuver(), 
//                    (ConditionType) t.getCondition().clone());
            try {
                graph.addTransition((TransitionType) t.clone());
            }
            catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }
	    }
	    graph.setInitialManeuver(getInitialManeuverId());
	    
	    return graph;
	}
	
	public void clear() {
		maneuvers.clear();
		transitions.clear();
		initialManeuver = null;
		setSaveGotoSequenceAsTrajectory(false);
	}

	/**
	 * @return the transitions
	 */
	public LinkedHashMap<String, TransitionType> getTransitions()
	{
		return transitions;
	}
}
