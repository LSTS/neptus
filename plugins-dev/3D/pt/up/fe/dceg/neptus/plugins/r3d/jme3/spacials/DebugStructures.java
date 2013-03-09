/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
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
 * Author: Margarida Faria
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
