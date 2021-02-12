/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
 * 15/Jan/2005
 */
package pt.lsts.neptus.types.map;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Hashtable;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import pt.lsts.imc.LblBeacon;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.gui.objparams.ParametersPanel;
import pt.lsts.neptus.gui.objparams.TransponderParameters;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.NameId;
import pt.lsts.neptus.types.coord.CoordinateSystem;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.misc.FileType;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.PropertiesLoader;
import pt.lsts.neptus.util.conf.ConfigFetch;

/**
 * Refactored in 06/11/2006.
 * 
 * @author Paulo Dias
 * @author Ze Carlos
 * @author Margarida Faria
 */
public class TransponderElement extends AbstractElement implements NameId{
    protected static final String DEFAULT_ROOT_ELEMENT = "transponder";
    private static Image transponderImg = ImageUtils.getImage("images/transponder.png");
//    private static String[] transpondersListArray;
    // id shared with Dune - order in LblConfig list
    public short duneId;

//    static {
//        final Vector<String> aTranspondersFiles = new Vector<String>();
//        File dir = new File("maps/");
//        File[] files = dir.listFiles(new FileFilter() {
//            @Override
//            public boolean accept(File pathname) {
//                String name = pathname.getName();
//                // NeptusLog.pub().info("<###> "+name + ": " +
//                // name.matches("^(lsts[0-9]+\\.conf)|([A-Za-z][A-Za-z\\-_0-9]+\\.conf)$"));
//                if (name.matches("^(lsts[0-9]+\\.conf)|([A-Za-z][A-Za-z0-9\\-\\_]*\\.conf)$")) {
//                    return true;
//                }
//                return false;
//            }
//        });
//        Arrays.sort(files, new Comparator<File>() {
//            @Override
//            public int compare(File o1, File o2) {
//                if (o1.getName().startsWith("lsts") && !o2.getName().startsWith("lsts"))
//                    return -1;
//                else if (!o1.getName().startsWith("lsts") && o2.getName().startsWith("lsts"))
//                    return 1;
//                return o1.compareTo(o2);
//            }
//        });
//        for (File file : files) {
//            // NeptusLog.pub().info("<###> "+file.getName());
//            aTranspondersFiles.add(file.getName());
//        }
//        transpondersListArray = aTranspondersFiles.toArray(new String[aTranspondersFiles.size()]);
//    }

    protected FileType file = null;
    protected boolean buoyAttached = true;

    private TransponderParameters params = null;

    /**
     * The config file format is like the following:
     * 
     * <pre>
     * transponder delay (msecs.)=49.0
     * Responder lockout (secs)=1.0
     * interrogation channel=4
     * reply channel=3
     * </pre>
     * 
     * And is located maps directory acording to the file name indicated. Is load by calling {@link #setFile(FileType)}
     */
    private PropertiesLoader propConf = null;

    /**
     * 
     */
    public TransponderElement() {
        super();
        duneId = -1;
    }

    /**
     * @param xml
     */
    public TransponderElement(String xml) {
        // super(xml);
        load(xml);
        duneId = -1;
    }

    public TransponderElement(Element elem) {
        // super(xml);
        load(elem);
        duneId = -1;
    }

    public TransponderElement(MapGroup mg, MapType parentMap) {
        super(mg, parentMap);
        if (mg != null)
            setCenterLocation(new LocationType(mg.getHomeRef().getCenterLocation()));
        duneId = -1;
    }

    /**
     * Creates a TransponderElement with the values on the beacon.
     * 
     * @param lblBeacon
     * @param duneId
     * @param mg
     * @param parentMap
     */
    public TransponderElement(LblBeacon lblBeacon, short duneId, MapGroup mg, MapType parentMap) {
        super(mg, parentMap);
        String beacon = lblBeacon.getBeacon();
        double lat = Math.toDegrees(lblBeacon.getLat());
        double lon = Math.toDegrees(lblBeacon.getLon());
        double depth = lblBeacon.getDepth();
        LocationType lt = new LocationType();
        lt.setLatitudeDegs(lat);
        lt.setLongitudeDegs(lon);
        lt.setDepth(depth);
        id = beacon;
        setCenterLocation(lt);
        propConf = TransponderUtils.getMatchingConf(lblBeacon);
        file = new FileType();
        String workingFile = propConf.getWorkingFile();
        String[] tokens = workingFile.split("/");
        NeptusLog.pub().debug("Beacon conf file:" + tokens[tokens.length - 1]);
        file.setHref(tokens[tokens.length - 1]);
        this.duneId = duneId;
    }

    private TransponderElement(short duneId, MapGroup mg, MapType parentMap, LocationType centerLocation,
            String identification, PropertiesLoader propConf) {
        super(mg, parentMap);
        this.centerLocation = centerLocation;
        this.id = identification;
        this.id = identification;
        this.propConf = propConf;
        file = new FileType();
        file.setHref(propConf.getWorkingFile());
        this.duneId = duneId;
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.types.map.AbstractElement#showParametersDialog(java.awt.Component, java.lang.String[], pt.lsts.neptus.types.map.MapType, boolean)
     */
    @Override
    public void showParametersDialog(Component parentComp, String[] takenNames, MapType map, boolean editable) {
        super.showParametersDialog(parentComp, takenNames, map, editable, false);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final TransponderElement externalTrans = (TransponderElement) obj;
        // Location
        if (!getCenterLocation().equals(externalTrans.getCenterLocation())) {
            return false;
        }
        // Name
        if (!getId().equals(externalTrans.getId())) {
            return false;
        }
        // Dune id
        if (duneId != externalTrans.duneId)
            return false;
        return true;
    }

    /**
     * Compare contents (interrogation channel, querry channel, transponder delay, lat, lon, depth and name) of this
     * beacon excluding the id field.
     * 
     * @param lblBeacon
     * @return true if all are equal false otherwise
     */
    public boolean equals(LblBeacon lblBeacon) {
        // Location
        LocationType lt = new LocationType();
        lt.setLatitudeDegs(Math.toDegrees(lblBeacon.getDouble("lat")));
        lt.setLongitudeDegs(Math.toDegrees(lblBeacon.getDouble("lon")));
        lt.setDepth(lblBeacon.getDouble("depth"));
        if(!getCenterLocation().equals(lt)){
            // System.out.print(lblBeacon.getBeacon() + " has different location that " + getIdentification());
            return false;
        }
        // Name
        String beaconName = lblBeacon.getString("beacon");
        if (!getId().equals(beaconName)) {
            // System.out.print(lblBeacon.getBeacon() + " has different name that " + getIdentification());
            return false;
        }

        // All is equal
        return true;
    }

    @Override
    public String getType() {
        return "Transponder";
    }

    @Override
    public boolean load(Element elem) {
        if (!super.load(elem))
            return false;

        try {
            // doc = DocumentHelper.parseText(xml);
            Node nd = doc.selectSingleNode("//file");
            if (nd != null)
                this.setFile(new FileType(nd.asXML()));
            nd = doc.selectSingleNode("//buoy-attached");
            if (nd != null)
                this.setBuoyAttached(new Boolean(nd.getText()).booleanValue());
        }
        catch (Exception e) {
            NeptusLog.pub().error(this, e);
            isLoadOk = false;
            return false;
        }
        isLoadOk = true;
        return true;
    }

    /**
     * @return Returns the file.
     */
    public FileType getFile() {
        return file;
    }

    /**
     * @param file The file to set.
     */
    public void setFile(FileType file) {
        this.file = file;

        try {
            propConf = new PropertiesLoader(ConfigFetch.resolvePath("maps/" + file.getHref()),
                    PropertiesLoader.PROPERTIES);
            fixPropertiesConfFormat();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * To fix the fact that the property value can not have a space in the key.
     */
    private void fixPropertiesConfFormat() {
        Hashtable<String, String> fixedValues = new Hashtable<String, String>();
        for (Object keyO : propConf.keySet()) {
            String key = (String) keyO;
            String value = propConf.getProperty(key);
            String[] vs = value.split("=");
            if (vs.length > 1) {
                key = key + " " + vs[0];
                value = vs[1];
            }
            fixedValues.put(key, value);
        }
        propConf.clear();
        propConf.putAll(fixedValues);
    }

    public String getConfiguration() {
        if (getFile() == null)
            return null;

        return getFile().getHref();
    }

    public void setConfiguration(String configuration) {
        if (getFile() == null) {
            setFile(new FileType());
        }

        this.getFile().setHref(configuration);
        setFile(getFile());
    }

    /**
     * @return the propConf
     */
    public PropertiesLoader getPropConf() {
        return propConf;
    }

    /**
     * @return Returns the buoyAttached.
     */
    public boolean isBuoyAttached() {
        return buoyAttached;
    }

    /**
     * @param buoyAttached The buoyAttached to set.
     */
    public void setBuoyAttached(boolean buoyAttached) {
        this.buoyAttached = buoyAttached;
    }

    @Override
    public String asXML() {
        String rootElementName = DEFAULT_ROOT_ELEMENT;
        return asXML(rootElementName);
    }

    @Override
    public String asXML(String rootElementName) {
        String result = "";
        Document document = asDocument(rootElementName);
        result = document.asXML();
        return result;
    }


    @Override
    public Element asElement() {
        String rootElementName = DEFAULT_ROOT_ELEMENT;
        return asElement(rootElementName);
    }


    @Override
    public Element asElement(String rootElementName) {
        return (Element) asDocument(rootElementName).getRootElement().detach();
    }


    @Override
    public Document asDocument() {
        String rootElementName = DEFAULT_ROOT_ELEMENT;
        return asDocument(rootElementName);
    }

    @Override
    public Document asDocument(String rootElementName) {
        Document document = DocumentHelper.createDocument();
        Element root = (Element) super.asDocument(DEFAULT_ROOT_ELEMENT).getRootElement().detach();
        document.add(root);

        root.add(getFile().asElement());
        root.addElement("buoy-attached").addText(new Boolean(isBuoyAttached()).toString());

        return document;
    }

    @Override
    public boolean containsPoint(LocationType lt, StateRenderer2D renderer) {

        double distance = getCenterLocation().getHorizontalDistanceInMeters(lt);
        
        if (renderer == null)
            return distance < 5;
        else
            return (distance * renderer.getZoom()) < 10;
    }

    @Override
    public int getLayerPriority() {
        return 9;
    }

    @Override
    public ParametersPanel getParametersPanel(boolean editable, MapType map) {

        if (params == null)
            params = new TransponderParameters(new CoordinateSystem(), objName);
        params.setIdEditor(objName);
        params.setMap(map != null ? map : getParentMap());
        params.setLocation(getCenterLocation());
        params.setConfiguration(getConfiguration());
        params.setHomeRef(getMapGroup().getCoordinateSystem());
        params.setEditable(editable);
        
        return params;
    }

    @Override
    public void initialize(ParametersPanel paramsPanel) {
        setCenterLocation(params.getLocationPanel().getLocationType());
        setConfiguration(params.getConfiguration());
    }

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer, double rotation) {
        Point2D tt = renderer.getScreenPosition(getCenterLocation());
        g.translate(tt.getX(), tt.getY());

        if (isSelected()) {
            g.setColor(Color.YELLOW);
            g.draw(new Rectangle2D.Double(-transponderImg.getWidth(renderer) / 2,
                    -transponderImg.getHeight(renderer) / 2, transponderImg.getWidth(null), transponderImg
                            .getHeight(null)));
        }

        g.drawImage(transponderImg, -transponderImg.getWidth(renderer) / 2, -transponderImg.getHeight(renderer) / 2,
                transponderImg.getWidth(null), transponderImg.getHeight(null), null);

        
        String text = getId();
        Rectangle2D stringBounds = g.getFontMetrics().getStringBounds(text, g);
        int x = (int) (-stringBounds.getWidth() / 2), y = (int) (stringBounds.getHeight() + 5);
        GuiUtils.drawText(text, x, y, Color.WHITE, Color.BLACK, g);
//        g.drawString(getId(), 7, 16);
    }

    @Override
    public ELEMENT_TYPE getElementType() {
        return ELEMENT_TYPE.TYPE_TRANSPONDER;
    }

//    public static final String[] getTranspondersListArray() {
//        return transpondersListArray;
//    }

    @Override
    public String getDisplayName() {
        return id;
    }

    @Override
    public String getIdentification() {
        return id;
    }

    public byte[] getMd5() {
        LblBeacon beacon = TransponderUtils.getTransponderAsLblBeaconMessage(this);
        return beacon.payloadMD5();
    }

    @Override
    public String toString() {
        String queryCh = propConf != null ? propConf.getProperty("interrogation channel") : "";
        String replyCh = propConf != null ? propConf.getProperty("reply channel") : "";
        String delay = propConf != null ? propConf.getProperty("transponder delay (msecs.)") : "";
        StringBuilder string = new StringBuilder();
        string.append(getDisplayName());
        string.append("[");
        string.append(getIdentification());
        string.append("]");
        string.append(getDisplayName());
        string.append(" ( query: ");
        string.append(queryCh);
        string.append(", reply: ");
        string.append(replyCh);
        string.append(", delay:");
        string.append(delay);
        string.append(")");
        return string.toString();
    }

    /**
     * 
     * @param id
     */
    public void setDuneId(short id) {
        duneId = id;

    }
    
    @Override
    public String getTypeAbbrev() {
        return "trans";
    }

    @Override
    public TransponderElement clone() {
        return new TransponderElement(duneId, getMapGroup(), getParentMap(), new LocationType(centerLocation),
                getIdentification(), propConf);
    }



}
