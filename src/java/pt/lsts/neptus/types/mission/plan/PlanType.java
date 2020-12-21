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
 * 2005/01/16
 */
package pt.lsts.neptus.types.mission.plan;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Vector;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.IMCOutputStream;
import pt.lsts.imc.PlanManeuver;
import pt.lsts.imc.PlanSpecification;
import pt.lsts.imc.PlanTransition;
import pt.lsts.imc.PlanVariable;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.comm.SystemUtils;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.gui.PropertiesProvider;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.Maneuver;
import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.mp.ManeuverLocation.Z_UNITS;
import pt.lsts.neptus.mp.SpeedType;
import pt.lsts.neptus.mp.SpeedType.Units;
import pt.lsts.neptus.mp.actions.PlanActions;
import pt.lsts.neptus.mp.element.PlanElements;
import pt.lsts.neptus.mp.maneuvers.IMCSerialization;
import pt.lsts.neptus.mp.maneuvers.LocatedManeuver;
import pt.lsts.neptus.mp.maneuvers.RowsManeuver;
import pt.lsts.neptus.params.ManeuverPayloadConfig;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.NameId;
import pt.lsts.neptus.types.XmlOutputMethods;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.MapGroup;
import pt.lsts.neptus.types.map.MapType;
import pt.lsts.neptus.types.map.PlanElement;
import pt.lsts.neptus.types.map.PlanUtil;
import pt.lsts.neptus.types.mission.GraphType;
import pt.lsts.neptus.types.mission.MissionType;
import pt.lsts.neptus.types.mission.TransitionType;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.types.vehicle.VehicleType.VehicleTypeEnum;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;
import pt.lsts.neptus.util.ByteUtil;
import pt.lsts.neptus.util.Dom4JUtil;
import pt.lsts.neptus.util.NameNormalizer;

/**
 * @author Paulo Dias
 * @author ZP
 */
public class PlanType implements XmlOutputMethods, PropertiesProvider, NameId {
    public static final int INIT_HOMEREF = 0, INIT_START_WPT = 1, INIT_NONE = 2;
    protected static final String DEFAULT_ROOT_ELEMENT = "plan";

    protected String id = NameNormalizer.getRandomID("pl"); // "id_"+System.currentTimeMillis();
    protected Vector<VehicleType> vehicles = new Vector<VehicleType>();
    protected MissionType missionType = null;
    protected GraphType graph = new GraphType();

    protected PlanActions startActions = new PlanActions();
    protected PlanActions endActions = new PlanActions();
    
    protected PlanElements planElements = new PlanElements();

    private MapType planMap = new MapType();
    private MapGroup mapGroup = null;

    private int startMode = INIT_START_WPT;    
    private static final String defaultVehicle = "lauv-xplore-1";
    
    public PlanType(MissionType mt) {
        super();
        this.graph = new GraphType();
        setMissionType(mt);
        setVehicle("isurus");
        mapGroup = MapGroup.getMapGroupInstance(mt);
    }

    
    
    /**
     * This method verifies that all the maneuvers in this plan have some incoming transition, returning true in that case. Otherwise it will return an exception
     * @return true if all maneuvers have at least one incoming transition
     * @throws Exception If some maneuver has no incoming transitions (unreacheable)
     */
    public boolean validatePlan() throws Exception {
        ArrayList<String> errors = new ArrayList<>();
        for (Maneuver man : getGraph().getAllManeuvers()) {
            if (man instanceof LocatedManeuver) {
                ManeuverLocation ml = ((LocatedManeuver) man).getManeuverLocation();
                if (ml.getZUnits() == Z_UNITS.NONE) {
                    errors.add(I18n.textf("The maneuver '%maneuver' has Z_UNITS set to NONE", man.getId()));
                }
                
                try {
                    VehicleType veh = getVehicleType();
                    if (veh != null) {
                        String vehTypeStr = veh.getType();
                        VehicleTypeEnum vehType = SystemUtils.getVehicleTypeFrom(vehTypeStr);
                        if (vehType == VehicleTypeEnum.UUV) {
                            if (ml.getZUnits() == Z_UNITS.ALTITUDE && ml.getZ() == 0) {
                                errors.add((I18n.textf("The maneuver '%maneuver' has Z_UNITS set to ALTITUDE and value 0!",
                                        man.getId())));
                            }
                        }
                    }
                }
                catch (Exception e) {
                    NeptusLog.pub().warn(e.getMessage());
                }
            }
        }

        for (Maneuver man : getGraph().getAllManeuvers()) {
            if (getGraph().getIncomingTransitions(man).isEmpty()
                    && !man.getId().equals(getGraph().getInitialManeuverId())) {
                errors.add((I18n.textf(
                        "The maneuver '%maneuver' has no incoming transitions and is not the initial maneuver!",
                        man.getId())));
            }
        }
        
        if (!errors.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            int count = 0;
            for (String err : errors) {
                if (count > 0)
                    sb.append("\n");
                sb.append(err);
                count++;
            }
            throw new Exception(sb.toString());
        }
        
        return true;
    }

    /**
     * 
     */
    public PlanType(String xml, MissionType mt) {
        super();
        load(xml);
        setMissionType(mt);
    }

    /** 
     * @see pt.lsts.neptus.types.mission.plan.AbstractPlanType#load(java.lang.String)
     */
    public boolean load(String xml) {
        // Clear data
        vehicles.clear();
        graph = null;
        startActions.clearMessages();
        endActions.clearMessages();
        planElements.getPlanElements().clear();

        try {
            Document doc = DocumentHelper.parseText(xml);
            this.setId(((Attribute)doc.selectSingleNode("/node()/@id")).getStringValue());
            try {
                String veh = ((Attribute)doc.selectSingleNode("/node()/@vehicle")).getStringValue();
                if (veh.contains(",")) {
                    String[] vehs = veh.split(",");
                    vehicles.clear();
                    for (String v : vehs)
                        addVehicle(v);
                }
                else
                    setVehicle(veh);
            }
            catch (Exception e) {
                if (getId() == null) {
                    setId(NameNormalizer.getRandomID("plan"));
                    NeptusLog.pub().error("plan has no valid id, using "+getId(), e);
                }
                if (getVehicle() == null) {
                    setVehicle(defaultVehicle);
                    NeptusLog.pub().error("plan with id "+getId()+" has no associated vehicle, using "+getVehicle(), e);
                }                
            }

            Node nd = doc.selectSingleNode("/node()/graph");
            graph = new GraphType(nd.asXML());

            nd = doc.selectSingleNode("/node()/actions/start-actions");
            if (nd != null) {
                startActions.load((Element)nd);
            }
            nd = doc.selectSingleNode("/node()/actions/end-actions");
            if (nd != null) {
                endActions.load((Element)nd);
            }

            nd = doc.selectSingleNode("/node()/elements");
            if (nd != null) {
                planElements.load(nd.asXML());
            }
        }
        catch (Exception e) {
            NeptusLog.pub().error(this, e);
            return false;
        }
        return true;
    }

    /**
     * @return Returns the id.
     */
    public String getId() {
        return id;
    }

    /**
     * @param id
     *            The id to set.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return Returns the vehicle.
     */
    public String getVehicle() {
        if (vehicles.isEmpty())
            return null;
        else
            return getVehicles().firstElement().toString();
    }

    /**
     * Adds the given vehicle to the set of vehicles in this plan
     * @param vehicle The vehicle to be added to this plan
     */
    public void addVehicle(String vehicle) {
        VehicleType vt = VehiclesHolder.getVehicleById(vehicle);
        if (vt != null && !vehicles.contains(vt))
            vehicles.add(vt);
    }

    /**
     * @param vehicle
     *            The vehicle to set.
     */
    public void setVehicle(String vehicle) {
        if (vehicle == null) {
            setVehicles(new ArrayList<VehicleType>());
//            for (Maneuver m : getGraph().getAllManeuvers())
//                m.setVehicles(null);
        }
        VehicleType vt = VehiclesHolder.getVehicleById(vehicle);
        if (vt != null) {
            this.setVehicleType(vt);
//            for (Maneuver m : getGraph().getAllManeuvers())
//                m.setVehicles(Arrays.asList(vt));
        }
    }

    /**
     * @param vehicle
     */
    public void setVehicle(VehicleType vehicle) {        
        setVehicleType(vehicle);
    }

    /**
     * @return Returns the graph.
     */
    public GraphType getGraph() {
        return graph;
    }

    @Override
    public String asXML() {
        String rootElementName = DEFAULT_ROOT_ELEMENT;
        return asXML(rootElementName);
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.types.XmlOutputMethods#asXML(java.lang.String)
     */
    @Override
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
    @Override
    public Element asElement() {
        String rootElementName = DEFAULT_ROOT_ELEMENT;
        return asElement(rootElementName);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * pt.lsts.neptus.types.XmlOutputMethods#asElement(java.lang.String)
     */
    @Override
    public Element asElement(String rootElementName) {
        return asDocument(rootElementName).getRootElement();
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.types.XmlOutputMethods#asDocument()
     */
    @Override
    public Document asDocument() {
        String rootElementName = DEFAULT_ROOT_ELEMENT;
        return asDocument(rootElementName);
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.types.XmlOutputMethods#asDocument(java.lang.String)
     */
    @Override
    public Document asDocument(String rootElementName){
        Document document = DocumentHelper.createDocument();
        Element root = document.addElement( rootElementName );

        //root.addElement( "id" ).addText(getId());
        root.addAttribute( "id", getId());
        if (vehicles.size() <= 1)
            root.addAttribute( "vehicle", getVehicle());
        else {
            String str = getVehicle().toString();

            for (int i = 1; i < vehicles.size(); i++) {
                str += ","+vehicles.get(i).toString();
            }
            root.addAttribute("vehicle", str);
        }
        root.add(getGraph().asElement());

        Element sActionsElm = startActions.asElement("start-actions");
        Element eActionsElm = endActions.asElement("end-actions");
        if (sActionsElm.hasContent() || eActionsElm.hasContent()) {
            Element acElm = root.addElement("actions");
            if (sActionsElm.hasContent())
                acElm.add(sActionsElm);
            if (eActionsElm.hasContent())
                acElm.add(eActionsElm);
        }

        org.w3c.dom.Document w3cDoc = planElements.asElement("elements").getOwnerDocument();
        Document d4jDoc = Dom4JUtil.convertDOMtoDOM4J(w3cDoc);
        Element planElementsElm = (Element) d4jDoc.getRootElement();
        if (planElementsElm.hasContent())
            root.add(planElementsElm);

        //        NeptusLog.pub().info("<###>Plan-----------------\n"+document.asXML());
        return document;
    }


    public boolean hasMultipleVehiclesAssociated() {
        return vehicles.size() > 1;
    }
    
    public Vector<VehicleType> getVehicles() {
        return vehicles;
    }

    public void setVehicles(Collection<VehicleType> vehicles) {
        this.vehicles.clear();
        for (VehicleType v : vehicles)
            if (!this.vehicles.contains(v))
                this.vehicles.add(v);      
        for (Maneuver m : getGraph().getAllManeuvers())
            m.setVehicles(this.vehicles);
    }

    /**
     * @return
     */
    public VehicleType getVehicleType() {
        if (vehicles.isEmpty())
            return null;
        return vehicles.firstElement();

    }
    
    
    /**
     * @param vehicleType
     */
    private void setVehicleType(VehicleType vehicleType) {
        vehicles.clear();
        if (vehicleType != null)
            vehicles.add(vehicleType);
        for (Maneuver m : getGraph().getAllManeuvers())
            m.setVehicles(this.vehicles);
    }

    /**
     * @return
     */
    public boolean isEmpty() {
        return graph.getAllManeuvers().length == 0;
    }

    /**
     * @return
     */
    public boolean hasInitialManeuver() {
        return graph.getInitialManeuverId() != null;
    }

    /**
     * @return
     */
    public MissionType getMissionType() {
        return missionType;
    }

    /**
     * @param missionType
     */
    public void setMissionType(MissionType missionType) {
        this.missionType = missionType;
    }

    /**
     * @return
     */
    public MapGroup getMapGroup() {
        if (mapGroup == null && missionType != null)
            mapGroup = MapGroup.getMapGroupInstance(missionType);
        return mapGroup;
    }

    /**
     * @param mapGroup
     */
    public void setMapGroup(MapGroup mapGroup) {
        this.mapGroup = mapGroup;
    }

    /**
     * @return
     */
    public MapType getPlanMap() {
        return planMap;
    }

    /**
     * @param planMap
     */
    public void setPlanMap(MapType planMap) {
        this.planMap = planMap;
    }

    /**
     * @param newID
     * @return
     */
    public PlanType copy(String newID) {
        PlanType copy = new PlanType(asXML(), getMissionType());
        copy.setId(newID);
        return copy;
    }

    /**
     * @return
     */
    public boolean isSaveGotoSequenceAsTrajectory() {
        return getGraph().isSaveGotoSequenceAsTrajectory();
    }

    /**
     * @param saveGotoSequenceAsTrajectory
     */
    public void setSaveGotoSequenceAsTrajectory(boolean saveGotoSequenceAsTrajectory) {
        getGraph().setSaveGotoSequenceAsTrajectory(saveGotoSequenceAsTrajectory);
    }

    /**
     * @param vehicle
     * @return
     */
    public boolean isSupportedBy(VehicleType vehicle) {
        return getVehicleType().getId().equals(vehicle.getId());
    }


    /**
     * @return
     */
    public PlanType clonePlan() {
        PlanType plan = new PlanType(this.missionType);
        plan.load(asXML());
//        plan.setId(getId());
//        Vector<VehicleType> vehicles = new Vector<VehicleType>();
//        vehicles.addAll(getVehicles());
//        plan.setVehicles(vehicles);
//
//        plan.graph = graph.clone();
//
//        plan.startActions = new PlanActions();
//        plan.startActions.load(startActions.asElement("start-actions"));
//        plan.endActions = new PlanActions();
//        plan.endActions.load(endActions.asElement("end-actions"));
//
//        plan.planElements = new PlanElements();
//        plan.planElements.load(planElements.asElement("elements"));
        return plan;		
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {	
        return super.clone();
    }

    public String toStringWithVehicles() {
        return toStringWithVehicles(false);
    }

    public String toStringWithVehicles(boolean extended) {
        String md5 = extended ? ByteUtil.encodeAsString(asIMCPlan(false).payloadMD5()) : "";
        String idStr = this.getId() + (extended ? "[md5:" + md5 + "]" : "");
        if (hasMultipleVehiclesAssociated()) {
            String ret = idStr+" ["+vehicles.firstElement();
            for (int i = 1; i < vehicles.size(); i++)
                ret += ", "+vehicles.get(i);
            return ret +"]";
        }
        else if (getVehicle() != null)
            return idStr+" ["+getVehicle()+"]";
        else
            return idStr;
    }
    
    @Override
    public String toString() {
        return this.getId();
    }

    /**
     * @param startMode
     */
    public void setStartMode(int startMode) {
        this.startMode = startMode;
    }

    /**
     * @return
     */
    public int getStartMode() {
        return startMode;
    }	

    /**
     * @param maneuverId
     * @return
     */
    public Vector<TransitionType> getExitingTransitions(String maneuverId) {
        return getGraph().getExitingTransitions(getGraph().getManeuver(maneuverId));
    }


    public void setPayloads() {
        Maneuver[] mans = getGraph().getAllManeuvers();
        for (Maneuver m : mans) {
            new ManeuverPayloadConfig(getVehicle(), m, null).getProperties();            
        }
    }
    
    public IMCMessage asIMCPlan() {
        return asIMCPlan(false);
    }
    
    /**
     * @return
     */
    public IMCMessage asIMCPlan(boolean fillInPayloads) {
        
        if (fillInPayloads)
            setPayloads();
        
        PlanSpecification plan = new PlanSpecification();
        plan.setPlanId(getId());
        plan.setDescription("");
        plan.setStartManId(getGraph().getInitialManeuverId());
        ArrayList<PlanVariable> vars = new ArrayList<PlanVariable>();
        plan.setVariables(vars);
        IMCMessage[] msgs = getStartActions().getAllMessages();
        if (msgs != null)
            plan.setStartActions(Arrays.asList(msgs));
        msgs = getEndActions().getAllMessages();
        if (msgs != null)
            plan.setEndActions(Arrays.asList(msgs));
        
        Vector<PlanManeuver> maneuvers = new Vector<>();
        for (Maneuver man : getGraph().getAllManeuvers()) {
            if (man instanceof IMCSerialization) {
                PlanManeuver m = new PlanManeuver();
                m.setManeuverId(man.getId());
                m.setValue("data", ((IMCSerialization)man).serializeToIMC());
                msgs = man.getStartActions().getAllMessages();
                if (msgs != null)
                    m.setStartActions(Arrays.asList(msgs));
                msgs = man.getEndActions().getAllMessages();
                if (msgs != null)
                    m.setEndActions(Arrays.asList(msgs));
                
                maneuvers.add(m);
            }
            else {
                NeptusLog.pub()
                .error("Error converting to IMC Plan: the maneuver "
                        + man.getId()
                        + " is not compatible with the IMC Protocol");
                return null;
            }
        }
        
        Vector<PlanTransition> transitions = new Vector<>();
        
        for (TransitionType tt : getGraph().getTransitions().values()) {
            PlanTransition pt = new PlanTransition();
            pt.setSourceMan(tt.getSourceManeuver());
            pt.setDestMan(tt.getTargetManeuver());
            pt.setConditions(tt.getCondition().toString());
            transitions.add(pt);
        }

        plan.setManeuvers(maneuvers);
        plan.setTransitions(transitions);
        
        return plan;
    }


    public String planStatistics() {
        return PlanUtil.getPlanStatisticsAsText(this, "Edited Plan Statistics", false, false);
    }

    public void getAsImage(BufferedImage img) {

        StateRenderer2D r2d = new StateRenderer2D(MapGroup.getMapGroupInstance(getMissionType()));
        PlanElement po = new PlanElement(r2d.getMapGroup(), new MapType());
        po.setTransp2d(1);
        po.setPlan(this);
        po.setRenderer(r2d);
        po.setColor(new Color(255,255,255,128));
        po.setShowDistances(true);
        po.setShowManNames(true);

        int numLocs = 0;
        double offsets[] = new double[2];

        for (Maneuver m : getGraph().getAllManeuvers()) {
            if (m instanceof LocatedManeuver) {
                double offs[] = ((LocatedManeuver)m).getManeuverLocation().getOffsetFrom(getMissionType().getHomeRef());
                offsets[0] += offs[0];
                offsets[1] += offs[1];
                numLocs++;
            }
        }

        if (numLocs > 0) {
            offsets[0] = offsets[0]/numLocs;
            offsets[1] = offsets[1]/numLocs;
            LocationType loc = new LocationType(getMissionType().getHomeRef());
            loc.translatePosition(offsets[0], offsets[1], 0);
            r2d.setCenter(loc);
        }

        r2d.addPostRenderPainter(po, "Plan Painter");
        r2d.setSize(img.getWidth(), img.getHeight());
        r2d.setBounds(0, 0, img.getWidth(), img.getHeight());
        Graphics2D g = img.createGraphics();
        r2d.update(g);
        g.dispose();
    }	

    /* (non-Javadoc)
     * @see pt.lsts.neptus.gui.PropertiesProvider#getProperties()
     */
    @Override
    public DefaultProperty[] getProperties() {
        Vector<DefaultProperty> props = new Vector<DefaultProperty>();
        
        DefaultProperty id = PropertiesEditor.getPropertyInstance("ID", "Generic properties", String.class, getId(), false);
        id.setShortDescription("The identifier for this plan");
        props.add(id);

        DefaultProperty sAcions = PropertiesEditor.getPropertyInstance("start-actions",
                "Plan " + " start actions", PlanActions.class, startActions, true);
        props.add(sAcions);
        DefaultProperty eAcions = PropertiesEditor.getPropertyInstance("end-actions",
                "Plan " + " end actions", PlanActions.class, endActions, true);
        props.add(eAcions);
        
        return props.toArray(new DefaultProperty[]{});

    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.gui.PropertiesProvider#setProperties(com.l2fprod.common.propertysheet.Property[])
     */
    @Override
    public void setProperties(Property[] properties) {
        for (Property p : properties) {
            try {
                if (p.getCategory() != null && p.getCategory().equalsIgnoreCase("Plan " + " start actions")) {
                    String pName = p.getName();
                    if (!pName.equalsIgnoreCase("start-actions"))
                        continue;
                    // Found prop.
                    PlanActions value = (PlanActions) p.getValue();
                    startActions = value;
                }
                else if (p.getCategory() != null && p.getCategory().equalsIgnoreCase("Plan " + " end actions")) {
                    String pName = p.getName();
                    if (!pName.equalsIgnoreCase("end-actions"))
                        continue;
                    // Found prop.
                    PlanActions value = (PlanActions) p.getValue();
                    endActions = value;
                }
            }
            catch (Exception e) {
                NeptusLog.pub().error(e);
            }
        }
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.gui.PropertiesProvider#getPropertiesDialogTitle()
     */
    @Override
    public String getPropertiesDialogTitle() {
        return "'" + getId() + "' plan properties";
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.gui.PropertiesProvider#getPropertiesErrors(com.l2fprod.common.propertysheet.Property[])
     */
    @Override
    public String[] getPropertiesErrors(Property[] properties) {
        Vector<String> errors = new Vector<String>();       
        return errors.toArray(new String[] {});
    }

    /**
     * @return the startActions
     */
    public PlanActions getStartActions() {
        return startActions;
    }
    
    /**
     * @return the endActions
     */
    public PlanActions getEndActions() {
        return endActions;
    }
    
    /**
     * @return the planElements
     */
    public PlanElements getPlanElements() {
        return planElements;
    }
    
    public static void main(String[] args) throws Exception {
//        ConfigFetch.initialize();
//
//        MissionType mt = new MissionType("./missions/APDL/missao-apdl.nmisz");
//        PlanType pivot = mt.getIndividualPlansList().values().iterator().next();
//
//        BufferedImage bi = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
//
//        pivot.getAsImage(bi);
//
//        JLabel lbl = new JLabel(new ImageIcon(bi));
//        GuiUtils.testFrame(lbl);

//        byte[] bytes = new byte[] {
//                0x05, 0x00, 0x72, 0x6f, 0x77, 0x73, 0x31, 0x00, 0x00, 0x00, 0x00, 0x00,
//                0x00, 0x0d, 0x00, 0x52, 0x6f, 0x77, 0x73, 0x4d, 0x61, 0x6e, 0x65, 0x75, 0x76, 0x65, 0x72, 0x31,
//                0x01, 0x00, 0x2b, 0x02, 0x0d, 0x00, 0x52, 0x6f, 0x77, 0x73, 0x4d, 0x61, 0x6e, 0x65, 0x75, 0x76,
//                0x65, 0x72, 0x31, (byte) 0xca, 0x01, 0x10, 0x27, 0x6e, 0x7f, 0x17, 0x60, 0x66, 0x00, (byte) 0xe7, 0x3f, (byte) 0x8e,
//                (byte) 0xb8, (byte) 0xf4, (byte) 0xff, (byte) 0xec, 0x73, (byte) 0xc3, (byte) 0xbf, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x7a, 0x44,
//                0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
//                0x00, 0x00, 0x00, (byte) 0xc8, 0x42, 0x00, 0x00, 0x48, 0x43, 0x00, 0x00, (byte) 0xd8, 0x41, 0x0f, 0x64, 0x03,
//                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
//                
//        };
//        MessageDigest md = MessageDigest.getInstance("MD5");
//        md.update(bytes);
//        NeptusLog.pub().info("<###> "+ByteUtil.encodeAsString(md.digest()) + " = \n" + "61245d173dc0d18b97b54541a0d14284");
//        
        
        
        {
            PlanType plan1 = new PlanType(new MissionType());
            
            RowsManeuver rows = new RowsManeuver();
            rows.setSpeed(new SpeedType(1.7, Units.MPS));
            ManeuverLocation loc = new ManeuverLocation();
            loc.setLatitudeStr("41N11'6.139669166224781''");
            loc.setLongitudeStr("8W42'21.723814187086976''");
            rows.setManeuverLocation(loc);
            
            plan1.getGraph().addManeuver(rows);
            
            IMCMessage p1 = plan1.asIMCPlan();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            IMCOutputStream imcOs = new IMCOutputStream(baos);
            p1.serialize(imcOs);

            ByteUtil.dumpAsHex(baos.toByteArray(), System.out);
            String p1Md5 = ByteUtil.encodeAsString(p1.payloadMD5());
            NeptusLog.pub().info("<###> "+p1Md5);
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] ba = baos.toByteArray();
            md.update(Arrays.copyOfRange(ba, 0x14, ba.length - 2));
            String p1Md5Calc = ByteUtil.encodeAsString(md.digest());
            NeptusLog.pub().info("<###> "+p1Md5Calc);
                        
//            ByteArrayInputStream bais = new ByteArrayInputStream(ba);
//            IMCInputStream imcIs = new IMCInputStream(bais);
            
            IMCMessage p2 = IMCDefinition.getInstance().parseMessage(ba);
            p2.setTimestamp(System.currentTimeMillis());
            p2.setSrc(0x15);
            PlanType plan2 = IMCUtils.parsePlanSpecification(new MissionType(), p2);
            
            PlanType plan3 = plan2.clonePlan();

            NeptusLog.pub().info("<###> "+ByteUtil.encodeAsString(p2.payloadMD5()));
            NeptusLog.pub().info("<###> "+ByteUtil.encodeAsString(plan2.asIMCPlan().payloadMD5()));
            NeptusLog.pub().info("<###> "+ByteUtil.encodeAsString(plan3.asIMCPlan().payloadMD5()));
        }
        
        {
            //Double. 99 71  0c 54 87 00 e7 3f
            LocationType loc = new LocationType();
            loc.setLatitudeStr("41N11'6.139669166224781''");
            loc.setLongitudeStr("8W42'21.723814187086976''");
            NeptusLog.pub().info("<###> "+loc.getLatitudeDegs()  + "  " + loc.getLongitudeDegs());
            NeptusLog.pub().info("<###> "+Long.toHexString(Double.doubleToLongBits(loc.getLatitudeRads()))  + "  " + 
                    Long.toHexString(Double.doubleToLongBits(loc.getLongitudeRads())));
            NeptusLog.pub().info("<###> "+Double.longBitsToDouble(0x3fe70087540c7199L));
            NeptusLog.pub().info("<###> "+Double.longBitsToDouble(0x3fe70087540c719fL));
            
            double la1r = Double.longBitsToDouble(0x3fe70087540c7199L);
            double la2r = Double.longBitsToDouble(0x3fe70087540c719fL);
            double la1d = Math.toDegrees(la1r);
            double la2d = Math.toDegrees(la2r);
            NeptusLog.pub().info("<###> "+la1d);
            NeptusLog.pub().info("<###> "+la2d);
            
            double absoluteLatLonDepth[] = loc.getAbsoluteLatLonDepth(); 
            double latRad = Math.toRadians(absoluteLatLonDepth[0]);
            Math.toRadians(absoluteLatLonDepth[1]);
            NeptusLog.pub().info("<###> "+latRad);
            NeptusLog.pub().info("<###> "+Math.toDegrees(latRad));
            NeptusLog.pub().info("<###> "+Long.toHexString(Double.doubleToLongBits(latRad)));

        }
    }
    
    public boolean isCompatibleWith(VehicleType vehicle) {
        if (getVehicle().equals(vehicle.getName()))
            return true;
        
        for (Maneuver m : getGraph().getAllManeuvers()) {
            if (!vehicle.getFeasibleManeuvers().containsKey(m.getType()))
                return false;
            // FIXME test the payloads
        }
        
        return true;
    }

    @Override
    public String getIdentification() {
        return getId();
    }

    @Override
    public String getDisplayName() {
        return getId();
    }
}
