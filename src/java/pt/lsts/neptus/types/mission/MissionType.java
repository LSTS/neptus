/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
 * 2005/01/14
 */
package pt.lsts.neptus.types.mission;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.StringTokenizer;
import java.util.TreeMap;

import javax.swing.ProgressMonitor;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import pt.lsts.imc.ImcStringDefs;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.types.XmlInputMethods;
import pt.lsts.neptus.types.XmlInputMethodsFromFile;
import pt.lsts.neptus.types.XmlOutputMethods;
import pt.lsts.neptus.types.coord.CoordinateSystem;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.MapGroup;
import pt.lsts.neptus.types.map.MapType;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;
import pt.lsts.neptus.util.DateTimeUtil;
import pt.lsts.neptus.util.Dom4JUtil;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.NameNormalizer;
import pt.lsts.neptus.util.ReflectionUtil;
import pt.lsts.neptus.util.XMLValidator;
import pt.lsts.neptus.util.ZipUtils;
import pt.lsts.neptus.util.conf.ConfigFetch;

/**
 * Refactored in 06/11/2006.
 * 
 * @author Paulo Dias
 * @author ZP
 */
public class MissionType implements XmlOutputMethods, XmlInputMethods, XmlInputMethodsFromFile {
    protected static final String DEFAULT_ROOT_ELEMENT = "mission-def";

    private String id = NameNormalizer.getRandomID();
    private String name = "Unnamed Mission";
    private String type = "Test";
    private String description = "No description available";

    private final LinkedList<String> notesList = new LinkedList<String>();

    private LinkedHashMap<String, VehicleMission> vehiclesList = new LinkedHashMap<String, VehicleMission>();
    private LinkedHashMap<String, MapMission> mapsList = new LinkedHashMap<String, MapMission>();

    private LinkedHashMap<String, ChecklistMission> checklistsList = new LinkedHashMap<String, ChecklistMission>();

    private HomeReference homeRef = new HomeReference();

    private TreeMap<String, PlanType> individualPlansList = new TreeMap<String, PlanType>();

    private Document doc;
    protected boolean isLoadOk = true;

    private String originalFilePath = "";
    private String compressedFilePath = null;

    private Component parentWindow = null;
    boolean showProgress = true;

    /**
     * 
     */
    public MissionType(String url) {
        super();
        loadFile(url);
    }

    /**
     * 
     */
    public MissionType(String url, Component parentWindow) {
        super();
        this.showProgress = true;
        this.parentWindow = parentWindow;
        loadFile(url);
    }

    public MissionType() {
        super();
    }

    // @Deprecated //WHY?
    public static MissionType createZippedMission(File fileToSave) {
        String sep = System.getProperty("file.separator", "/");
        String tmpDir = ConfigFetch.getNeptusTmpDir();

        MissionType mt = new MissionType();
        File outDir = new File(tmpDir, "mission_" + mt.getId());
        if (outDir.exists()) {
            FileUtil.deltree(outDir.getAbsolutePath());
        }
        outDir.mkdirs();
        outDir.deleteOnExit();

        mt.setOriginalFilePath(new File(outDir, "mission.nmis").getAbsolutePath());
        mt.setCompressedFilePath(fileToSave.getAbsolutePath());

        MapType map = new MapType();
        map.saveFile(outDir + sep + "maps" + sep + "map.nmap");

        MapMission mm = new MapMission();
        mm.setHref(map.getHref());
        mm.setId(map.getId());
        mm.setName(map.getName());
        mm.setMap(map);

        mt.addMap(mm);

        mt.save(false);
        return mt;

    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.types.XmlInputMethods#load(org.dom4j.Element)
     */
    @Override
    public boolean load(Element elem) {
        doc = Dom4JUtil.elementToDocument(elem);
        if (doc == null) {
            isLoadOk = false;
            return false;
        }
        return load();
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.types.XmlInputMethods#load(java.lang.String)
     */
    @Override
    public boolean load(String xml) {
        try {
            doc = DocumentHelper.parseText(xml);
        }
        catch (DocumentException e) {
            e.printStackTrace();
            isLoadOk = false;
            return false;
        }
        return load();
    }

    /**
     * @return
     */
    private boolean load() {
        long initTime = System.currentTimeMillis();

        ProgressMonitor pmon = null;
        if (showProgress) {
            pmon = new ProgressMonitor(ConfigFetch.getSuperParentFrame(), I18n.text("Loading mission"),
                    I18n.text("Verifying against schema..."), 0, 100);
            pmon.setMillisToDecideToPopup(1000);
        }

        try {
            if (showProgress) {
                pmon.setProgress(10);
                pmon.setNote(I18n.text("Loading mission header..."));
            }

            isLoadOk = validate(doc);

            this.setId(doc.selectSingleNode("/mission-def/header/id").getText());
            this.setName(doc.selectSingleNode("/mission-def/header/name").getText());
            this.setType(doc.selectSingleNode("/mission-def/header/type").getText());
            Node nd = doc.selectSingleNode("/mission-def/header/description");
            if (nd != null)
                this.setDescription(nd.getText());

            List<?> lst = doc.selectNodes("/mission-def/header/notes/note");
            ListIterator<?> lstIt = lst.listIterator();
            while (lstIt.hasNext()) {
                Element elem = (Element) lstIt.next();
                notesList.add(elem.getText());
            }

            // FIXED De Referential para Reference
            nd = doc.selectSingleNode("/mission-def/definitions/home-referential");
            if (nd == null) {
                nd = doc.selectSingleNode("/mission-def/definitions/home-reference");
            }

            if (showProgress) {
                pmon.setProgress(20);
                pmon.setNote(I18n.text("Loading home reference..."));
            }

            if (nd != null) {
                homeRef = new HomeReference(nd.asXML());
                // homeRef = new HomeReference((Element)nd);
            }

            if (showProgress) {
                pmon.setProgress(40);
                pmon.setNote(I18n.text("Loading vehicles..."));
            }

            lst = doc.selectNodes("/mission-def/definitions/vehicles/vehicle");
            lstIt = lst.listIterator();
            while (lstIt.hasNext()) {
                Element velem = (Element) lstIt.next();
                VehicleMission vm = new VehicleMission();
                vm.setId(velem.selectSingleNode("./id").getText());
                vm.setName(velem.selectSingleNode("./name").getText());
                // FIXED De Referential para Reference
                nd = velem.selectSingleNode("./vehicle-referential");
                if (nd == null) {
                    nd = velem.selectSingleNode("./vehicle-reference");
                }

                if (nd == null) {
                    vm.setCoordinateSystem(homeRef);
                    vm.setHomeRefUsed(true);
                }
                else {
                    String xml1 = nd.asXML();
                    CoordinateSystem cs = new CoordinateSystem(xml1);
                    vm.setCoordinateSystem(cs);
                }
                vm.setVehicle(VehiclesHolder.getVehicleById(vm.getId()));
                vehiclesList.put(vm.getId(), vm);
            }

            lst = doc.selectNodes("/mission-def/definitions/maps/map");
            lstIt = lst.listIterator();

            if (showProgress) {
                pmon.setProgress(60);
                pmon.setNote(I18n.text("Loading maps..."));
            }
            while (lstIt.hasNext()) {
                Element velem = (Element) lstIt.next();
                MapMission mm = new MapMission();
                mm.setId(velem.selectSingleNode("./id").getText());
                mm.setName(velem.selectSingleNode("./name").getText());
                if ("".equals(originalFilePath))
                    mm.setHref(velem.selectSingleNode("./href").getText());
                else
                    mm.setHrefAndLoadMap(ConfigFetch.resolvePathWithParent(originalFilePath,
                            velem.selectSingleNode("./href").getText()));
                mapsList.put(mm.getId(), mm);
            }
            // generateMapGroup();

            if (showProgress) {
                pmon.setProgress(80);
                pmon.setNote(I18n.text("Loading Checklists..."));
            }
            lst = doc.selectNodes("/mission-def/definitions/checklists/checklist");
            lstIt = lst.listIterator();
            while (lstIt.hasNext()) {
                Element velem = (Element) lstIt.next();
                ChecklistMission cm = new ChecklistMission();
                cm.setId(velem.selectSingleNode("./id").getText());
                cm.setName(velem.selectSingleNode("./name").getText());
                if ("".equals(originalFilePath))
                    cm.setHref(velem.selectSingleNode("./href").getText());
                else
                    cm.setHrefAndLoadChecklist(ConfigFetch.resolvePathWithParent(originalFilePath, velem
                            .selectSingleNode("./href").getText()));
                checklistsList.put(cm.getId(), cm);
            }

            if (showProgress) {
                pmon.setProgress(95);
                pmon.setNote(I18n.text("Loading plans..."));
            }
            lst = doc.selectNodes("/mission-def/body/plan");
            if (lst == null || lst.size() == 0) {
                lst = doc.selectNodes("/mission-def/body/individual-plan");
            }
            lstIt = lst.listIterator();
            while (lstIt.hasNext()) {
                Element elem = (Element) lstIt.next();
                PlanType mm = new PlanType(elem.asXML(), this);
                individualPlansList.put(mm.getId(), mm);
            }
        }
        catch (Exception e) {
            NeptusLog.pub().error(this, e);
            isLoadOk = false;
            long totalTime = System.currentTimeMillis() - initTime;
            NeptusLog.pub().info(this + ": Total mission load time: " + totalTime + " ms.");
            return false;
        }
        if (showProgress) {
            pmon.setProgress(100);
            pmon.setNote(I18n.text("Complete"));
        }
        long totalTime = System.currentTimeMillis() - initTime;
        NeptusLog.pub().debug(this + ": Total mission load time: " + totalTime + " ms.");

        isLoadOk = true;
        MapGroup.resetMissionInstance(this);
        return true;
    }

    @Override
    public boolean loadFile(File file) {
        return loadFile(file.getAbsolutePath());
    }

    /**
     * @param url
     */
    @Override
    public boolean loadFile(String url) {
        ProgressMonitor pmon = null;

        if (showProgress) {
            pmon = new ProgressMonitor(parentWindow, I18n.text("Loading mission"),
                    I18n.text("Verifying against schema..."), 0, 100);
            pmon.setMillisToDecideToPopup(0);
        }

        String extension = url.substring(url.lastIndexOf('.') + 1, url.length());

        if (extension.equalsIgnoreCase("nmisz") || extension.equals("zip")) {
            String tmpDir = ConfigFetch.getNeptusTmpDir();
            File outDir = new File(tmpDir, "mission_" + getId());
            if (outDir.exists()) {
                FileUtil.deltree(outDir.getAbsolutePath());
            }
            outDir.mkdirs();

            outDir.deleteOnExit();

            NeptusLog.pub().debug("uncompressing mission to " + outDir);
            ZipUtils.unZip(url, outDir.getAbsolutePath());
            originalFilePath = new File(outDir, "mission.nmis").getAbsolutePath();
            compressedFilePath = url;
        }
        else {
            originalFilePath = new File(url).getAbsolutePath();
            compressedFilePath = null;
        }

        String fileAsString = FileUtil.getFileAsString(originalFilePath);

        return load(fileAsString);
    }

    @Override
    public boolean isLoadOk() {
        return isLoadOk;
    }

    public static boolean validate(Document doc) {
        try {
            String sLoc = new File(ConfigFetch.getMissionSchemaLocation()).getAbsoluteFile().toURI().toASCIIString();
            XMLValidator xmlVal = new XMLValidator(doc, sLoc);
            boolean ret = xmlVal.validate();
            return ret;
        }
        catch (Exception e) {
            NeptusLog.pub().error("MissionType:validate", e);
            GuiUtils.errorMessage(ConfigFetch.getSuperParentFrame(), e);
            return false;
        }
    }

    public static boolean validate(String xml) {
        try {
            String sLoc = new File(ConfigFetch.getMissionSchemaLocation()).getAbsoluteFile().toURI().toASCIIString();
            XMLValidator xmlVal = new XMLValidator(xml, sLoc);
            boolean ret = xmlVal.validate();
            return ret;
        }
        catch (Exception e) {
            NeptusLog.pub().error("MissionType:validate", e);
            GuiUtils.errorMessage(ConfigFetch.getSuperParentFrame(), e);
            return false;
        }
    }

    public static boolean validate(File file) {
        try {
            // System.err.println(file.getAbsoluteFile().toURI());
            String xml = FileUtil.getFileAsString(file);
            return validate(xml);
        }
        catch (Exception e) {
            NeptusLog.pub().error("MissionType:validate", e);
            GuiUtils.errorMessage(ConfigFetch.getSuperParentFrame(), e);
            return false;
        }
    }

    /**
     * @return Returns the id.
     */
    public String getId() {
        return id;
    }

    /**
     * @param id The id to set.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return Returns the name.
     */
    public String getName() {
        return name;
    }

    /**
     * @param name The name to set.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return Returns the type.
     */
    public String getType() {
        return type;
    }

    /**
     * @param type The type to set.
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @return Returns the description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description The description to set.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return Returns the mapsList.
     */
    public LinkedHashMap<String, MapMission> getMapsList() {
        return mapsList;
    }

    /**
     * @return Returns the checklistsList.
     */
    public LinkedHashMap<String, ChecklistMission> getChecklistsList() {
        return checklistsList;
    }

    /**
     * @param checklistsList The checklistsList to set.
     */
    public void setChecklistsList(LinkedHashMap<String, ChecklistMission> checklistsList) {
        this.checklistsList = checklistsList;
    }

    /**
     * @return Returns the vehiclesList.
     */
    public LinkedHashMap<String, VehicleMission> getVehiclesList() {
        return vehiclesList;
    }

    /**
     * @return Returns the notesList.
     */
    public LinkedList<?> getNotesList() {
        return notesList;
    }

    /**
     * @return Returns the homeRef.
     */
    public HomeReference getHomeRef() {
        homeRef.setMission(this);
        return homeRef;
    }

    /**
     * @param homeRef The homeRef to set.
     */
    public void setHomeRef(HomeReference homeRef) {
        this.homeRef = homeRef;
    }

    /**
     * @param homeRef The homeRef to set.
     */
    public void setHomeRef(CoordinateSystem cs) {
        this.homeRef.setLocation(cs);
        this.homeRef.setAnglesUsed(cs.isAnglesUsed());
        this.homeRef.setRoll(cs.getRoll());
        this.homeRef.setPitch(cs.getPitch());
        this.homeRef.setYaw(cs.getYaw());
        this.homeRef.setId("home");
    }

    /**
     * @return Returns the individualPlansList.
     */
    public TreeMap<String, PlanType> getIndividualPlansList() {
        return individualPlansList;
    }

    /**
     * @return Returns the originalFilePath.
     */
    public String getOriginalFilePath() {
        return originalFilePath;
    }

    /**
     * @param originalFilePath The originalFilePath to set.
     */
    public void setOriginalFilePath(String originalFilePath) {
        // FIXME Actualizar tb os filhos
        this.originalFilePath = originalFilePath;
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.types.XmlOutputMethods#asXML()
     */
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
     * @see pt.lsts.neptus.types.XmlOutputMethods#asElement(java.lang.String)
     */
    @Override
    public Element asElement(String rootElementName) {
        return (Element) asDocument(rootElementName).getRootElement().detach();
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

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.types.XmlOutputMethods#asDocument(java.lang.String)
     */
    @Override
    public Document asDocument(String rootElementName) {
        Document document = DocumentHelper.createDocument();
        Element root = document.addElement(rootElementName);

        root.addComment(ConfigFetch.getSaveAsCommentForXML());

        Element header = root.addElement("header");
        header.addElement("id").addText(getId());
        header.addElement("name").addText(getName());
        header.addElement("type").addText(getType());
        if (!description.equalsIgnoreCase(""))
            header.addElement("description").addText(getDescription());
        if (notesList.size() != 0) {
            Element notes = header.addElement("notes");
            for (Iterator<?> iter = notesList.iterator(); iter.hasNext();) {
                notes.addElement("note").addText((String) iter.next());
            }
        }

        Element definitions = root.addElement("definitions");
        Element vehicles = definitions.addElement("vehicles");
        for (Iterator<?> iter = vehiclesList.values().iterator(); iter.hasNext();) {
            VehicleMission vm = (VehicleMission) iter.next();
            Element ve = vehicles.addElement("vehicle");
            ve.addElement("id").addText(vm.getId());
            ve.addElement("name").addText(vm.getName());
            CoordinateSystem vcs = vm.getCoordinateSystem();
            if (vcs != null)
                if (!"home".equals(vcs.getId()))
                    ve.add(vcs.asElement("vehicle-reference"));
        }

        Element maps = definitions.addElement("maps");
        for (Iterator<?> iter = mapsList.values().iterator(); iter.hasNext();) {
            MapMission mm = (MapMission) iter.next();
            Element ma = maps.addElement("map");
            ma.addElement("id").addText(mm.getId());
            ma.addElement("name").addText(mm.getName());
            if ("".equals(originalFilePath))
                ma.addElement("href").addText(mm.getHref());
            else
                ma.addElement("href").addText(FileUtil.relativizeFilePathAsURI(originalFilePath, mm.getHref()));
        }

        Element checklists = definitions.addElement("checklists");
        for (Iterator<?> iter = checklistsList.values().iterator(); iter.hasNext();) {
            ChecklistMission cm = (ChecklistMission) iter.next();
            Element cl = checklists.addElement("checklist");
            cl.addElement("id").addText(cm.getId());
            cl.addElement("name").addText(cm.getName());
            if ("".equals(originalFilePath))
                cl.addElement("href").addText(cm.getHref());
            else
                cl.addElement("href").addText(FileUtil.relativizeFilePathAsURI(originalFilePath, cm.getHref()));
        }

        definitions.add(homeRef.asElement("home-reference"));

        Element body = root.addElement("body");
        for (Iterator<?> iter = individualPlansList.values().iterator(); iter.hasNext();) {
            PlanType indp = (PlanType) iter.next();
            body.add(indp.asElement());
        }

        return document;
    }

    public void addVehicle(VehicleMission vm) {
        vehiclesList.put(vm.getId(), vm);
    }

    public void addMap(MapMission map) {
        mapsList.put(map.getId(), map);
    }

    /**
     * @param ipt be sure to add this mission to the plan otherwise this plan will belong to 2 missions!!
     */
    public void addPlan(PlanType ipt) {
        individualPlansList.put(ipt.getId(), ipt);
    }

    public void setMapsList(LinkedHashMap<String, MapMission> mapsList) {
        this.mapsList = mapsList;
    }

    public void setPlanList(TreeMap<String, PlanType> planList) {
        this.individualPlansList = planList;
    }

    public void setVehiclesList(LinkedHashMap<String, VehicleMission> vehiclesList) {
        this.vehiclesList = vehiclesList;
    }

    /**
     * @param filename The zip filename where to save the mission
     */
    public boolean asZipFile(String filename, boolean saveNeptusConfigurations) {
        String sep = System.getProperty("file.separator");
        String tmpDir = ConfigFetch.getNeptusTmpDir(); // System.getProperty("java.io.tmpdir");
        File outDir = new File(tmpDir, "tmpMission");
        if (outDir.exists()) {
            FileUtil.deltree(outDir.getAbsolutePath());
        }
        outDir.mkdirs();

        outDir.deleteOnExit();

        File mapsDir = new File(outDir, "maps");
        mapsDir.mkdirs();

        File imgsDir = new File(outDir, "images");
        imgsDir.mkdirs();

        File modelsDir = new File(outDir, "models");
        modelsDir.mkdirs();

        File chkDir = new File(outDir, "checklists");
        chkDir.mkdirs();

        Document myDoc = asDocument();
        // NeptusLog.pub().info("<###>Mission Zip +++++++++++++++++++++++ " + myDoc.asXML());
        if (originalFilePath == null || originalFilePath.length() == 0) {
            GuiUtils.errorMessage(null, I18n.text("Mission not saved"), I18n.text("The mission has to be saved first"));
            return false;
        }
        String oldPath = compressedFilePath;
        compressedFilePath = null;
        save(false);
        compressedFilePath = oldPath;

        String missionDir = originalFilePath.substring(0, originalFilePath.lastIndexOf(sep));
        // NeptusLog.pub().info("<###>missiondir="+missionDir);
        // NeptusLog.pub().info("<###>outdir="+outDir.getAbsolutePath());

        List<?> lst = myDoc.selectNodes("//href");
        Iterator<?> lstIt = lst.iterator();
        while (lstIt.hasNext()) {
            Node n = (Node) lstIt.next();
            String oldHref = ConfigFetch.resolvePathWithParent(missionDir, n.getText());
            String newHref = oldHref.substring(oldHref.lastIndexOf(sep) + 1, oldHref.length());
            if (n.getPath().contains("/map/")) {
                newHref = "maps/" + newHref;
            }
            if (n.getPath().contains("/checklist/")) {
                newHref = "checklists/" + newHref;
            }
            n.setText(newHref);
        }

        for (MapMission map : mapsList.values()) {

            String mapHref = map.getHref();
            mapHref = ConfigFetch.resolvePathWithParent(missionDir, mapHref);
            String mapDir = mapHref.substring(0, mapHref.lastIndexOf(sep));

            Document mapDoc = map.getMap().asDocument();
            List<?> l = mapDoc.selectNodes("//map/features/image/href");
            Iterator<?> l2 = l.iterator();
            while (l2.hasNext()) {
                Node n = (Node) l2.next();
                String imgHref = ConfigFetch.resolvePathWithParent(mapDir, n.getText());
                String imgName = imgHref.substring(imgHref.lastIndexOf(sep) + 1, imgHref.length());
                // NeptusLog.pub().info("<###>IMAGE> "+imgHref+ " -> " + outDir.getAbsolutePath()+sep+imgName);
                FileUtil.copyFile(imgHref, outDir.getAbsolutePath() + sep + "images" + sep + imgName);
                n.setText("../images/" + imgName);
            }
            l = mapDoc.selectNodes("//map/features/image/bathymetryImage");
            l2 = l.iterator();
            while (l2.hasNext()) {
                Node n = (Node) l2.next();
                String imgHref = ConfigFetch.resolvePathWithParent(mapDir, n.getText());
                String imgName = imgHref.substring(imgHref.lastIndexOf(sep) + 1, imgHref.length());
                // NeptusLog.pub().info("<###>BATHIMAGE> "+imgHref+ " -> " + outDir.getAbsolutePath()+sep+imgName);
                FileUtil.copyFile(imgHref, outDir.getAbsolutePath() + sep + "images" + sep + imgName);
                n.setText("../images/" + imgName);
            }
            l = mapDoc.selectNodes("//map/features/image/href-altitude");
            l2 = l.iterator();
            while (l2.hasNext()) {
                Node n = (Node) l2.next();
                String imgHref = ConfigFetch.resolvePathWithParent(mapDir, n.getText());
                String imgName = imgHref.substring(imgHref.lastIndexOf(sep) + 1, imgHref.length());
                // NeptusLog.pub().info("<###>BATHIMAGE> "+imgHref+ " -> " + outDir.getAbsolutePath()+sep+imgName);
                FileUtil.copyFile(imgHref, outDir.getAbsolutePath() + sep + "images" + sep + imgName);
                n.setText("../images/" + imgName);
            }

            l = mapDoc.selectNodes("//map/features/model3d/href");
            l2 = l.iterator();
            while (l2.hasNext()) {
                Node n = (Node) l2.next();
                String imgHref = ConfigFetch.resolvePathWithParent(mapDir, n.getText());
                String imgName = imgHref.substring(imgHref.lastIndexOf(sep) + 1, imgHref.length());
                // NeptusLog.pub().info("<###>IMAGE> "+imgHref+ " -> " + outDir.getAbsolutePath()+sep+imgName);
                FileUtil.copyFile(imgHref, outDir.getAbsolutePath() + sep + "models" + sep + imgName);
                n.setText("../models/" + imgName);

                // Copy also textures of the model in a file with the same name with added ".textures"
                String imgHrefTex = imgHref + ".textures";
                String imgNameTex = imgName + ".textures";
                File fxTex = new File(imgHrefTex);
                if (fxTex.exists()) {
                    FileUtil.copyFile(imgHrefTex, outDir.getAbsolutePath() + sep + "models" + sep + imgNameTex);
                    String strFx = FileUtil.getFileAsString(fxTex);
                    StringTokenizer strTok = new StringTokenizer(strFx);
                    while (strTok.hasMoreTokens()) {
                        String tFx = strTok.nextToken();
                        String tFxAbs = ConfigFetch.resolvePathWithParent(imgHrefTex, tFx);
                        FileUtil.copyFile(tFxAbs, outDir.getAbsolutePath() + sep + "models" + sep + tFx);
                    }
                }
            }
            l = mapDoc.selectNodes("//map/features/model3d/href-2d");
            l2 = l.iterator();
            while (l2.hasNext()) {
                Node n = (Node) l2.next();
                String imgHref = ConfigFetch.resolvePathWithParent(mapDir, n.getText());
                String imgName = imgHref.substring(imgHref.lastIndexOf(sep) + 1, imgHref.length());
                // NeptusLog.pub().info("<###>IMAGE> "+imgHref+ " -> " + outDir.getAbsolutePath()+sep+imgName);
                FileUtil.copyFile(imgHref, outDir.getAbsolutePath() + sep + "models" + sep + imgName);
                n.setText("../models/" + imgName);
            }

            String mapName = mapHref.substring(mapHref.lastIndexOf(sep), mapHref.length());
            // NeptusLog.pub().info("<###>MAP> "+mapHref+ " -> " + outDir+sep+mapName);
            // FileUtil.saveToFile(outDir+sep+mapName, mapDoc.asXML());
            FileUtil.saveToFile(outDir + sep + "maps" + sep + mapName,
                    FileUtil.getAsPrettyPrintFormatedXMLString(mapDoc));

        }

        for (ChecklistMission clist : checklistsList.values()) {
            String href = clist.getHref();
            href = ConfigFetch.resolvePathWithParent(missionDir, href);
            String clName = href.substring(href.lastIndexOf(sep), href.length());
            FileUtil.saveToFile(outDir + sep + "checklists" + sep + clName,
                    FileUtil.getAsPrettyPrintFormatedXMLString(clist.getChecklist().asDocument()));
        }

        FileUtil.saveToFile(outDir.getAbsolutePath() + sep + "mission.nmis",
                FileUtil.getAsPrettyPrintFormatedXMLString(FileUtil.getAsCompactFormatedXMLString(myDoc)));

        if (saveNeptusConfigurations) {
            FileUtil.copyFile(ConfigFetch.resolvePath("conf/general-properties.xml"), outDir + sep
                    + "general-properties.xml");

            FileUtil.saveToFile(outDir + sep + "IMC.xml", ImcStringDefs.getDefinitions());
        }

        try {
            ZipUtils.zipDir(filename, outDir.getAbsolutePath());
        }
        catch (Exception e) {
            NeptusLog.pub().error(e);
            return false;
        }
        return true;
    }

    public MapGroup generateMapGroup() {

        MapGroup mg = MapGroup.getNewInstance(getHomeRef());

        Object[] maps = getMapsList().values().toArray();

        for (int i = 0; i < maps.length; i++) {
            MapMission mm = (MapMission) maps[i];
            MapType mt = mm.getMap();
            mt.setHref(mm.getHref());
            mt.setChanged(false);
            mt.setMission(this);
            mt.setMapGroup(mg);
            mg.addMap(mt);
        }

        return mg;
    }

    public synchronized boolean save(boolean savePreviousState) {
        long startTimeMillis = System.currentTimeMillis();

        String caller = ReflectionUtil.getCallerStamp();
        
        if (getOriginalFilePath() == null || getOriginalFilePath().equals("")) {
            if (compressedFilePath != null) {
                File fxTmp = new File(compressedFilePath);
                if (!fxTmp.exists())
                    try {
                        fxTmp.createNewFile();
                    }
                    catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                if (!fxTmp.exists()) {
                    GuiUtils.errorMessage(ConfigFetch.getSuperParentFrame(), I18n.text("Save mission"),
                            I18n.text("Cannot save mission: file name is non existant"));
                    NeptusLog.pub().error("Cannot save mission file: compressedFilePath is NULL");
                    return false;
                }
                setOriginalFilePath(fxTmp.getParentFile().getAbsolutePath() + "/tmp-mission.nmis");
            }
            else {
                GuiUtils.errorMessage(ConfigFetch.getSuperParentFrame(), I18n.text("Save mission"),
                        I18n.text("Cannot save mission: file name is empty"));
                NeptusLog.pub().error("Cannot save mission file: OriginalFilePath is NULL");
                return false;
            }
        }
      
        if (savePreviousState) {
            String missionlog = GuiUtils.getLogFileName("mission_state", "zip");
            // Thread t = new Thread(new Runnable() {
            // public void run() {
            // asZipFile(missionlog, true);
            // NeptusLog.pub().info("The current mission state was saved on "+missionlog);
            // }
            // });
            // t.start();
            boolean sr = asZipFile(missionlog, true);
            NeptusLog.pub().debug("The current mission state was" + (sr ? "" : " NOT") + " saved on " + missionlog
                    + " in " + getFormatedDuration(startTimeMillis) + " from " + caller);
        }

        if (compressedFilePath != null) {
            boolean sr = asZipFile(compressedFilePath, false);
            NeptusLog.pub().info("The mission was" + (sr ? "" : " NOT") + " saved to " + compressedFilePath
                    + " in " + getFormatedDuration(startTimeMillis) + " from " + caller);            
            return sr;
        }

        for (MapMission map : mapsList.values()) {
            String maphref = map.getMap().getHref();
            if (maphref == null) {
                String misFx = new File(getOriginalFilePath()).getParentFile().getPath();
                maphref = misFx + "/" + map.getMap().getName();
            }
            map.getMap().saveFile(maphref);
        }
        for (ChecklistMission clist : checklistsList.values()) {
            String href = clist.getHref();
            FileUtil.saveToFile(href,
                    FileUtil.getAsPrettyPrintFormatedXMLString(clist.getChecklist().asDocument()));
        }

        boolean sr = FileUtil.saveToFile(getOriginalFilePath(),
                FileUtil.getAsPrettyPrintFormatedXMLString(asDocument()));
        NeptusLog.pub().info("The mission '" + getId() + "' was" + (sr ? "" : " NOT") + " saved to '" + getOriginalFilePath() + "'"
                        + " in " + getFormatedDuration(startTimeMillis) + " from " + caller);
        return sr;
    }

    private String getFormatedDuration(long startTimeMillis) {
        return DateTimeUtil.milliSecondsToFormatedString(System.currentTimeMillis() - startTimeMillis);
    }
    
    public File getMissionFile() {
        if (compressedFilePath != null)
            return new File(compressedFilePath);
        else
            return new File(originalFilePath);

    }

    public void setMissionFile(File fileToSave) {
        String ext = FileUtil.getFileExtension(fileToSave);
        if (ext.equalsIgnoreCase(FileUtil.FILE_TYPE_MISSION_COMPRESSED) || ext.equalsIgnoreCase("zip")) {
            compressedFilePath = fileToSave.getAbsolutePath();
        }
        else {
            originalFilePath = fileToSave.getAbsolutePath();
            compressedFilePath = null;
        }

        // if (compressedFilePath != null)
        // compressedFilePath = fileToSave.getAbsolutePath();
        // else
        // originalFilePath = fileToSave.getAbsolutePath();
    }

    public LocationType getStartLocation() {
        return IMCUtils.lookForStartPosition(this);
    }

    public String getCompressedFilePath() {
        return compressedFilePath;
    }

    public void setCompressedFilePath(String compressedFilePath) {
        this.compressedFilePath = compressedFilePath;
    }

    /**
     * @param plan The plan to be renamed.
     * @param newName New name.
     * @param override Forces override if a plan with newName exists. 
     * @return Return if the rename was made. 
     */
    public boolean renamePlan(PlanType plan, String newName, boolean override) {
        if (!override && individualPlansList.containsKey(newName))
            return false;
        
        individualPlansList.remove(plan.getId());
        plan.setId(newName);
        individualPlansList.put(newName, plan);
        return true;
    }
}