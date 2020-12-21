/*
 * Copyright (c) 2004-2017 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: pdias
 * 08/05/2017
 */
package pt.lsts.neptus.mp.element;

import java.awt.Graphics2D;

import pt.lsts.neptus.mp.OperationLimits;
import pt.lsts.neptus.mp.interactions.OperationLimitsInteraction;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * @author pdias
 *
 */
@PluginDescription
public class OperationLimitsPlanElement implements IPlanElement<OperationLimits>, Renderer2DPainter {

    private OperationLimits limits = new OperationLimits();
    
    public OperationLimitsPlanElement() {
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.mp.element.IPlanElement#getName()
     */
    @Override
    public String getName() {
        return getHoldingTypeName();
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.mp.element.IPlanElement#getHoldingTypeName()
     */
    @Override
    public String getHoldingTypeName() {
        return getHoldingType().getSimpleName();
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.mp.element.IPlanElement#getHoldingType()
     */
    @Override
    public Class<OperationLimits> getHoldingType() {
        return OperationLimits.class;
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.mp.element.IPlanElement#getElement()
     */
    @Override
    public OperationLimits getElement() {
        return limits;
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.mp.element.IPlanElement#setElement(java.lang.Object)
     */
    @Override
    public void setElement(Object element) {
        if (element instanceof OperationLimits)
            this.limits = (OperationLimits) element;
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.mp.element.IPlanElement#getElementAsXml()
     */
    @Override
    public String getElementAsXml() {
        return limits == null ? "" : limits.asXml();
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.mp.element.IPlanElement#loadElementXml(java.lang.String)
     */
    @Override
    public OperationLimits loadElementXml(String xml) {
        OperationLimits elm = OperationLimits.loadXml(xml);
        if (elm != null)
            setElement(elm);
        return elm;
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.mp.element.IPlanElement#getEditor()
     */
    @Override
    public IPlanElementEditorInteraction<OperationLimits> getEditor() {
        return new OperationLimitsInteraction(getElement());
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.mp.element.IPlanElement#getPainter()
     */
    @Override
    public Renderer2DPainter getPainter() {
        return this;
    }

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        if (limits == null)
            return;
        
        boolean ed = limits.isEditingPainting();
        boolean sy = limits.isShynched();
        limits.setEditingPainting(false);
        limits.setShynched(true);
        limits.paint(g, renderer);
        limits.setEditingPainting(ed);
        limits.setShynched(sy);
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.mp.element.IPlanElement#translate(double, double, double)
     */
    @Override
    public void translate(double offsetNorth, double offsetEast, double offsetDown) {
        double latDeg = limits.getOpAreaLat();
        double lonDeg = limits.getOpAreaLon();
        LocationType loc = new LocationType(latDeg, lonDeg);
        loc.translatePosition(offsetNorth, offsetEast, offsetDown);
        loc.convertToAbsoluteLatLonDepth();
        limits.setOpAreaLat(loc.getLatitudeDegs());
        limits.setOpAreaLon(loc.getLongitudeDegs());
    }

    public static void main(String[] args) {
        OperationLimitsPlanElement rdpe = new OperationLimitsPlanElement();
        OperationLimits rps = new OperationLimits();
        rps.setOpAreaLat(LocationType.FEUP.getLatitudeDegs());
        rps.setOpAreaLon(LocationType.FEUP.getLongitudeDegs());
        rps.setOpAreaWidth(200.);
        rps.setOpAreaLength(100.);
        rdpe.setElement(rps);
        
        String xml = rdpe.getElementAsXml();
        System.out.println(xml);
        
        OperationLimitsPlanElement rdpe1 = new OperationLimitsPlanElement();
        rdpe1.loadElementXml(xml);
        xml = rdpe1.getElementAsXml();
        System.out.println(xml);
    }
}
