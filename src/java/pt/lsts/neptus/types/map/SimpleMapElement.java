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
 * Author: José Pinto
 * Nov 17, 2011
 */
package pt.lsts.neptus.types.map;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import com.l2fprod.common.propertysheet.Property;

import pt.lsts.neptus.gui.objparams.CustomParametersPanel;
import pt.lsts.neptus.gui.objparams.ParametersPanel;
import pt.lsts.neptus.plugins.PluginProperty;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * @author zp
 * 
 */
public class SimpleMapElement extends AbstractElement {

    public SimpleMapElement(MapGroup mg, MapType map) {
        super(mg, map);
        //id = id.replaceFirst("me", getType());
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

        g.drawString(getId(), 6, 6);
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
