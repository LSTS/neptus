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
 * Jul 5, 2012
 */
package pt.up.fe.dceg.neptus.plugins.r3d.jme3;

/**
 * Enumeration with all the paths used
 * 
 * @author Margarida Faria
 * 
 */
public enum ASSET_PATH {
    TERRAIN_LIGHTING("Common/MatDefs/Terrain/TerrainLighting.j3md"),
    TERRAIN("Common/MatDefs/Terrain/Terrain.j3md"),  
    TERRAIN_WIREFRAME("Common/MatDefs/Misc/Unshaded.j3md"), 
    TERRAIN_LIGHTING_COLOR("/assets/materialDefs/TerrainLightingColor.j3md"),  // All of the coloring according to height is done in the Fragment Shader
    TERRAIN_COLOR("/assets/materialDefs/ColorTerrain.j3md"), 
    M_HEIGHT("/mapas/colorBathyForHeightMap.jpg"), 
    M_HEIGHT_GRADIENT("/mapas/gradientHeightMap.jpg"), 
    M_HEIGHT_STEPS("/mapas/testHeights_4.bmp"), 
    M_HEIGHT_XY("/mapas/heightMapout.jpg"), 
    M_HEIGHT_NED("/mapas/checkNEDinJME.jpg"), 
    M_ALPHA("/mapas/alphaBathymetry.jpg"), 
    T_WATER("/texturas/water_723-diffuse.jpg"), 
    T_WATER_N("/texturas/water_723-normal.jpg"),  
    T_ROAD_TEXTURE("/texturas/TxUMAUcrackedearth.png"), 
    T_ROAD_TEXTURE_N("/texturas/TxUMAUcrackedearth_n.png"), 
    T_PLAIN_COLOR("/texturas/simpleTextureLight.png"), 
    T_BATHY_COLOR_IMAGE("/mapas/colorBathyForColor.jpg"), 
    BASE("assets/Textures/Terrain/splat"),
    // skybox set1
    S_PLAIN_400("/ceu/BlueSky_400.jpg"),
    S_CUT_EAST("/ceu/skyBox_cut_1.jpg"), 
    S_CUT_SOUTH("/ceu/skyBox_cut_2.jpg"), 
    S_CUT_WEST("/ceu/skyBox_cut_3.jpg"), 
    S_CUT_NORTH("/ceu/skyBox_cut_4.jpg"), 
    S_CUT_SET5("/ceu/skyBox_cut_5_top.jpg"), 
    // skybox set2
    S_SET2_EAST("/ceu/set2_left_1.jpg"), 
    S_SET2_SOUTH("/ceu/set2_center_2.jpg"), 
    S_SET2_WEST("/ceu/set2_right_3.jpg"), 
    S_SET2_NORTH("/ceu/set2_right_4.jpg"), 
    S_SET2_TOP("/ceu/set2_top.jpg"), 
    S_SET2_BOTTOM("/ceu/set2_bottom.jpg"), 
    
    I_HELP_SCREEN("/assets/Interface/HelpScreenLayout.xml");

    public final String relativePath;

    private ASSET_PATH(String relativePath) {
        this.relativePath = relativePath;
    }

}
