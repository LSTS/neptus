/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by Margarida Faria
 * Jul 9, 2012
 */
package pt.up.fe.dceg.neptus.plugins.r3d.jme3.spacials;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.plugins.r3d.dto.BathymetryLogInfo;
import pt.up.fe.dceg.neptus.plugins.r3d.jme3.WorldInformation;

import com.jme3.asset.AssetManager;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.water.WaterFilter;

/**
 * Aggregates all things water like the two kinds of water representation and switching between them. To use this first
 * create an instance and then use the initialize method.
 * 
 * @author Margarida Faria
 * 
 */
public class Water extends Element3D {
    private static final ColorRGBA GEOID_PLANE_COLOR = new ColorRGBA(0.804f, 0.537f, 0.2f, 0.5f);
    private static final ColorRGBA WATER_PLANE_COLOR = new ColorRGBA(0f, 0f, 1f, 0.5f);

    /**
     * Indication for water type
     * 
     * @author Margarida Faria
     * 
     */
    public enum WATER_TYPE {
        NONE, SEA_LEVEL_REF, REAL_WATER;
    }

    private WATER_TYPE waterRepresentation;
    private Geometry waterSeaLevel;
    private Geometry geoidLevel;
    private FilterPostProcessor filterWater;

    private final ViewPort viewPort;

    /**
     * Simple initialization of variables.
     * 
     * @param viewPort
     * @param assetManager
     * @param rootNode
     * @param worldInfo
     */
    public Water(ViewPort viewPort, AssetManager assetManager, Node rootNode, WorldInformation worldInfo) {
        super(assetManager, rootNode, worldInfo);
        this.viewPort = viewPort;
    }

    /**
     * Generate the water structures, set if there is going to be water.
     * 
     * @param water true for water, false for no water
     */
    public void initWater(WATER_TYPE water, boolean showRealWater) {
        // water
        this.waterRepresentation = water;
        geoidLevel = createPlane(worldInfo.getGeoidtLvl(), "Geoid level", GEOID_PLANE_COLOR);

        // if there is water level info init water plane representation
        if (worldInfo.getWaterLvl() != BathymetryLogInfo.INVALID_HEIGHT) {
            waterSeaLevel = createPlane(worldInfo.getWaterLvl(), "Sea level", WATER_PLANE_COLOR);
            // if driver supports init realistic water filter
            if (showRealWater) {
                initRealisticWater();
                // adjust to selected representation
                switch (waterRepresentation) {
                    case NONE:
                        viewPort.removeProcessor(this.filterWater);
                        break;
                    case REAL_WATER:
                        // this is the way it already is
                        break;
                    case SEA_LEVEL_REF:
                        viewPort.removeProcessor(this.filterWater);
                        fatherNode.attachChild(waterSeaLevel);
                        fatherNode.attachChild(geoidLevel);
                        break;
                    default:
                        NeptusLog.pub().error(
                                "Water type " + waterRepresentation + " is not expected in the initialization. "
                                        + Thread.currentThread().getStackTrace());
                        break;
                }
            }
            // if plane representation is asked but there is no support for realistic water
            else if (waterRepresentation.equals(WATER_TYPE.SEA_LEVEL_REF)){
                fatherNode.attachChild(waterSeaLevel);
                fatherNode.attachChild(geoidLevel);
            }

        }
        else if (waterRepresentation.equals(WATER_TYPE.SEA_LEVEL_REF)) {
            fatherNode.attachChild(geoidLevel);
        }
    }


// if (!showRealWater || worldInfo.getWaterLvl() == BathymetryLogInfo.INVALID_HEIGHT) {
        // fatherNode.attachChild(waterSeaLevel);
        // fatherNode.attachChild(geoidLevel);
        // }
        // else {
        // initRealisticWater();
        //
        // switch (waterRepresentation) {
        // case NONE:
        // viewPort.removeProcessor(this.filterWater);
        // break;
        // case REAL_WATER:
        // // this is the way it already is
        // break;
        // case SEA_LEVEL_REF:
        // viewPort.removeProcessor(this.filterWater);
        // fatherNode.attachChild(waterSeaLevel);
        // fatherNode.attachChild(geoidLevel);
        // break;
        // default:
        // NeptusLog.pub().error("Water type " + waterRepresentation + " is not expected in the initialization. "
        // + Thread.currentThread().getStackTrace());
        // break;
        // }
        // }

        // if (worldInfo.getWaterLvl() != BathymetryLogInfo.INVALID_HEIGHT) {
        // initRealisticWater();
        // waterSeaLevel = createPlane(worldInfo.getWaterLvl(), "Sea level", WATER_PLANE_COLOR);
        //
        // switch (showWater) {
        // case NONE:
        // viewPort.removeProcessor(this.filterWater);
        // break;
        // case REAL_WATER:
        // // this is the way it already is
        // break;
        // case SEA_LEVEL_REF:
        // viewPort.removeProcessor(this.filterWater);
        // fatherNode.attachChild(waterSeaLevel);
        // fatherNode.attachChild(geoidLevel);
        // break;
        // default:
        // NeptusLog.pub().error("Water type " + showWater + " is not expected in the initialization. "
        // + Thread.currentThread().getStackTrace());
        // break;
        // }
        // }
        // else if(!showRealWater){
        // waterSeaLevel = createPlane(worldInfo.getWaterLvl(), "Sea level", WATER_PLANE_COLOR);
        // fatherNode.attachChild(waterSeaLevel);
        // fatherNode.attachChild(geoidLevel);
        // }
        // else {
        // switch (showWater) {
        // case NONE:
        // viewPort.removeProcessor(this.filterWater);
        // break;
        // case REAL_WATER:
        // // this is the way it already is
        // break;
        // case SEA_LEVEL_REF:
        // viewPort.removeProcessor(this.filterWater);
        // fatherNode.attachChild(geoidLevel);
        // break;
        // default:
        // NeptusLog.pub().error("Water type " + showWater + " is not expected in the initialization. "
        // + Thread.currentThread().getStackTrace());
        // break;
        // }
        // }
    // }

    /**
     * Generation of water based on jME3's WaterFilter.
     * 
     * @param lightDir the light direction
     */
    private void initRealisticWater() {
        filterWater = new FilterPostProcessor(assetManager);
        WaterFilter water = new WaterFilter(fatherNode, WorldInformation.LIGHT_DIR);
        // Color constantes
        ColorRGBA shallowWaterColor = new ColorRGBA(0.0078f, 0.3176f, 0.5f, 1.0f);
        ColorRGBA deepWaterColor = new ColorRGBA(0.0039f, 0.00196f, 0.145f, 1.0f);
        Vector3f colorExtinction = new Vector3f(5.0f, 20.0f, 30.0f);
        // set underwater color
        water.setDeepWaterColor(deepWaterColor);
        water.setWaterColor(shallowWaterColor);
        water.setColorExtinction(colorExtinction); // refraction color extintion
        water.setWaterTransparency(0.1f);
        // set wave stuff
        water.setUseFoam(false);
        water.setUseRipples(true);
        water.setMaxAmplitude(0.3f);
        water.setWaveScale(0.008f);
        water.setSpeed(0.7f);
        water.setShoreHardness(0.8f); // changed from 0.6 to 0.8 so water integrates better with terrain
        water.setRefractionConstant(0.01f); // changed from 0.2 to 0.01 underwater sky woobles more
        water.setShininess(0.3f);
        water.setSunScale(1.0f);
        // set the offset for the water level
        water.setWaterHeight(worldInfo.getWaterLvl());
        filterWater.addFilter(water);
        viewPort.addProcessor(filterWater);
    }

    /**
     * Iterates over the modes of surfice visualization: none, planes in the place of water and geoid and real water
     */
    public void toggleWater() {
        try {
            WATER_TYPE[] waterTypes = WATER_TYPE.values();
            waterRepresentation = waterTypes[(waterRepresentation.ordinal() + 1) % 3];
            // PerformanceTest.changedTerrainDefs(showTerrain, showWater, showSky);
            switch (waterRepresentation) {
                case NONE:
                    viewPort.removeProcessor(this.filterWater);
                    fatherNode.detachChild(waterSeaLevel);
                    fatherNode.detachChild(geoidLevel);
                    break;
                case REAL_WATER:
                    viewPort.addProcessor(this.filterWater);
                    fatherNode.detachChild(waterSeaLevel);
                    fatherNode.detachChild(geoidLevel);
                    break;
                case SEA_LEVEL_REF:
                    viewPort.removeProcessor(this.filterWater);
                    fatherNode.attachChild(waterSeaLevel);
                    fatherNode.attachChild(geoidLevel);
                    break;
                default:
                    NeptusLog.pub().error(
                            "Water type " + waterRepresentation + " is not expected in the initialization. "
                                    + Thread.currentThread().getStackTrace());
                    break;
            }
        }
        catch (Exception e) {
            NeptusLog.pub().error(I18n.text("Error occured toggling water"));
            e.printStackTrace();
        }
    }

    /**
     * Shows the geoid plane on the mode for planes
     */
    public void toggleGeoid() {
        WATER_TYPE[] waterTypes = WATER_TYPE.values();
        waterRepresentation = waterTypes[(waterRepresentation.ordinal() + 1) % 3];
        // PerformanceTest.changedTerrainDefs(showTerrain, showWater, showSky);
        switch (waterRepresentation) {
            case NONE:
                viewPort.removeProcessor(this.filterWater);
                fatherNode.detachChild(geoidLevel);
                break;
            case REAL_WATER:
                viewPort.addProcessor(this.filterWater);
                fatherNode.detachChild(geoidLevel);
                break;
            case SEA_LEVEL_REF:
                viewPort.removeProcessor(this.filterWater);
                fatherNode.attachChild(geoidLevel);
                break;
            default:
                NeptusLog.pub().error("Water type " + waterRepresentation + " is not expected in the initialization. "
                        + Thread.currentThread().getStackTrace());
                break;
        }
    }

    /**
     * Shows the geoid plane on the mode for planes
     */
    public void togglePlanes() {
        // WATER_TYPE[] waterTypes = WATER_TYPE.values();
        // waterRepresentation = waterTypes[(waterRepresentation.ordinal() + 1) % 3];

        // PerformanceTest.changedTerrainDefs(showTerrain, showWater, showSky);
        switch (waterRepresentation) {
            case NONE:
            case REAL_WATER:
                waterRepresentation = WATER_TYPE.SEA_LEVEL_REF;
                fatherNode.attachChild(waterSeaLevel);
                fatherNode.attachChild(geoidLevel);
                break;
            case SEA_LEVEL_REF:
                waterRepresentation = WATER_TYPE.NONE;
                fatherNode.detachChild(waterSeaLevel);
                fatherNode.detachChild(geoidLevel);
                break;
            default:
                NeptusLog.pub().error(
                        "Water type " + waterRepresentation + " is not expected in the initialization. "
                                + Thread.currentThread().getStackTrace());
                break;
        }
    }
}
