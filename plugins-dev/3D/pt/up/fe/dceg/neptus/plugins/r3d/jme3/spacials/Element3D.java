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
 * Aug 16, 2012
 */
package pt.up.fe.dceg.neptus.plugins.r3d.jme3.spacials;

import java.util.Vector;

import pt.up.fe.dceg.neptus.plugins.r3d.jme3.ASSET_PATH;
import pt.up.fe.dceg.neptus.plugins.r3d.jme3.WorldInformation;
import pt.up.fe.dceg.neptus.plugins.r3d.jme3.spacials.TerrainFromHeightMap.TEXTURE_MAP;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Spline;
import com.jme3.math.Spline.SplineType;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.control.LodControl;
import com.jme3.scene.debug.Arrow;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Curve;
import com.jme3.scene.shape.Line;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;
import com.jme3.util.BufferUtils;

/**
 * Father object of all 3D objects.
 * <p>
 * Here is all the information all of them need like objects required by the engine or scale of meters to 3D units.
 * 
 * @author Margarida Faria
 * 
 */
public class Element3D {
    protected final AssetManager assetManager;
    protected final Node fatherNode;
    protected final WorldInformation worldInfo;

    /**
     * Basic initialization of information, to be called by all children.
     * 
     * @param assetManager
     * @param fatherNode
     * @param worldInfo
     */
    protected Element3D(AssetManager assetManager, Node fatherNode, WorldInformation worldInfo) {
        super();
        this.assetManager = assetManager;
        this.fatherNode = fatherNode;
        this.worldInfo = worldInfo;
    }

    /**
     * Creates an empty box with the given characteristics and attached it to the scenograph.
     * 
     * @param color
     * @param position
     * @param size
     * @return the attached Geometry
     */
    protected Geometry attachBox(ColorRGBA color, Vector3f position, float size) {
        Box box = new Box(position, size, size, size);
        Geometry geom = new Geometry("Box", box);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", color);
        geom.setMaterial(mat);
        fatherNode.attachChild(geom);
        return geom;
    }

    /**
     * Creates a transparent plane with the same dimension as the terrain and placed directly above it.
     * 
     * @param offset the y coordinate for the plane
     * @param name
     * @param planeColor
     * @return the attached Geometry
     */
    protected Geometry createPlane(float offset, String name, ColorRGBA planeColor) {
        Material planeMaterial = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        planeMaterial.setColor("Color", planeColor);
        planeMaterial.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
        Box box = new Box(new Vector3f(0f, 0f, 0f), 1024, 0.01f, 1024);
        Geometry planeGeo = new Geometry(name, box);
        planeGeo.setQueueBucket(Bucket.Translucent);
        planeGeo.setMaterial(planeMaterial);
        planeMaterial.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
        // set the offset
        planeGeo.setLocalTranslation(worldInfo.getMapSize() / 2, offset, -worldInfo.getMapSize() / 2);
        return planeGeo;
    }

    /**
     * Creates a box with specified color and size.
     * <br>    x - the size of the box along the x axis, in both directions.
     * <br>    y - the size of the box along the y axis, in both directions.
     * <br>    z - the size of the box along the z axis, in both directions.
     * 
     * @param color
     * @param size 
     * @return the resulting geometry
     */
    protected Geometry createBox(ColorRGBA color, Vector3f size) {
        Material material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        material.setColor("Color", color);
        Box box = new Box(Vector3f.ZERO, size.x, size.y, size.z);
        Geometry geom = new Geometry("Box", box);
        geom.setMaterial(material);
        return geom;
    }

    /**
     * Creates simple line from start point to end point.
     * 
     * @param start
     * @param end
     * @param color
     * @return the attached Geometry
     */
    protected Geometry attachLine(Vector3f start, Vector3f end, ColorRGBA color) {
        Line line = new Line(start, end);
        line.setLineWidth(4);
        return putShape(line, color);
    }

    /**
     * Adds a mesh to the father node in the scenegraph.
     * 
     * @param shape
     * @param color
     * @return the attached Geometry
     */
    protected Geometry putShape(Mesh shape, ColorRGBA color) {
        Geometry g = new Geometry("coordinate axis", shape);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.getAdditionalRenderState().setWireframe(true);
        mat.setColor("Color", color);
        g.setMaterial(mat);
        fatherNode.attachChild(g);
        return g;
    }

    /**
     * Build a line that passes through the given points.
     * 
     * @param pathPoints
     * @param splineType the blending function
     * @param geomName
     * @return the attached Geometry
     */
    protected Geometry buildCurve(Vector<Vector3f> pathPoints, SplineType splineType, String geomName) {
        Spline spline = new Spline(splineType, pathPoints, 0.5f, false);
        Curve curve = new Curve(spline, 10);
        Geometry pathGeo = new Geometry(geomName, curve);
        Material pathMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        pathMat.setColor("Color", ColorRGBA.Green);
        pathGeo.setMaterial(pathMat);
        fatherNode.attachChild(pathGeo);
        return pathGeo;
    }

    /**
     * Attach the worlds coordinates to a given position.
     * 
     * @param pos
     */
    protected void attachCoordinateAxes(Vector3f pos) {
        Arrow arrow = new Arrow(Vector3f.UNIT_X.multLocal(worldInfo.convertHeightMeter2Px_heightMapScale(1)));
        arrow.setLineWidth(4); // make arrow thicker
        putShape(arrow, ColorRGBA.Red).setLocalTranslation(pos);

        arrow = new Arrow(Vector3f.UNIT_Y.multLocal(worldInfo.convertHeightMeter2Px_heightMapScale(1)));
        arrow.setLineWidth(4); // make arrow thicker
        putShape(arrow, ColorRGBA.Green).setLocalTranslation(pos);

        arrow = new Arrow(Vector3f.UNIT_Z.multLocal(worldInfo.convertHeightMeter2Px_heightMapScale(1)));
        arrow.setLineWidth(4); // make arrow thicker
        putShape(arrow, ColorRGBA.Blue).setLocalTranslation(pos);
    }

    /**
     * Sets the texture encapsulating the coupling between the material and it's channels.
     * 
     * @param terrainDef
     * @return the generated material
     */
    protected Material setTextureTerrainWithLight(ASSET_PATH terrainDef) {
        Material matTerrain = new Material(assetManager, terrainDef.relativePath);
        matTerrain.setBoolean("useTriPlanarMapping", false);
        matTerrain.setFloat("Shininess", 0.0f);
        // Alpha map
        matTerrain.setTexture(TEXTURE_MAP.ALPHA_BLUE.alpha,
                assetManager.loadTexture(ASSET_PATH.BASE.relativePath + ASSET_PATH.M_ALPHA.relativePath));
        // road (not in use but jME needs it to be set)
        setTexture(matTerrain, ASSET_PATH.T_ROAD_TEXTURE.relativePath, "", TEXTURE_MAP.ALPHA_RED, 64);
        // normal map texture to make terrain look like it has better definition
        setTexture(matTerrain, ASSET_PATH.T_PLAIN_COLOR.relativePath, ASSET_PATH.T_ROAD_TEXTURE_N.relativePath,
                TEXTURE_MAP.ALPHA_GREEN, 64);
        return matTerrain;
    }

    protected Material phongIluminated() {
        Material mat_lit = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        mat_lit.setBoolean("UseMaterialColors", true);
        mat_lit.setColor("Diffuse", ColorRGBA.White);
        mat_lit.setColor("Ambient", ColorRGBA.White);
        mat_lit.setColor("Specular", ColorRGBA.White);
        return mat_lit;
    }

    /**
     * All the steps needed to put a texture in use.
     * 
     * @param material
     * @param diffuseImagePath
     * @param normalMapPath
     * @param channel
     * @param textureScale
     */
    private void setTexture(Material material, String diffuseImagePath, String normalMapPath, TEXTURE_MAP channel,
            int textureScale) {
        // load diffuse texture image
        Texture diffuseTexture = assetManager.loadTexture(ASSET_PATH.BASE.relativePath + diffuseImagePath);
        diffuseTexture.setWrap(WrapMode.Repeat);
        // associate with one channel in alpha
        material.setTexture(channel.diffuse, diffuseTexture);
        // define texture scale
        material.setFloat(channel.diffuseScale, textureScale);
        if (normalMapPath.length() > 0) {
            // setup normalMap of texture
            Texture normalMapTexture = assetManager.loadTexture(ASSET_PATH.BASE.relativePath + normalMapPath);
            normalMapTexture.setWrap(WrapMode.Repeat);
            // add to the material
            material.setTexture(channel.normal, normalMapTexture);
        }
    }

    protected Material setTextureTerrainWireframe() {
        Material matWire = new Material(assetManager, ASSET_PATH.TERRAIN_WIREFRAME.relativePath);
        matWire.getAdditionalRenderState().setWireframe(true);
        matWire.setColor("Color", ColorRGBA.Green);
        return matWire;
    }

    /**
     * Creates a mesh with the specified triangles and attaches it to the father node.
     * 
     * @param vertices each vertex position
     * @param vertexIndex the order for the triangles
     * @param meshName
     * @param color
     * @param lodControl
     * @return the attached Geometry
     */
    protected Geometry createMesh(Vector3f[] vertices, int[] vertexIndex, String meshName, ColorRGBA color,
            VertexBuffer[] lod) {
        Geometry geom;
        Mesh mesh = new Mesh();
        // For downside
        mesh.setBuffer(Type.Position, 3, BufferUtils.createFloatBuffer(vertices));
        mesh.setBuffer(Type.Index, 3, BufferUtils.createIntBuffer(vertexIndex));
        mesh.updateBound();
        geom = new Geometry(meshName, mesh);
        if (lod != null) {
            mesh.setLodLevels(lod);
            LodControl lodControl = new LodControl();
            lodControl.setDistTolerance(20f);
            geom.addControl(lodControl);
        }
        // PerspectiveLodCCalculator perspectiveLodCalculator = new PerspectiveLodCalculator(cam, 50);

        // Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        // mat.getAdditionalRenderState().setWireframe(true);
        // mat.setColor("Color", ColorRGBA.Red);

        Material mat = phongIluminated();
        geom.setMaterial(mat);
        fatherNode.attachChild(geom);
        return geom;
    }
}
