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

import pt.up.fe.dceg.neptus.plugins.r3d.jme3.WorldInformation;
import pt.up.fe.dceg.neptus.types.coord.LocationType;

import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.Camera.FrustumIntersect;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

/**
 * Contains the 3D representation of the marker as well as all relevant information of a marker.
 * 
 * @author Margarida Faria
 * 
 */
public class Marker extends Element3D {
    private final Spatial spatial;

    private final LocationType positionLatLonDepth;
    private final Vector3f positionXYZ;
    private final String label;


    /**
     * Stores the data and creates the 3D representation.
     * 
     * @param assetManager
     * @param fatherNode
     * @param worldInfo
     * @param show true to start with spatial visible, false to start hidden
     * @param positionLatLonDepth
     * @param positionXYZ
     * @param label
     */
    public Marker(AssetManager assetManager, Node fatherNode, WorldInformation worldInfo,
            LocationType location, String label) {
        super(assetManager, fatherNode, worldInfo);
        this.label = label;
        this.positionLatLonDepth = location;
        // LatLon
        double[] calcOffSets = worldInfo.calcOffSets(location);
        float offset[] = { (float) calcOffSets[0], (float) calcOffSets[1] };
        offset = worldInfo.convertNED2jME_heightMapScale(offset);
        // Depth
        float heightConverted = worldInfo.convertDepthMeter2Px_heightMapScale((float) positionLatLonDepth.getDepth());
        this.positionXYZ = new Vector3f(offset[1], heightConverted, -offset[0]);

        this.spatial = createBox(ColorRGBA.randomColor(), new Vector3f(2f, 4f, 2f));
        spatial.move(positionXYZ);
        super.fatherNode.attachChild(spatial);
    }

    public void updateLabelGui(Node guiNode, Camera cam) {
            FrustumIntersect contains = cam.contains(spatial.getWorldBound());
            boolean containsGui = contains.equals(FrustumIntersect.Inside);
        if (!containsGui)
            return;

        Vector3f localTranslation = spatial.getLocalTranslation();
        Vector3f labelPos = cam.getScreenCoordinates(localTranslation);
        BitmapFont myFont = assetManager.loadFont("Interface/Fonts/Console.fnt");
        BitmapText hudText = new BitmapText(myFont, false);
        hudText.setSize(20);
        hudText.setColor(ColorRGBA.Blue); // font color
        hudText.setText(label); // the text
        hudText.setLocalTranslation(labelPos); // position
        guiNode.attachChild(hudText);

    }


}
