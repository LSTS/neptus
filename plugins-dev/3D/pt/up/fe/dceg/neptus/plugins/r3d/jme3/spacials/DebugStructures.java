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
 * Jul 12, 2012
 */
package pt.up.fe.dceg.neptus.plugins.r3d.jme3.spacials;

import pt.up.fe.dceg.neptus.plugins.r3d.dto.BathymetryLogInfo;
import pt.up.fe.dceg.neptus.plugins.r3d.jme3.WorldInformation;

import com.jme3.asset.AssetManager;
import com.jme3.scene.Node;

/**
 * Add here all objects that help debug but aren't supposed to show in the final version.
 * 
 * @author Margarida Faria
 * 
 */
public class DebugStructures extends Element3D {

    /**
     * All objects are added in the constructor.
     * 
     * @param rootNode
     * @param assetManager
     * @param batyData
     * @param worldInfo
     */
    public DebugStructures(Node rootNode, AssetManager assetManager, BathymetryLogInfo batyData,
            WorldInformation worldInfo) {
        super(assetManager, rootNode, worldInfo);
        // attachCoordinateAxes(new Vector3f(0, 0, 0));

        // float terrainOffset = worldInfo.convertDepthMeter2Px_heightMapScale(batyData.getMaxDepth());
        // attachCoordinateAxes(new Vector3f(0, terrainOffset, 0));
        // attachCoordinateAxes(new Vector3f(0, worldInfo.getTerrainLvl(), 0));
        // Arrow arrow = new Arrow(Vector3f.UNIT_Y.multLocal(worldInfo.convertHeightMeter2Px_heightMapScale(7)));
        // arrow.setLineWidth(4); // make arrow thicker
        // putShape(arrow, ColorRGBA.Green).setLocalTranslation(new Vector3f(0, worldInfo.getTerrainLvl(), 0));
        // arrow = new Arrow(Vector3f.UNIT_Y.multLocal(7));
        // arrow.setLineWidth(4); // make arrow thicker
        // putShape(arrow, ColorRGBA.Orange).setLocalTranslation(new Vector3f(0, worldInfo.getTerrainLvl(), 0));
        // attachLine(new Vector3f(0, 0, 0), new Vector3f(0, -255, 0), ColorRGBA.Brown);

        // Lines for terrain offset
        // attachLine(new Vector3f(WorldInformation.MAP_SIZE - 2, 0, 0), new Vector3f(WorldInformation.MAP_SIZE - 2,
        // WorldInformation.convertDepthMeter2Px(batyData.getMinDepth()), 0),
        // ColorRGBA.Cyan);
        // attachLine(new Vector3f(WorldInformation.MAP_SIZE - 98, 0, 0), new Vector3f(WorldInformation.MAP_SIZE - 98,
        // terrainOffset, 0), ColorRGBA.Blue);

        // rootNode.attachChild(createPlane(0, "45", ColorRGBA.Pink));
        // rootNode.attachChild(createPlane(worldInfo.getTerrainLvl(), "ground", ColorRGBA.Cyan));
        // System.out.println("worldInfo.getTerrainLvl():" + worldInfo.getTerrainLvl());
    }

}
