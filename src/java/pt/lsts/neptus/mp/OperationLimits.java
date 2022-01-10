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
 * Sep 24, 2010
 */
package pt.lsts.neptus.mp;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.bind.JAXB;
import javax.xml.bind.annotation.XmlRootElement;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.renderer2d.LayerPriority;
import pt.lsts.neptus.renderer2d.OffScreenLayerImageControl;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.ParallelepipedElement;
import pt.lsts.neptus.util.ColorUtils;
import pt.lsts.neptus.util.conf.ConfigFetch;

/**
 * @author zp
 * @author pdias
 *
 */
@XmlRootElement(name = "OperationLimits")
@LayerPriority(priority = 35)
public class OperationLimits implements Renderer2DPainter {

    public static final String FOLDER_CONF_OPLIMITS = ConfigFetch.getConfFolder() + "/oplimits/";

    public static final Color STRIPES_YELLOW_TRAMP = ColorUtils.setTransparencyToColor(ColorUtils.STRIPES_YELLOW, 130);
    public static final Paint PAINT_STRIPES = ColorUtils.createStripesPaint(ColorUtils.STRIPES_YELLOW, Color.BLACK);
    public static final Paint PAINT_STRIPES_TRAMSP = ColorUtils.createStripesPaint(STRIPES_YELLOW_TRAMP,
            ColorUtils.setTransparencyToColor(Color.BLACK, 130));
    public static final Paint PAINT_STRIPES_NOT_SYNC = ColorUtils.createStripesPaint(ColorUtils.STRIPES_YELLOW, Color.RED);
    public static final Paint PAINT_STRIPES_NOT_SYNC_TRAMSP = ColorUtils.createStripesPaint(STRIPES_YELLOW_TRAMP,
            ColorUtils.setTransparencyToColor(Color.RED, 130));

    protected Double maxDepth = null;
    protected Double minAltitude = null;
    protected Double maxAltitude = null;
    protected Double maxSpeed = null;
    protected Double minSpeed = null;
    protected Double maxVertRate = null;

    protected Double opAreaWidth = null;
    protected Double opAreaLength = null;
    protected Double opAreaLat = null;
    protected Double opAreaLon = null;
    protected Double opRotationRads = null;

    private OffScreenLayerImageControl offScreen = new OffScreenLayerImageControl();
    private boolean isShynched = true;
    private boolean isEditingPainting = false;

    /**
     * @return the isShynched
     */
    public boolean isShynched() {
        return isShynched;
    }
    
    /**
     * @param isShynched the isShynched to set
     */
    public void setShynched(boolean isShynched) {
        if (this.isShynched != isShynched)
            offScreen.triggerImageRebuild();
        this.isShynched = isShynched;
    }
    
    /**
     * @return the isEditingPainting
     */
    public boolean isEditingPainting() {
        return isEditingPainting;
    }
    
    /**
     * @param isEditingPainting the isEditingPainting to set
     */
    public void setEditingPainting(boolean isEditingPainting) {
        if (this.isEditingPainting != isEditingPainting)
            offScreen.triggerImageRebuild();
        this.isEditingPainting = isEditingPainting;
    }
    
    public Double getMaxDepth() {
        return maxDepth;
    }

    public void setMaxDepth(Double maxDepth) {
        this.maxDepth = maxDepth;
    }

    public Double getMinAltitude() {
        return minAltitude;
    }

    public void setMinAltitude(Double minAltitude) {
        this.minAltitude = minAltitude;
    }

    public Double getMaxAltitude() {
        return maxAltitude;
    }

    public void setMaxAltitude(Double maxAltitude) {
        this.maxAltitude = maxAltitude;
    }

    public Double getMaxSpeed() {
        return maxSpeed;
    }

    public void setMaxSpeed(Double maxSpeed) {
        this.maxSpeed = maxSpeed;
    }

    public Double getMinSpeed() {
        return minSpeed;
    }

    public void setMinSpeed(Double minSpeed) {
        this.minSpeed = minSpeed;
    }

    public Double getMaxVertRate() {
        return maxVertRate;
    }

    public void setMaxVertRate(Double maxVertRate) {
        this.maxVertRate = maxVertRate;
    }

    public Double getOpAreaWidth() {
        return opAreaWidth;
    }

    public void setOpAreaWidth(Double opAreaWidth) {
        this.opAreaWidth = opAreaWidth;
        offScreen.triggerImageRebuild();
    }

    public Double getOpAreaLength() {
        return opAreaLength;
    }

    public void setOpAreaLength(Double opAreaLength) {
        this.opAreaLength = opAreaLength;
        offScreen.triggerImageRebuild();
    }

    public Double getOpAreaLat() {
        return opAreaLat;
    }

    public void setOpAreaLat(Double opAreaLat) {
        this.opAreaLat = opAreaLat;
        offScreen.triggerImageRebuild();
    }

    public Double getOpAreaLon() {
        return opAreaLon;
    }

    public void setOpAreaLon(Double opAreaLon) {
        this.opAreaLon = opAreaLon;
        offScreen.triggerImageRebuild();
    }

    public Double getOpRotationRads() {
        return opRotationRads;
    }

    public void setOpRotationRads(Double opRotationRads) {
        this.opRotationRads = opRotationRads;
        offScreen.triggerImageRebuild();
    }

    public void setArea(ParallelepipedElement selection) {
        if (selection == null) {
            opAreaLat = opAreaLon = opAreaLength = opAreaWidth = opRotationRads = null;
        }
        else {
            double[] lld = selection.getCenterLocation().getAbsoluteLatLonDepth();
            opAreaLat = lld[0];
            opAreaLon = lld[1];
            opAreaLength = selection.getLength();
            opAreaWidth = selection.getWidth();
            opRotationRads = selection.getYawRad();
        }
        
        offScreen.triggerImageRebuild();
    }

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        if (!(opAreaLat != null && opAreaLength != null && opAreaLon != null && opAreaWidth != null
                && opRotationRads != null))
            return;
        
        boolean recreateImage = offScreen.paintPhaseStartTestRecreateImageAndRecreate(g, renderer);
        if (recreateImage) {
            Graphics2D g1 = offScreen.getImageGraphics();

            LocationType lt = new LocationType();
            lt.setLatitudeDegs(opAreaLat);
            lt.setLongitudeDegs(opAreaLon);
            Point2D pt = renderer.getScreenPosition(lt);
            g1.translate(pt.getX(), pt.getY());
            g1.scale(1, -1);
            g1.rotate(renderer.getRotation());
            g1.rotate(-opRotationRads + Math.PI / 2);
            g1.setColor(Color.red.brighter());
            double length = opAreaLength * renderer.getZoom();
            double width = opAreaWidth * renderer.getZoom();

            g1.setStroke(new BasicStroke(4));
            
            g1.setPaint(isShynched ? PAINT_STRIPES_TRAMSP : PAINT_STRIPES_NOT_SYNC_TRAMSP);
            if (isEditingPainting) {
                g1.setPaint(STRIPES_YELLOW_TRAMP);
                g1.fill(new Rectangle2D.Double(-length / 2, -width / 2, length, width));
                g1.setPaint(isShynched ? PAINT_STRIPES : PAINT_STRIPES_NOT_SYNC);
            }
            g1.draw(new Rectangle2D.Double(-length / 2, -width / 2, length, width));
        }            
        offScreen.paintPhaseEndFinishImageRecreateAndPaintImageCacheToRenderer(g, renderer);
    }

    public String asXml() {
        StringWriter writer = new StringWriter();
        JAXB.marshal(this, writer);
        return writer.toString();
    }

    public static OperationLimits loadXml(String xml) {
        return JAXB.unmarshal(new StringReader(xml), OperationLimits.class);
    }

    /**
     * Return the file path for the operation limits for a system with name systemName.
     * 
     * @param systemName
     * @return
     */
    public static String getFilePathForSystem(String systemName) {
        return FOLDER_CONF_OPLIMITS + systemName + ".xml";
    }

    public static void main(String[] args) {
        OperationLimits lims = new OperationLimits();
        lims.setMaxAltitude(200d);
        lims.setMinAltitude(100d);
        String xml = lims.asXml();
        NeptusLog.pub().info("<###> " + xml);
        OperationLimits lims2 = OperationLimits.loadXml(xml);
        NeptusLog.pub().info("<###> " + lims2.asXml());
    }
}
