/*
 * Copyright (c) 2004-2015 Universidade do Porto - Faculdade de Engenharia
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
 * European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://www.lsts.pt/neptus/licence.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: coop
 * 11 Jul 2015
 */
package pt.lsts.neptus.hyperspectral;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.HyperSpecData;
import pt.lsts.neptus.mra.importers.IMraLog;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.coord.MapTileUtil;

/**
 * Layer where all the Hyperspectral data will be drawn.
 * @author tsmarques
 *
 */
public class OnPathLayer {
    private BufferedImage layer;
    private LocationType center;
    
    private final HashMap<Double, List<HyperspectralData>> dataset = new HashMap<>();
    private List<HyperspectralData> currentData = null;
    /* Contains the top left(maxLat and minLon) and bottom(minLat and maxLon) locations
       for each wavelength of the dataset  */
    private final HashMap<Double, LocationType[]> datasetMaxMinLocs = new HashMap<>();
    
    /* Data and corresponding EstimatedState */
    private HashMap<HyperspectralData, EstimatedState> dataState;
    /* All EstimatedStates */
    private IMraLog estimatedStatesLog;
    
    public OnPathLayer() {
        estimatedStatesLog = null;
        dataState = new HashMap<>();
    }
    
    public boolean contains(double wavelength) {
        return dataset.containsKey(wavelength);
    }
    
    public void saveEstimatedStatesLog(IMraLog log) {
        if(estimatedStatesLog == null)
            estimatedStatesLog = log;
    }
        
    public void addData(double wavelength, HyperSpecData msg, EstimatedState closestState, boolean isOverlapped) {
        HyperspectralData data = new HyperspectralData(msg, closestState, isOverlapped);
        
        List<HyperspectralData> dataList;
        if(dataset.containsKey(wavelength))
            dataList = dataset.get(wavelength);
        else {
            dataList = new LinkedList<>();
            dataset.put(wavelength, dataList);
        }
        dataList.add(data);
        
        dataState.put(data, closestState);
    }
    
    public void updateMinMaxLocations(double wavelength, LocationType newDataLocation) {
        double minLat = 180;
        double maxLat = -180;
        double minLon = 360;
        double maxLon = -360;
        
        
        if(datasetMaxMinLocs.containsKey(wavelength)) {
            LocationType[] currentMinMaxLoc = datasetMaxMinLocs.get(wavelength);
                        
            maxLat = currentMinMaxLoc[0].getLatitudeDegs();
            maxLon = currentMinMaxLoc[1].getLongitudeDegs();
            minLat = currentMinMaxLoc[1].getLatitudeDegs();
            minLon = currentMinMaxLoc[0].getLongitudeDegs();
        }
        
        if(newDataLocation.getLatitudeDegs() < minLat)
            minLat = newDataLocation.getLatitudeDegs();
        if(newDataLocation.getLatitudeDegs() > maxLat)
            maxLat = newDataLocation.getLatitudeDegs();
        if(newDataLocation.getLongitudeDegs() < minLon)
            minLon = newDataLocation.getLongitudeDegs();
        if(newDataLocation.getLongitudeDegs() > maxLon)
            maxLon = newDataLocation.getLongitudeDegs();
        
        LocationType topleft = new LocationType(maxLat, minLon);
        LocationType botright = new LocationType(minLat, maxLon);
        LocationType[] minMaxLoc = {topleft, botright};        
        
        datasetMaxMinLocs.put(wavelength, minMaxLoc);
    }  
    
    public BufferedImage getLayer() {
        return layer;
    }
    
    public LocationType getCenter() {
        return center;
    }
    
    private void resizeLayer(int newWidth, int newHeight) {
        /* TODO */
    }
    
    private LocationType[] initLayerArea(double wavelength) {
        LocationType[] locs = datasetMaxMinLocs.get(wavelength);
        LocationType topleft = locs[0];
        LocationType botright = locs[1];
        
        double padding = 65; /* TODO: best value? */
        
        topleft.setOffsetNorth(padding);
        topleft.setOffsetWest(padding);
        botright.setOffsetSouth(padding);
        botright.setOffsetEast(padding);
        
        topleft = topleft.getNewAbsoluteLatLonDepth();
        botright = botright.getNewAbsoluteLatLonDepth();
        
        LocationType[] updatedLocs = {topleft, botright};
        
        return updatedLocs;
    }
    
    public void generateLayer(double dataWavelength, StateRenderer2D renderer) {
        if(!dataset.containsKey(dataWavelength))
            return;
        
        /* get layer's 'area' */
        LocationType[] locs = initLayerArea(dataWavelength);
        LocationType topleft = locs[0];
        LocationType botright = locs[1];
        
        currentData = dataset.get(dataWavelength);
        
        Point2D p1 = renderer.getScreenPosition(topleft);
        Point2D p2 = renderer.getScreenPosition(botright);
        
        double top = p1.getY();
        double left = p1.getX();
        double right = p2.getX();
        double bottom = p2.getY();
        
        layer = new BufferedImage((int)(right - left), (int)(bottom - top), BufferedImage.TYPE_INT_ARGB);
        /* compute layer's center */
        double centerX = left + ((right - left) / 2);
        double centerY = top + ((bottom - top) / 2);
                
        Point2D p = new Point2D.Double(centerX, centerY);
        center = renderer.getRealWorldLocation(p);
        
        Graphics2D g = (Graphics2D) layer.getGraphics();
        double startX = -left;
        double startY = -top;
        g.translate(startX, startY);
        
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        for(HyperspectralData hyperspec : currentData)
            addDataToLayer(hyperspec, g, renderer);
    }
    
    /* TODO: check if current layer contains the given data. If not, resize accordingly */
    private void addDataToLayer(HyperspectralData hyperspec, Graphics2D g, StateRenderer2D renderer) {
        Point2D dataPosition = renderer.getScreenPosition(hyperspec.dataLocation);
        
        int x = (int)(dataPosition.getX() - (hyperspec.data.getWidth() / 2.0));
        int y = (int)(dataPosition.getY() - (hyperspec.data.getHeight() / 2.0));
        
        g.rotate(hyperspec.getVehicleHeading(), dataPosition.getX(), dataPosition.getY());
        g.drawImage(hyperspec.data, x, y, null);
        g.rotate(-hyperspec.getVehicleHeading(), dataPosition.getX(), dataPosition.getY());
    }
}
