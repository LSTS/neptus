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
 * Aug 24, 2012
 */
package pt.up.fe.dceg.neptus.plugins.r3d.jme3.spacials;

import pt.up.fe.dceg.neptus.plugins.r3d.jme3.ASSET_PATH;
import pt.up.fe.dceg.neptus.plugins.r3d.jme3.WorldInformation;

import com.jme3.asset.AssetManager;
import com.jme3.light.DirectionalLight;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.texture.Texture;
import com.jme3.util.SkyFactory;

/**
 * @author Margarida Faria
 *
 */
public class Sky extends Element3D {
    private boolean showSky;
    private Spatial skySpacial;

    /**
     * This class will house the representation of the sky and managed associated light.
     * 
     * @param assetManager
     * @param fatherNode
     * @param worldInfo
     * @param sky true for sky, false for no sky
     */
    public Sky(AssetManager assetManager, Node fatherNode, WorldInformation worldInfo, boolean sky) {
        super(assetManager, fatherNode, worldInfo);
        this.showSky = sky;
        // initSkyBoxSet1();
        initSkyBoxSet2();
        if (showSky) {
            fatherNode.attachChild(skySpacial);
        }
        initLight();
    }

    /**
     * Create a sky box
     */
    protected void initSkyBoxSet1() {
        Texture north = assetManager.loadTexture(ASSET_PATH.BASE.relativePath + ASSET_PATH.S_CUT_NORTH.relativePath);
        Texture south = assetManager.loadTexture(ASSET_PATH.BASE.relativePath + ASSET_PATH.S_CUT_SOUTH.relativePath);

        Texture west = assetManager.loadTexture(ASSET_PATH.BASE.relativePath + ASSET_PATH.S_CUT_WEST.relativePath);
        Texture east = assetManager.loadTexture(ASSET_PATH.BASE.relativePath + ASSET_PATH.S_CUT_EAST.relativePath);

        Texture up = assetManager.loadTexture(ASSET_PATH.BASE.relativePath + ASSET_PATH.S_CUT_SET5.relativePath);
        Texture down = assetManager.loadTexture(ASSET_PATH.BASE.relativePath + ASSET_PATH.S_PLAIN_400.relativePath);

        skySpacial = SkyFactory.createSky(assetManager, west, east, north, south, up, down);
    }

    /**
     * Create a sky box
     */
    private void initSkyBoxSet2() {
        Texture north = assetManager.loadTexture(ASSET_PATH.BASE.relativePath + ASSET_PATH.S_SET2_NORTH.relativePath);
        Texture south = assetManager.loadTexture(ASSET_PATH.BASE.relativePath + ASSET_PATH.S_SET2_SOUTH.relativePath);

        Texture west = assetManager.loadTexture(ASSET_PATH.BASE.relativePath + ASSET_PATH.S_SET2_WEST.relativePath);
        Texture east = assetManager.loadTexture(ASSET_PATH.BASE.relativePath + ASSET_PATH.S_SET2_EAST.relativePath);

        Texture up = assetManager.loadTexture(ASSET_PATH.BASE.relativePath + ASSET_PATH.S_SET2_TOP.relativePath);
        Texture down = assetManager.loadTexture(ASSET_PATH.BASE.relativePath + ASSET_PATH.S_SET2_BOTTOM.relativePath);

        skySpacial = SkyFactory.createSky(assetManager, west, east, north, south, up, down);
    }

    /**
     * If sky is on display hides it. If it's already hidden shows it.
     */
    public void toggleSky() {
        showSky = !showSky;
        // PerformanceTest.changedTerrainDefs(showTerrain, showWater, showSky);
        if (showSky) {
            fatherNode.attachChild(skySpacial);
        }
        else {
            fatherNode.detachChild(skySpacial);
        }
    }

    /**
     * Initialize directional light simulating the sun.
     * 
     */
    private void initLight() {
        DirectionalLight light = new DirectionalLight();
        light.setDirection(WorldInformation.LIGHT_DIR);
        fatherNode.addLight(light);

        // AmbientLight al = new AmbientLight();
        // al.setColor(ColorRGBA.Gray.mult(1f));
        // fatherNode.addLight(al);
    }

}
