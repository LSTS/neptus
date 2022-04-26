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
 * 31/01/2015
 */
package pt.lsts.neptus.renderer2d;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.image.BufferedImage;

import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.AngleUtils;

/**
 * This is a helper class in order to control an offscreen buffered image to be used in
 * the {@link StateRenderer2D} painter.
 * An example of its use in the painter method is present next:
 * <blockquote><pre>
 * private OffScreenLayerImageControl offScreenImageControl = new OffScreenLayerImageControl();
 * 
 *  @Override
 *  public void paint(Graphics2D g, StateRenderer2D renderer) {
 *      super.paint(g, renderer);
 *      
 *      boolean recreateImage = offScreenImageControl.paintPhaseStartTestRecreateImageAndRecreate(g, renderer);
 *      if (recreateImage) {
 *          Graphics2D g2 = offScreenImageControl.getImageGraphics();
 *          // Paint what you want in the graphics
 *          ...
 *      }            
 *      offScreenImageControl.paintPhaseEndFinishImageRecreateAndPaintImageCacheToRenderer(g, renderer);
 *       
 *      ...
 *  }
 * </pre></blockquote>
 * 
 * Also if you want to force a repaint/recreation of the image, call:
 * <blockquote><pre>
 *  offScreenImageControl.triggerImageRebuild();
 * </pre></blockquote>
 * 
 * @author pdias
 *
 */
public class OffScreenLayerImageControl {

    private static int offScreenBufferPixelDefault = 400;
    
    private int offScreenBufferPixel = offScreenBufferPixelDefault;

    // Cache image
    private BufferedImage cacheImg = null;
    
    private Dimension dim = null;
    private int lastLod = -1;
    private LocationType lastCenter = null;
    private double lastRotation = Double.NaN;

    private boolean clearImgCacheRqst = false;
    
    private Graphics2D imageGraphics = null;

    private int imageTransparencyType = Transparency.TRANSLUCENT;

    public OffScreenLayerImageControl() {
    }

    /**
     * @param imageTransparencyType  See {@link Transparency}
     */
    public OffScreenLayerImageControl(int imageTransparencyType) {
        this.imageTransparencyType = imageTransparencyType;
    }

    /**
     * @return the cacheImg
     */
    public BufferedImage getCacheImg() {
        return cacheImg;
    }
    
    public void triggerImageRebuild() {
        clearImgCacheRqst = true;
    }
    
    /**
     * @return the offScreenBufferPixel
     */
    public int getOffScreenBufferPixel() {
        return offScreenBufferPixel;
    }
    
    /**
     * @param offScreenBufferPixel the offScreenBufferPixel to set
     */
    public void setOffScreenBufferPixel(int offScreenBufferPixel) {
        this.offScreenBufferPixel = offScreenBufferPixel;
        triggerImageRebuild();
    }
    
    /**
     * @return the lastCenter
     */
    public LocationType getLastCenter() {
        return lastCenter == null ? null : lastCenter.getNewAbsoluteLatLonDepth();
    }
    
    /**
     * Calculates the corner locations, clockwise from top left (4 points), or null if some error occurs.
     * 
     * @return
     */
    public LocationType[] getLastCorners() {
        try {
            Dimension dim = new Dimension(this.dim);
            int lastLod = this.lastLod;
            LocationType lastCenter = this.lastCenter.getNewAbsoluteLatLonDepth();
            double lastRotation = this.lastRotation;

            double topY = -dim.getHeight() / 2d;
            double bottomY = dim.getHeight() / 2d;
            double leftX = -dim.getWidth() / 2d;
            double rightX = dim.getWidth() / 2d;
            double[] topLeftPts = AngleUtils.rotate(lastRotation, leftX, topY, true);
            double[] topRightPts = AngleUtils.rotate(lastRotation, rightX, topY, true);
            double[] bottomRightPts = AngleUtils.rotate(lastRotation, rightX, bottomY, true);
            double[] bottomLeftPts = AngleUtils.rotate(lastRotation, leftX, bottomY, true);
            
            LocationType lTopLeft = lastCenter.getNewAbsoluteLatLonDepth();
            lTopLeft.translateInPixel(topLeftPts[0], topLeftPts[1], lastLod);
            LocationType lTopRight = lastCenter.getNewAbsoluteLatLonDepth();
            lTopRight.translateInPixel(topRightPts[0], topRightPts[1], lastLod);
            LocationType lBottomRight = lastCenter.getNewAbsoluteLatLonDepth();
            lBottomRight.translateInPixel(bottomRightPts[0], bottomRightPts[1], lastLod);
            LocationType lBottomLeft = lastCenter.getNewAbsoluteLatLonDepth();
            lBottomLeft.translateInPixel(bottomLeftPts[0], bottomLeftPts[1], lastLod);
            
            return new LocationType[] {lTopLeft, lTopRight, lBottomRight, lBottomLeft};
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public Dimension getCurDimentions(Dimension rv) {
        int newWidth = (int) dim.getWidth() + offScreenBufferPixel * 2;
        int newHeight = (int) dim.getHeight() + offScreenBufferPixel * 2;

        if (rv == null) {
            return new Dimension(newWidth, newHeight);
        }
        else {
            rv.setSize(newWidth, newHeight);
            return rv;
        }
    }
    
    /**
     * 
     */
    public boolean paintPhaseStartTestRecreateImageAndRecreate(Graphics2D g, StateRenderer2D renderer) {
        BufferedImage prevCache = cacheImg;
        int prevWidth = cacheImg != null ? cacheImg.getWidth() : -1;
        int prevHeight = cacheImg != null ? cacheImg.getHeight() : -1;
        
        if (!clearImgCacheRqst) {
            if (isToRegenerateCache(renderer)) {
                cacheImg = null;
            }
        }
        else {
            cacheImg = null;
            clearImgCacheRqst = false;
        }
        
        if (cacheImg == null) {
            if (imageGraphics != null) {
                try {
                    imageGraphics.dispose();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }

            dim = renderer.getSize(new Dimension());
            lastLod = renderer.getLevelOfDetail();
            lastCenter = renderer.getCenter();
            lastRotation = renderer.getRotation();

            int newWidth = (int) dim.getWidth() + offScreenBufferPixel * 2;
            int newHeight = (int) dim.getHeight() + offScreenBufferPixel * 2;
            Graphics2D g2;
            if (prevCache == null || prevWidth != newWidth && prevHeight != newHeight) {
                GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                GraphicsDevice gs = ge.getDefaultScreenDevice();
                GraphicsConfiguration gc = gs.getDefaultConfiguration();
                cacheImg = gc.createCompatibleImage(newWidth , newHeight , imageTransparencyType);
            }
            else { // Let us reuse the image
                cacheImg = prevCache;
                Graphics2D gc = (Graphics2D) cacheImg.createGraphics().create();
                gc.setBackground(new Color(0, 0, 0, 0));
                gc.clearRect(0, 0, cacheImg.getWidth(), cacheImg.getHeight());
                gc.dispose();
            }
            g2 = cacheImg.createGraphics();
            
            g2.translate(offScreenBufferPixel, offScreenBufferPixel);
            
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            // g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            // g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
            
            imageGraphics = g2;
            return true;
        }
        else {
            // imageGraphics = null;
            return false;
        }
    }

    /**
     * @return the imageGraphics
     */
    public Graphics2D getImageGraphics() {
        return imageGraphics;
    }

    public void paintPhaseEndFinishImageRecreateAndPaintImageCacheToRenderer(Graphics2D g, StateRenderer2D renderer) {
        if (imageGraphics != null) {
            imageGraphics.dispose();
            imageGraphics = null;
        }
        paintPhaseEndFinishImageRecreateAndPaintImageCacheToRendererNoGraphicDispose(g, renderer);
    }
    
    public void paintPhaseEndFinishImageRecreateAndPaintImageCacheToRendererNoGraphicDispose(Graphics2D g, StateRenderer2D renderer) {
        
        if (cacheImg != null) {
            Graphics2D g3 = (Graphics2D) g.create();

            double[] offset = renderer.getCenter().getDistanceInPixelTo(lastCenter, renderer.getLevelOfDetail());
            offset = AngleUtils.rotate(renderer.getRotation(), offset[0], offset[1], true);

            g3.drawImage(cacheImg, null, (int) offset[0] - offScreenBufferPixel, (int) offset[1] - offScreenBufferPixel);
            g3.dispose();
        }
    }

    private boolean isToRegenerateCache(StateRenderer2D renderer) {
        if (dim == null || lastLod < 0 || lastCenter == null || Double.isNaN(lastRotation)) {
            Dimension dimN = renderer.getSize(new Dimension());
            if (dimN.height != 0 && dimN.width != 0)
                dim = dimN;
            return true;
        }
        LocationType current = renderer.getCenter().getNewAbsoluteLatLonDepth();
        LocationType last = lastCenter == null ? new LocationType(0, 0) : lastCenter;
        double[] offset = current.getDistanceInPixelTo(last, renderer.getLevelOfDetail());
        if (Math.abs(offset[0]) > offScreenBufferPixel || Math.abs(offset[1]) > offScreenBufferPixel) {
            return true;
        }

        if (!dim.equals(renderer.getSize()) || lastLod != renderer.getLevelOfDetail()
                /*|| !lastCenter.equals(renderer.getCenter())*/ || Double.compare(lastRotation, renderer.getRotation()) != 0) {
            return true;
        }
        
        return false;
    }
}
