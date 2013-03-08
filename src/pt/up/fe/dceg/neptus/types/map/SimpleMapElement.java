/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by zp
 * Nov 17, 2011
 * $Id:: SimpleMapElement.java 9845 2013-02-01 19:53:46Z pdias                  $:
 */
package pt.up.fe.dceg.neptus.types.map;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import pt.up.fe.dceg.neptus.gui.objparams.CustomParametersPanel;
import pt.up.fe.dceg.neptus.gui.objparams.ParametersPanel;
import pt.up.fe.dceg.neptus.plugins.PluginProperty;
import pt.up.fe.dceg.neptus.plugins.PluginUtils;
import pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D;
import pt.up.fe.dceg.neptus.types.coord.LocationType;

import com.l2fprod.common.propertysheet.Property;

/**
 * @author zp
 * 
 */
public class SimpleMapElement extends AbstractElement {

    public SimpleMapElement(MapGroup mg, MapType map) {
        super(mg, map);
        id = id.replaceFirst("obj", getType());
        name = id;
    }

    public SimpleMapElement() {
        super();
    }

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer, double rotation) {
        Point2D tt = renderer.getScreenPosition(getCenterLocation());

        g.translate(tt.getX(), tt.getY());

        g.setColor(Color.BLACK);
        g.drawLine(-3, 0, 3, 0);
        g.drawLine(0, -3, 0, 3);

        g.drawString(getName(), 6, 6);
    }

    @Override
    public boolean load(Element elem) {
        if (!super.load(elem))
            return false;

        PluginProperty[] props = PluginUtils.getPluginProperties(this);

        for (int i = 0; i < props.length; i++) {
            String name = "//" + props[i].getName().replaceAll(" ", "");
            Node nd = doc.selectSingleNode(name);

            if (nd != null) {
                String pValue = nd.getText();
                try {
                    props[i].unserialize(pValue);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        PluginUtils.setPluginProperties(this, props);
        return true;
    }

    @Override
    public ParametersPanel getParametersPanel(boolean editable, MapType map) {
        return new CustomParametersPanel(PluginUtils.getPluginProperties(this)) {
            private static final long serialVersionUID = 1L;

            @Override
            public String getErrors() {
                String[] errors = PluginUtils.validatePluginProperties(SimpleMapElement.this, psp.getProperties());

                if (errors == null || errors.length == 0)
                    return null;

                String ret = "";
                for (String err : errors)
                    ret += err + "\n";

                return ret;
            }
        };

    }

    @Override
    public void initialize(ParametersPanel paramsPanel) {
        Property[] props = ((CustomParametersPanel) paramsPanel).getProperties();
        PluginUtils.setPluginProperties(this, props);
    }

    @Override
    public boolean containsPoint(LocationType point, StateRenderer2D renderer) {
        return false;
    }

    @Override
    public ELEMENT_TYPE getElementType() {
        return ELEMENT_TYPE.TYPE_OTHER;
    }

    @Override
    public int getLayerPriority() {
        return 0;
    }

    @Override
    public String getType() {
        return getClass().getSimpleName();
    }

    @Override
    public String asXML() {
        String rootElementName = getType();
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
        String rootElementName = getType();
        return asElement(rootElementName);
    }

    @Override
    public Element asElement(String rootElementName) {
        return (Element) asDocument(rootElementName).getRootElement().detach();
    }

    @Override
    public Document asDocument() {
        String rootElementName = getType();
        return asDocument(rootElementName);
    }

    @Override
    public Document asDocument(String rootElementName) {
        Document document = DocumentHelper.createDocument();

        Element root = (Element) super.asDocument(getType()).getRootElement().detach();
        document.add(root);

        PluginProperty[] props = PluginUtils.getPluginProperties(this);

        for (int i = 0; i < props.length; i++) {
            String name = props[i].getName().replaceAll(" ", "");
            root.addElement(name).addText(props[i].serialize());
        }

        return document;
    }
}
