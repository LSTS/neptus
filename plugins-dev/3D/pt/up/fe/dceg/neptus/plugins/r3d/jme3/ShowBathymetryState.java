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
 * Jun 4, 2012
 */
package pt.up.fe.dceg.neptus.plugins.r3d.jme3;

import java.awt.Canvas;
import java.util.EnumSet;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.plugins.r3d.MarkerObserver;
import pt.up.fe.dceg.neptus.plugins.r3d.dto.BathymetryLogInfo;
import pt.up.fe.dceg.neptus.plugins.r3d.jme3.spacials.DebugStructures;
import pt.up.fe.dceg.neptus.plugins.r3d.jme3.spacials.MarkersNode;
import pt.up.fe.dceg.neptus.plugins.r3d.jme3.spacials.Sky;
import pt.up.fe.dceg.neptus.plugins.r3d.jme3.spacials.TerrainFromHeightMap;
import pt.up.fe.dceg.neptus.plugins.r3d.jme3.spacials.TerrainFromHeightMap.TERRAIN_TYPE;
import pt.up.fe.dceg.neptus.plugins.r3d.jme3.spacials.VehiclePath;
import pt.up.fe.dceg.neptus.plugins.r3d.jme3.spacials.Water;
import pt.up.fe.dceg.neptus.plugins.r3d.jme3.spacials.Water.WATER_TYPE;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.math.Vector3f;
import com.jme3.niftygui.NiftyJmeDisplay;
import com.jme3.renderer.Caps;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.system.JmeCanvasContext;

import de.lessvoid.nifty.Nifty;

/**
 * This is the main state for the visualization. It creates a terrain based on a bathymetry mapping image
 * 
 * @author Margarida Faria
 * 
 */
public class ShowBathymetryState extends AbstractAppState implements ActionListener {
    /**
     * 
     */
    // initial state
    private final boolean SKY = false;
    private final WATER_TYPE WATER = WATER_TYPE.NONE;
    private final int STARTING_CAM_SPEED = 1;
    private final Vector3f CAM_DIRECTION = new Vector3f(0.5f, -1f, -0.5f);
    // Inner workings of engine variables
    private JmeComponent app;
    private Node rootNode;
    private AssetManager assetManager;
    private InputManager inputManager;
    private ViewPort viewPort;
    private BulletAppState bulletState;
    // GUI variables
    private NiftyJmeDisplay niftyDisplay;
    private boolean showHelp = false;
    private boolean resizeOnScreenSwitch = false;
    private static String helpScreenName;
    // World variables
    private final WorldInformation worldInfo;
    private BathymetryLogInfo bathyData; // only on initialization, will be set to null because is no longer needed
    private final MarkerObserver markerObserver;
    // 3D representations
    private TerrainFromHeightMap terrainDefs;
    private Water waterDefs;
    private VehiclePath vehiclePath;
    private Sky sky;
    private MarkersNode markersNode;
    // Movement variables
    private final Vector3f walkDirection = new Vector3f();
    private boolean left = false;
    private boolean right = false;
    private  boolean forward = false;
    private  boolean backwards = false;
    private float camSpeed = STARTING_CAM_SPEED;
    private final Vector3f camLocation;
    private CharacterControl player;

    /**
     * All the possible actions
     * 
     * @author Margarida Faria
     * 
     */
    private enum Actions {
        LEFT,
        RIGHT,
        UP,
        DOWN,
        T_SKY,
        T_WATER,
        T_TERRAIN,
        T_HELP,
        T_GEOID,
        T_PLANES,
        T_VEHICLE_PATH,
        T_VEHICLE_PATH_OPTIMIZED;
    }

    /**
     * Converts meters into jme units and sets starting position of camera
     * 
     * @param bathyInfo with all the information extracted from the logs
     * @param markerObserver
     */
    public ShowBathymetryState(BathymetryLogInfo bathyInfo, MarkerObserver markerObserver) {
        super();
        this.bathyData = bathyInfo;
        this.markerObserver = markerObserver;
        // init variables so that WorldInformation can take charge of conversions
        float[] cornerOffsetsM = { (float) bathyInfo.getReferenceTopLeftCornerOffsets()[0],
                (float) bathyInfo.getReferenceTopLeftCornerOffsets()[1] };
        worldInfo = new WorldInformation(bathyInfo, cornerOffsetsM);
        // init starting camLocation
        float waterTerrainDif = (worldInfo.getWaterLvl() - worldInfo.getTerrainLvl()) / 3;
        float camLvl = worldInfo.getTerrainLvl() + waterTerrainDif * 2;
        camLocation = new Vector3f(10, camLvl, -10);
        // NeptusLog.pub().debug(
        // I18n.text("camLocation") + ":" + camLvl + "; " + I18n.text("terrainLvl") + ": "
        // + worldInfo.getTerrainLvl() + "; " + I18n.text("geoidLvl") + ":"
        // + worldInfo.getGeoidtLvl());
    }

    /**
     * Initialization method, called when starting the state. Here are done the terrain, light and camera
     * initializations
     */
    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        // init stuff that is independent of whether state is PAUSED or RUNNING
        super.initialize(stateManager, app);
        this.app = (JmeComponent) app;
        this.rootNode = this.app.getRootNode();
        this.assetManager = this.app.getAssetManager();
        // this.stateManager = this.app.getStateManager();
        this.inputManager = this.app.getInputManager();

        boolean fullCompatibility;
        EnumSet<Caps> caps = app.getRenderer().getCaps();
        if (caps.contains(Caps.GLSL130)) {
            fullCompatibility = true;
        }
        else {
            fullCompatibility = false;
        }
        // Sky
        sky = new Sky(assetManager, rootNode, worldInfo, SKY);
        // Terrain
        this.viewPort = this.app.getViewPort();
        // float minTerrainHeight = batyData.getGeoidHeightM() - batyData.getMaxDepth();
        // float terrainMinHeight = WorldInformation.convertDepthMeter2Px(minTerrainHeight);
        terrainDefs = new TerrainFromHeightMap(bathyData.getBuffImageHeightMap(), assetManager, rootNode,
                worldInfo.getTerrainLvl(), worldInfo);
        terrainDefs.initTerrain(app.getCamera(), TERRAIN_TYPE.COLOR_BY_HEIGHT, fullCompatibility);
        // Water
        waterDefs = new Water(viewPort, assetManager, rootNode, worldInfo);
        // waterDefs.initWater(WATER, batyData.getWaterLevel(), batyData.getGeoidLevel(), rootNode);
        waterDefs.initWater(WATER, fullCompatibility);

        // Camera
        initCamera();
        // Physics
        bulletState = new BulletAppState();
        stateManager.attach(bulletState);
        setupTerrainPhysics();
        setupPlayerPhysics();
        // Commands
        setUpKeys(fullCompatibility);
        // GUI
        setHelpScreen(fullCompatibility);
        setTextPressSpace();
        app.getGuiViewPort().addProcessor(niftyDisplay);
        // vehicle path
        vehiclePath = new VehiclePath(bathyData, rootNode, assetManager, worldInfo);
        // Markers
        markersNode = new MarkersNode(assetManager, rootNode, worldInfo, true, markerObserver);

        // DEBUG
        new DebugStructures(rootNode, assetManager, bathyData, worldInfo);

        // mark to delete for Garbage Collector
        bathyData = null;
    }

    private void setupTerrainPhysics() {
        // bulletAppState.getPhysicsSpace().enableDebug(assetManager);
        // We set up collision detection for the scene by creating a
        // compound collision shape and a static RigidBodyControl with mass zero.
        CollisionShape sceneShape = CollisionShapeFactory.createMeshShape(terrainDefs.getTerrain());
        RigidBodyControl landscape = new RigidBodyControl(sceneShape, 0);
        terrainDefs.getTerrain().addControl(landscape);
        bulletState.getPhysicsSpace().add(landscape);
    }

    private void setupPlayerPhysics() {
        // We set up collision detection for the player by creating a capsule collision shape and a CharacterControl.
        // The CharacterControl offers extra settings for size, stepheight, jumping, falling, and gravity.
        // We also put the player in its starting position.
        CapsuleCollisionShape capsuleShape = new CapsuleCollisionShape(1.5f, 6f, 1);
        player = new CharacterControl(capsuleShape, 0.05f);
        player.setGravity(0);
        player.setPhysicsLocation(this.app.getCamera().getLocation());
        bulletState.getPhysicsSpace().add(player);
    }

    private void initCamera() {
        // bigger frustrum because of depth
        // app.getCamera().setFrustumPerspective(45f, (float) app.getCamera().getWidth() / app.getCamera().getHeight(),
        // 1f, 1400f);
         app.getCamera().setLocation(camLocation);
         app.getCamera().lookAtDirection(CAM_DIRECTION.normalizeLocal(), Vector3f.UNIT_Y);
    }

    @Override
    public void cleanup() {
        super.cleanup();
    }

    @Override
    public void setEnabled(boolean enabled) {
      // Pause and unpause
      super.setEnabled(enabled);
      if(enabled){
        // init stuff that is in use while this state is RUNNING
            // this.app.getRootNode().attachChild(getX()); // modify scene graph...
            // this.app.doSomethingElse(); // call custom methods...
      } else {
        // take away everything not needed while this state is PAUSED
            // ...
      }
    }

    /**
     * Only the camera movement works by update
     */
    @Override
    public void update(float tpf) {
        // For testing
        // JmeCanvasContext canvasContext = (JmeCanvasContext) app.getContext();
        // Canvas canvas = canvasContext.getCanvas();
        // PerformanceTest.printToLog(PrintType.UPDATE, canvas.getHeight(), canvas.getWidth());
        if (isEnabled()) {
            updateMovement(tpf);
            markersNode.updateMarkers(app.getGuiNode(), app.getCamera());
        }
        else {
            // do the following while game is PAUSED, e.g. play an idle animation.
            // ...
        }
    }

    private void updateMovement(float tpf) {
        if (left || right || forward || backwards) {
            camSpeed += tpf * 2;
        }
        // get the position of the camera
        Vector3f camDir = app.getCamera().getDirection().clone().multLocal(camSpeed);
        Vector3f camLeft = app.getCamera().getLeft().clone().multLocal(camSpeed);
        // adjust position of rigid body for collisions
        walkDirection.set(0, 0, 0);
        if (left) {
            walkDirection.addLocal(camLeft);
        }
        if (right) {
            walkDirection.addLocal(camLeft.negate());
        }
        if (forward) {
            walkDirection.addLocal(camDir);
        }
        if (backwards) {
            walkDirection.addLocal(camDir.negate());
        }
        player.setWalkDirection(walkDirection);
        app.getCamera().setLocation(player.getPhysicsLocation());
    }

    /**
     * We over-write some navigational key mappings here, so we can add physics-controlled walking and jumping:
     */
    private void setUpKeys(boolean allCapabilitiesAvailable) {
        // Remove zoom with scroll btn
        inputManager.deleteMapping("FLYCAM_ZoomIn");
        inputManager.deleteMapping("FLYCAM_ZoomOut");
        // Moving
        inputManager.addMapping(Actions.LEFT.name(), new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping(Actions.RIGHT.name(), new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping(Actions.UP.name(), new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping(Actions.DOWN.name(), new KeyTrigger(KeyInput.KEY_S));
        inputManager.addListener(this, Actions.LEFT.name());
        inputManager.addListener(this, Actions.RIGHT.name());
        inputManager.addListener(this, Actions.UP.name());
        inputManager.addListener(this, Actions.DOWN.name());
        // Toggle objects on screen
        if (allCapabilitiesAvailable) {
            inputManager.addMapping(Actions.T_TERRAIN.name(), new KeyTrigger(KeyInput.KEY_2));
            inputManager.addListener(this, Actions.T_TERRAIN.name());
        }
        // Only enable if there is data for the water height
        if (!allCapabilitiesAvailable) {
            inputManager.addMapping(Actions.T_PLANES.name(), new KeyTrigger(KeyInput.KEY_1));
            inputManager.addListener(this, Actions.T_PLANES.name());
        }
        else if (worldInfo.getWaterLvl() == BathymetryLogInfo.INVALID_HEIGHT) {
            inputManager.addMapping(Actions.T_GEOID.name(), new KeyTrigger(KeyInput.KEY_1));
            inputManager.addListener(this, Actions.T_GEOID.name());
        }
        else {
            inputManager.addMapping(Actions.T_WATER.name(), new KeyTrigger(KeyInput.KEY_1));
            inputManager.addListener(this, Actions.T_WATER.name());
        }
        inputManager.addMapping(Actions.T_SKY.name(), new KeyTrigger(KeyInput.KEY_3));
        inputManager.addMapping(Actions.T_VEHICLE_PATH.name(), new KeyTrigger(KeyInput.KEY_4));
        inputManager.addMapping(Actions.T_HELP.name(), new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addListener(this, Actions.T_SKY.name());
        inputManager.addListener(this, Actions.T_VEHICLE_PATH.name());
        // for custom mesh development
        // inputManager.addMapping(Actions.T_VEHICLE_PATH_OPTIMIZED.name(), new KeyTrigger(KeyInput.KEY_5));
        // inputManager.addListener(this, Actions.T_VEHICLE_PATH_OPTIMIZED.name());
        inputManager.addListener(this, Actions.T_HELP.name());

    }

    /**
     * These are our custom actions triggered by key presses. We do not walk yet, we just keep track of the direction
     * the user pressed.
     */
    @Override
    public void onAction(String name, boolean keyPressed, float tpf) {
        if (name.equals(Actions.LEFT.name())) {
            left = keyPressed;
            if (!keyPressed) {
                camSpeed = STARTING_CAM_SPEED;
            }
        }
        else if (name.equals(Actions.RIGHT.name())) {
            right = keyPressed;
            if (!keyPressed) {
                camSpeed = STARTING_CAM_SPEED;
            }
        }
        else if (name.equals(Actions.UP.name())) {
            forward = keyPressed;
            if (!keyPressed) {
                camSpeed = STARTING_CAM_SPEED;
            }
        }
        else if (name.equals(Actions.DOWN.name())) {
            backwards = keyPressed;
            if (!keyPressed) {
                camSpeed = STARTING_CAM_SPEED;
            }
        }
        else if (name.equals(Actions.T_SKY.name()) && !keyPressed) {
            sky.toggleSky();
        }
        else if (name.equals(Actions.T_WATER.name()) && !keyPressed) {
            waterDefs.toggleWater();
        }
        else if (name.equals(Actions.T_GEOID.name()) && !keyPressed) {
            waterDefs.toggleGeoid();
        }
        else if (name.equals(Actions.T_PLANES.name()) && !keyPressed) {
            waterDefs.togglePlanes();
        }
        else if (name.equals(Actions.T_TERRAIN.name()) && !keyPressed) {
            terrainDefs.toggleTerrain();
        }
        else if (name.equals(Actions.T_HELP.name()) && !keyPressed) {
            toggleHelpScreen();
        }
        else if (name.equals(Actions.T_VEHICLE_PATH.name()) && !keyPressed) {
            vehiclePath.togglePath();
        }
        else if (name.equals(Actions.T_VEHICLE_PATH_OPTIMIZED.name()) && !keyPressed) {
            vehiclePath.togglePathOptimized();
        }
    }

    private void toggleHelpScreen() {
        showHelp = !showHelp;
        if (showHelp) {
            niftyDisplay.getNifty().gotoScreen(helpScreenName);
        }
        else {
            niftyDisplay.getNifty().gotoScreen("start");
        }

        if (resizeOnScreenSwitch) {
            resizeHelpScreen();
            resizeOnScreenSwitch = false;
        }
    }

    private void setHelpScreen(boolean showAll) {
        if (niftyDisplay == null) {
            try {
                niftyDisplay = new NiftyJmeDisplay(assetManager, inputManager, app.getAudioRenderer(), app.getGuiViewPort());
            }
            catch (Exception e) {
                // TODO Sometimes this generates a org.bushe.swing.event.EventServiceExistsException: An event service
                // by the name NiftyEventBusalready exists. Perhaps multiple threads tried to create a service about the
                // same time? error
                NeptusLog.pub().warn(e.getMessage(), e);
            }
        }
        Nifty nifty = niftyDisplay.getNifty();
        JmeCanvasContext canvasContext = (JmeCanvasContext) app.getContext();
        Canvas canvas = canvasContext.getCanvas();
        HelpScreenController helpScreenNiftyController = new HelpScreenController(bathyData.getWaterHeight(),
                worldInfo.getGeoidHeightM(), bathyData.getMinWaterColumn(), bathyData.getMaxWaterColumn(),
                canvas.getWidth(),
                canvas.getHeight());
        if (showAll) {
            helpScreenName = "helpScreen";
        }
        else {
            helpScreenName = "helpScreenLowSpecs";
        }
        nifty.fromXml(ASSET_PATH.I_HELP_SCREEN.relativePath, helpScreenName, helpScreenNiftyController);
    }

    private void setTextPressSpace() {
        Nifty nifty = niftyDisplay.getNifty();
        nifty.fromXml(ASSET_PATH.I_HELP_SCREEN.relativePath, "start");
    }

    public void resizeHelpScreen() {
        try {
            JmeCanvasContext canvasContext = (JmeCanvasContext) app.getContext();
            Canvas canvas = canvasContext.getCanvas();
            niftyDisplay.reshape(app.getGuiViewPort(), canvas.getWidth(), canvas.getHeight());
            resizeOnScreenSwitch = true;
        }
        catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }

}