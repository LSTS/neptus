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
 * Jul 5, 2012
 * $Id:: ASSET_PATH.java 9615 2012-12-30 23:08:28Z pdias                        $:
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
