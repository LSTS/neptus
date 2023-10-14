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
 * Author: 
 * 15/Jan/2005
 */
package pt.lsts.neptus.types.map;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;

import pt.lsts.neptus.gui.objparams.MarkParameters;
import pt.lsts.neptus.gui.objparams.ParametersPanel;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.GuiUtils;

/**
 * Refactored in 06/11/2006.
 * 
 * @author Paulo Dias
 * @author Ze Carlos
 */
public class MarkElement extends AbstractElement {
    MarkParameters params = null;

    public MarkElement() {
        super();
    }

    /**
     * @param xml
     */
    public MarkElement(String xml) {
        super(xml);
    }

    public MarkElement(MapGroup mg, MapType map) {
        super(mg, map);
        if (mg != null)
            setCenterLocation(new LocationType(mg.getHomeRef().getCenterLocation()));
    }

    @Override
    public String getType() {
        return "Mark";
    }

    @Override
    public int getLayerPriority() {
        return 10;
    }

    public LocationType getPosition() {
        return getCenterLocation();
    }

    public void translate(double offsetNorth, double offsetEast, double offsetDown) {
        getCenterLocation().translatePosition(offsetNorth, offsetEast, offsetDown);
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
    public ParametersPanel getParametersPanel(boolean editable, MapType map) {

        if (params == null)
            params = new MarkParameters();

        params.setLocation(getCenterLocation());
        params.setEditable(editable);

        // NeptusLog.pub().info("<###> "+map.numObjects());
        return params;
    }

    @Override
    public void initialize(ParametersPanel paramsPanel) {
        if (params == null)
            params = new MarkParameters();
        setCenterLocation(params.getLocationPanel().getLocationType());
    }

    @Override
    public ELEMENT_TYPE getElementType() {
        return ELEMENT_TYPE.TYPE_MARK;
    }
    
    @Override
    public String getTypeAbbrev() {
        return "mark";
    }

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer, double rotation) {
        Point2D tt = renderer.getScreenPosition(getCenterLocation());

        g.translate(tt.getX(), tt.getY());

        if (!isSelected())
            g.setColor(new Color(255, 0, 0, 100));
        else
            g.setColor(Color.WHITE);

        g.drawOval(-5, -5, 10, 10);

        Color fgColor;
        if (!isSelected())
            fgColor = Color.BLACK;
        else
            fgColor = Color.RED;

        g.setColor(fgColor);

        g.drawLine(-3, -3, 3, 3);
        g.drawLine(-3, 3, 3, -3);

        String text = getId();
        GuiUtils.drawText(text, 6, 6, Color.WHITE, fgColor, g);
    }
}
