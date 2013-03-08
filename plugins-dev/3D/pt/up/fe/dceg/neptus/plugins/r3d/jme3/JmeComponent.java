package pt.up.fe.dceg.neptus.plugins.r3d.jme3;

import java.awt.Canvas;
import java.awt.Rectangle;
import java.util.EnumSet;
import java.util.logging.Level;

import org.lwjgl.LWJGLException;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.mra.importers.IMraLogGroup;
import pt.up.fe.dceg.neptus.plugins.r3d.BadDrivers;
import pt.up.fe.dceg.neptus.plugins.r3d.BadDriversEvent;
import pt.up.fe.dceg.neptus.plugins.r3d.Bathymetry3DGenerator;
import pt.up.fe.dceg.neptus.plugins.r3d.MarkerObserver;
import pt.up.fe.dceg.neptus.plugins.r3d.NoBottomDistanceEntitiesException;
import pt.up.fe.dceg.neptus.plugins.r3d.dto.BathymetryLogInfo;
import pt.up.fe.dceg.plugins.tidePrediction.Harbors;

import com.jme3.app.SimpleApplication;
import com.jme3.renderer.Caps;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeCanvasContext;

/**
 * Object to serve as entry point for the 3D engine (jMonkey3)
 * 
 * @author Margarida Faria
 * 
 */
public class JmeComponent extends SimpleApplication {
    public static final boolean TEST_CURVE = false;
    private JmeCanvasContext ctx;
    private final IMraLogGroup source;
    private final BadDrivers badDrivers;
    private final MarkerObserver markerObserver;
    private final Harbors harbor;

    /**
     * Setup the canvas object and adds it to the panel
     * 
     * @param source
     * @param badDrivers
     * @param markerObserver
     * @param harbor
     */
    public JmeComponent(IMraLogGroup source, BadDrivers badDrivers, MarkerObserver markerObserver, Harbors harbor) {
        super();
        // createApplicationAndCanvas(SIZE_CANVAS_INSIDE_NEPTUS, SIZE_CANVAS_INSIDE_NEPTUS);
        this.source = source;
        this.badDrivers = badDrivers;
        this.markerObserver = markerObserver;
        this.harbor = harbor;
    }

    @Override
    public void handleError(String errMsg, Throwable t){
        if (t instanceof LWJGLException) {
            errMsg = I18n.text("Probably bad graphics drivers. ") + errMsg;
            fireNoSupport(I18n.text("Graphic card is not enough for this plugin."));
        }
        NeptusLog.pub().error("----->" + errMsg, t);
        fireNoSupport(errMsg);
        stop();
    }

    /**
     * Call the startCanvas and adds the runnable to EventQueue
     * 
     * @return panel with jME inside
     */
    public Canvas getComponentAndStartIt() {
        // java.awt.EventQueue.invokeLater(new Runnable() {
        // @Override
        // public void run() {
                startCanvas();
        // }
        // });
        return ctx.getCanvas();
    }

    /**
     * Setup the canvas object and adds it to the panel
     * 
     * @param rectangle
     */
    // private void createApplicationAndCanvas() {
    // AppSettings settings = new AppSettings(true);
    // settings.setWidth(SIZE_CANVAS_INSIDE_NEPTUS);
    // settings.setHeight(SIZE_CANVAS_INSIDE_NEPTUS);
    // settings.setAudioRenderer(null);
    // java.util.logging.Logger.getLogger("").setLevel(Level.WARNING);
    // setSettings(settings);
    // setShowSettings(false);
    // setDisplayStatView(false);
    // setDisplayFps(false);
    // createCanvas();
    // ctx = (JmeCanvasContext) getContext();
    // ctx.setSystemListener(this);
    // }
    public void createApplicationAndCanvas(Rectangle rectangle) {
        AppSettings settings = new AppSettings(true);
        settings.setWidth(rectangle.width);
        settings.setHeight(rectangle.height);
        settings.setAudioRenderer(null);
        setSettings(settings);
        java.util.logging.Logger.getLogger("").setLevel(Level.WARNING);
        setShowSettings(false);
        setPauseOnLostFocus(false);
        setDisplayStatView(false);
        setDisplayFps(false);
        createCanvas();
        // startCanvas();
        JmeCanvasContext context = (JmeCanvasContext) getContext();
        context.getCanvas().setSize(rectangle.width, rectangle.height);
    }

    /**
     * The overriden method from jME where all initialization is done
     */
    @Override
    public void simpleInitApp() {
        // check for minimal driver capabilities
        EnumSet<Caps> caps = getRenderer().getCaps();
        if (!caps.contains(Caps.GLSL100)) {
            fireNoSupport(I18n.text("Graphical card does not support GLSL100, usually introduced in openGL 2.0."));
        }
        // // Validation of tide prediction correction
        // ValidateTideCorrection validate = new ValidateTideCorrection(source, harbor);
        // try {
        // validate.printRelevantData();
        // }
        // catch (Exception e2) {
        // // TODO Auto-generated catch block
        // e2.printStackTrace();
        // }

        // Extract bathymery data from log
        Bathymetry3DGenerator bathyGen;
        BathymetryLogInfo bathyInfo = null;
        bathyGen = new Bathymetry3DGenerator(source, harbor);
        if (source.getLsfIndex().getDefinitions().getVersion().compareTo("5.0.0") >= 0) {
            try {
                bathyInfo = bathyGen.extractBathymetryInfoIMC5(true);
            }
            catch (Exception e) {
                try {
                    bathyInfo = bathyGen.extractBathymetryInfoIMC5(false);
                }
                catch (Exception e1) {
                    // This should never happen because call with false doe not use methods that generate exception
                    handleError("", e1);
                    e1.printStackTrace();
                }
            }
        }
        else {
            try {
                if (TEST_CURVE) {
                    bathyGen.extractBathymetryInfoRawIMC4();
                }
                bathyInfo = bathyGen.extractBathymetryInfoIMC4();
            }
            catch (NoBottomDistanceEntitiesException e1) {
                fireNoSupport(e1.getMessage());
                return;
            }
        }
        // Check for enough data to generate graph
        if (bathyInfo.getDepthVec().size() < 2 || bathyInfo.getEastVec().size() < 2) {
            fireNoSupport(I18n.text("There is not enough data to generate a graph."));
            return;
        }
        flyCam.setDragToRotate(true);
        // Don't pause even if focus is lost
        setPauseOnLostFocus(false);
        // load appState
        ShowBathymetryState bathySate = new ShowBathymetryState(bathyInfo, markerObserver);
        stateManager.attach(bathySate);
    }

    private void fireNoSupport(String msg) {
        NeptusLog.pub().info(msg);
        badDrivers.fireBadDrivers(new BadDriversEvent(msg));
        stop();
    }

    /**
     * The overriden method from jME where all update is done
     */
    @Override
    public void simpleUpdate(float tpf) {

    }

    /**
     * Resizes the jME canvas
     * 
     * @param bounds of new canvas
     */
    public void resize(Rectangle bounds) {
        ((JmeCanvasContext) getContext()).getCanvas().setBounds(bounds);
        ShowBathymetryState dataState = stateManager.getState(ShowBathymetryState.class);
        if (dataState != null) {
            dataState.resizeHelpScreen();
        }
    }

    // just to test
    // public static void main(String args[]) {
    // JFrame frame = new JFrame();
    // JmeComponent comp = new
    // JmeComponent("/home/meg/workspace/Neptus/plugins-dev/3D/assets/Textures/Terrain/splat/colorBathyForHeightMap.jpg");
    //
    // frame.setLayout(new MigLayout());
    // frame.setVisible(true);
    // frame.setSize(800, 600);
    // frame.add(comp.panel);
    // frame.repaint();
    //
    // frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    // }
}
