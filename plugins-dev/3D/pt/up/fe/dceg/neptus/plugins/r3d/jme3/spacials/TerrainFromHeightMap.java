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
 * Jun 14, 2012
 * $Id:: TerrainFromHeightMap.java 9924 2013-02-14 10:37:31Z mfaria             $:
 */
package pt.up.fe.dceg.neptus.plugins.r3d.jme3.spacials;

import java.awt.image.BufferedImage;

import pt.up.fe.dceg.neptus.plugins.r3d.jme3.ASSET_PATH;
import pt.up.fe.dceg.neptus.plugins.r3d.jme3.WorldInformation;

import com.jme3.asset.AssetManager;
import com.jme3.bounding.BoundingBox;
import com.jme3.material.Material;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;
import com.jme3.terrain.geomipmap.TerrainLodControl;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.terrain.geomipmap.lodcalc.DistanceLodCalculator;
import com.jme3.terrain.heightmap.AbstractHeightMap;
import com.jme3.terrain.heightmap.ImageBasedHeightMap;
import com.jme3.texture.Texture;

/**
 * Here is everything related to terrain generation.
 * 
 * @author Margarida Faria
 * 
 */
public class TerrainFromHeightMap extends Element3D {
    private final String TERRAIN_NAME = "terrain";

    private final BufferedImage heightMap;
    private TerrainQuad terrain;
    
    // Toggle Variables
    private boolean showTerrain;
    private Material plainTextureMaterial;
    private Material colorShaderMaterial;

    /**
     * Enumeration aggregating the strings that identify the associated alpha map, diffuse map, duffuse scale and normal
     * map
     * 
     * @author Margarida Faria
     * 
     */
    public enum TEXTURE_MAP {
        ALPHA_GREEN("AlphaMap", "DiffuseMap", "DiffuseMap_0_scale", "NormalMap"), 
        ALPHA_RED("AlphaMap", "DiffuseMap_1", "DiffuseMap_1_scale", "NormalMap_1"), 
        ALPHA_BLUE("AlphaMap", "DiffuseMap_2", "DiffuseMap_2_scale", "NormalMap_2"), 
        SIMPLE_ALPHA_RED("Alpha", "Tex1", "Tex1Scale", null), 
        SIMPLE_ALPHA_GREEN("Alpha", "Tex2", "Tex2Scale", null), 
        SIMPLE_ALPHA_BLUE("Alpha", "Tex3", "Tex3Scale", null);

        public final String alpha;
        public final String diffuse;
        public final String diffuseScale; 
        public final String normal;

        private TEXTURE_MAP(String alpha, String diffuse, String diffuseScale, String normal) {
            this.alpha = alpha;
            this.diffuse = diffuse;
            this.diffuseScale = diffuseScale;
            this.normal = normal;
        }
    }

    /**
     * Indication for terrain materials
     * 
     * @author Margarida Faria
     * 
     */
    public enum TERRAIN_TYPE {
        TEXTURED, COLOR_BY_HEIGHT, WIREFRAME;
    }



    /**
     * The constructor with the fundamental variables initialized
     * 
     * @param heightMap
     * @param assetManager
     * @param rootNode
     * @param minTerrainHeight
     * @param worldInfo
     */
    public TerrainFromHeightMap(BufferedImage heightMap, AssetManager assetManager, Node rootNode,
            float minTerrainHeight, WorldInformation worldInfo) {
        super(assetManager, rootNode, worldInfo);
        this.heightMap = heightMap;
    }

    /**
     * Generate the terrain specifying what terrain definition to use
     * 
     * @param cam the camera
     * @param terrainType the terrain definition
     */
    /**
     * @param cam
     * @param terrainType
     * @param showColored
     */
    public void initTerrain(Camera cam, TERRAIN_TYPE terrainType, boolean showColored) {
        terrain = generateHeightMapFromArray(cam);
        // terrain = generateHeighmapFromImage(ASSET_PATH.BASE.relativePath + ASSET_PATH.M_HEIGHT_NED.relativePath,
        // cam);
        // TERRAIN MATERIAL
        Material material;
        plainTextureMaterial = setTextureTerrainWithLight(ASSET_PATH.TERRAIN_LIGHTING);
        if (!showColored) {
            material = plainTextureMaterial;
            showTerrain = false;
        }
        else {
            colorShaderMaterial = setTextureTerrainWithLight(ASSET_PATH.TERRAIN_LIGHTING_COLOR);
            float deltaWaterColumnM = worldInfo.getMaxWaterColumnM() - worldInfo.getMinWaterColumnM();
            float deltaWaterColumn = worldInfo.convertHeightMeter2Px_heightMapScale(deltaWaterColumnM);
            colorShaderMaterial.setFloat("MaxHeight", deltaWaterColumn);
            // NeptusLog.pub().debug(
            // "MaxWaterColumnM: " + worldInfo.getMaxWaterColumnM() + "; MaxWaterColumnJME: "
            // + worldInfo.convertHeightMeter2Px_heightMapScale(worldInfo.getMaxWaterColumnM())
            // + "; MinWaterColumnM: " + worldInfo.getMinWaterColumnM() + "; MinWaterColumnJME: "
            // + worldInfo.convertHeightMeter2Px_heightMapScale(worldInfo.getMinWaterColumnM())
            // + "; deltaWaterColumnM: " + deltaWaterColumnM + "; deltaJME:" + deltaWaterColumn);
            // colorShaderMaterial.setFloat("MaxHeight", 2);
            // colorShaderMaterial.setFloat("MinHeight", worldInfo.getTerrainLvl());
            colorShaderMaterial.setFloat("MinHeight", 0);
            switch (terrainType) {
                case TEXTURED:
                    material = plainTextureMaterial;
                    showTerrain = false;
                    break;
                case COLOR_BY_HEIGHT:
                    material = colorShaderMaterial;
                    showTerrain = true;
                    break;
                case WIREFRAME:
                    material = setTextureTerrainWireframe();
                    break;
                default:
                    material = colorShaderMaterial;
                    break;
            }
        }

        // PerformanceTest.changedTerrainDefs(showTerrain, water, sky);
        terrain.setMaterial(material);
        terrain.setModelBound(new BoundingBox());
        terrain.updateModelBound();
        terrain.setLocalTranslation(worldInfo.getMapSize() / 2, worldInfo.getTerrainLvl(), -worldInfo.getMapSize() / 2);
        // terrain.setLocalTranslation(worldInfo.getMapSize() / 2, 0, -worldInfo.getMapSize() / 2);
        terrain.setLocalScale(1f, 1f, 1f);
        fatherNode.attachChild(terrain);

    }

    /**
     * Generates a terrain based on a gray scale image. Used for testing, use the Buffered Image generated by
     * BathymetryGenarator.generateBufferedImage(XYZDataType, ColorMap) and then generateHeightMapFromArray instead.
     * 
     * @param grayScaleFilePath the path (with filename) to the image.
     * @param cam the camera
     * @return the resulting terrain
     */
    @Deprecated
    public TerrainQuad generateHeighmapFromImage(String grayScaleFilePath, Camera cam) {
        AbstractHeightMap heightmap = null;
        Texture heightMapImage = assetManager.loadTexture(grayScaleFilePath);
        heightmap = new ImageBasedHeightMap(heightMapImage.getImage());

        heightmap.load();
        TerrainQuad terrain = new TerrainQuad(TERRAIN_NAME, WorldInformation.PATCH_SIZE, worldInfo.getMapSize() + 1,
                heightmap.getHeightMap());
        TerrainLodControl control = new TerrainLodControl(terrain, cam);
        control.setLodCalculator(new DistanceLodCalculator(WorldInformation.PATCH_SIZE, 5f)); // patch size, and a
                                                                                            // multiplier
        return terrain;
    }

    private TerrainQuad generateHeightMapFromArray(Camera cam) {
        float[] greyScaleValues = convertBufferedImageToArray();
        TerrainQuad terrain = new TerrainQuad(TERRAIN_NAME, WorldInformation.PATCH_SIZE, worldInfo.getMapSize() + 1,
                greyScaleValues);

        TerrainLodControl control = new TerrainLodControl(terrain, cam);
        control.setLodCalculator(new DistanceLodCalculator(WorldInformation.PATCH_SIZE, 5f)); // patch size, and a
                                                                                            // multiplier
        terrain.addControl(control);
        return terrain;
    }

    /**
     * Takes the BufferedImage for bathymetry set in the constuctor and creates an array with the rgb values converted
     * to greyscale
     * 
     * @return rgb values from the BufferedImage converted to greyscale
     */
    private float[] convertBufferedImageToArray() {
        int imageWidth = heightMap.getWidth();
        int imageHeight = heightMap.getHeight();
        float array[] = new float[imageWidth * imageHeight];
        int[] dataBuffInt = heightMap.getRGB(0, 0, imageWidth, imageHeight, null, 0, imageWidth);
        float average;
        for (int i = 0; i < dataBuffInt.length; i++) {
            // In the BufferedImage, red, green, blue and alpha are store concatenated so we need to shift to extract
            average = (dataBuffInt[i] >> 16) & 0xFF; // red
            average += (dataBuffInt[i] >> 8) & 0xFF; // green
            average += (dataBuffInt[i] >> 0) & 0xFF; // blue
            average = average / 3f;
            array[i] = average;
        }
        return array;
    }

    /**
     * If terrain is on display hides it. If it's already hidden shows it.
     */
    public void toggleTerrain() {
        showTerrain = !showTerrain;
        // PerformanceTest.changedTerrainDefs(showTerrain, showWater, showSky);
        if (showTerrain) {
            terrain.setMaterial(colorShaderMaterial);
        }
        else {
            terrain.setMaterial(plainTextureMaterial);
        }
    }

    /**
     * Get the current terrain.
     * 
     * @return
     */
    public TerrainQuad getTerrain() {
        return terrain;
    }
}
