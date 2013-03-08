package pt.up.fe.dceg.neptus.plugins.r3d.jme3;

import java.text.DecimalFormat;

import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.plugins.r3d.dto.BathymetryLogInfo;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.screen.ScreenController;

/**
 * Controller to load dynamic data on the GUI
 * 
 * @author meg
 */
public class HelpScreenController implements ScreenController {
    public static final float WASD_HEIGHT_SPACE_PROPORTION = 0.065f;
    public static final int WASD_HEIGTH = 116;// 141;
    public static final int WASD_WIDTH = 250;// 303;
    public static final float MOUSE_HEIGHT_SPACE_PROPORTION = 0.12f;
    public static final int MOUSE_HEIGTH = 222;// 335;// 235;
    public static final int MOUSE_WIDTH = 246;// 372;// 264;

    private final float waterLevel, geoidLevel, maxHeight, minHeight;
    private final DecimalFormat df = new DecimalFormat("#.##");

    private final int screenHeight, screenWidth;

    /** custom methods */

    /**
     * Calculate height for the picture with the keyboard keys
     * 
     * @return
     */
    public String getWASDHeightPx() {
        int availableHeight = (int) (screenHeight * WASD_HEIGHT_SPACE_PROPORTION);
        return availableHeight+"px";
    }

    /**
     * Calculate height for the picture with the mouse
     * 
     * @return
     */
    public String getMouseHeightPx() {
        // calc desired pixels
        int availableHeight = (int) (screenHeight * MOUSE_HEIGHT_SPACE_PROPORTION);
        return availableHeight + "px";
    }

    /**
     * Calculate width for the picture with the mouse
     * 
     * @return
     */
    public String getMouseWidthPx() {
        float imageRatio = MOUSE_WIDTH / MOUSE_HEIGTH;  
        int newWidth = calcNewWidth(imageRatio, MOUSE_HEIGHT_SPACE_PROPORTION);
        return newWidth+"px";
    }

    /**
     * Calculate width for the picture with the keyboard keys
     * 
     * @return
     */
    public String getWASDWidthPx() {
        float imageRatio = WASD_WIDTH / WASD_HEIGTH;
        int newWidth = calcNewWidth(imageRatio, WASD_HEIGHT_SPACE_PROPORTION);
        return newWidth + "px";
    }

    private int calcNewWidth(float imageRatio, float heightProportion) {
        float availableHeight = (screenHeight * heightProportion);
        return (int) (availableHeight * imageRatio);
    }

    /** Nifty GUI ScreenControl methods */
    @Override
    public void bind(Nifty nifty, Screen screen) {
    }

    /**
     * Constructor with values for the dynamic parts of GUI
     * 
     * @param waterLevel
     * @param geoidLevel
     * @param maxHeight
     * @param minHeight
     * @param screenHeight
     * @param screenWidth
     */
    public HelpScreenController(float waterLevel, float geoidLevel, float maxHeight, float minHeight,
            int screenHeight, int screenWidth) {
        super();
        this.waterLevel = waterLevel;
        this.geoidLevel = geoidLevel;
        this.maxHeight = maxHeight;
        this.minHeight = minHeight;
        this.screenHeight = screenHeight;
        this.screenWidth = screenWidth;
    }

    /**
     * @return the waterLevel rounded to 2 decimal places
     */
    public String getWaterLvlFormated() {
        if (waterLevel == BathymetryLogInfo.INVALID_HEIGHT) {
            return I18n.text("Water heigth unavailable");
        }
        else {
            return df.format(waterLevel);
        }
    }

    /**
     * @return the geoidLevel rounded to 2 decimal places
     */
    public String getGeoidLvlFormated() {
        return df.format(geoidLevel);
    }

    /**
     * @return the maxTerrainHeigth rounded to 2 decimal places
     */
    public String getMaxTerrainHeigthFormated() {
        return df.format(minHeight);
    }

    /**
     * @return the waterLevel rounded to 2 decimal places
     */
    public String getMinTerrainHeigthFormated() {
        return df.format(maxHeight);
    }

    /**
     * @return the waterLevel
     */
    public float getWaterLevel() {
        return waterLevel;
    }

    /**
     * @return the geoidLevel
     */
    public float getGeoidLevel() {
        return geoidLevel;
    }

    /**
     * @return the maxHeight
     */
    public float getMaxHeight() {
        return maxHeight;
    }

    /**
     * @return the minHeight
     */
    public float getMinHeight() {
        return minHeight;
    }

    /**
     * @return the screenHeight
     */
    public int getScreenHeight() {
        return screenHeight;
    }

    /**
     * @return the screenWidth
     */
    public int getScreenWidth() {
        return screenWidth;
    }

    public String getPressSpaceForMoreInfoText() {
        return I18n.text("Press space for more information");
    }

    public String getForwardText() {
        return I18n.text("Forward");
    }

    public String getLeftText() {
        return I18n.text("Left");
    }

    public String getRightText() {
        return I18n.text("Right");
    }

    public String getBackText() {
        return I18n.text("Back");
    }

    public String getCameraDirectionText() {
        return I18n.text("Camera direction");
    }

    public String getToggleWaterDisplayText() {
        return I18n.text("1 Toggle water display");
    }

    public String getToggleTerrainColorText() {
        return I18n.text("2 Toggle terrain color");
    }

    public String getToggleOnOffTheSkyText() {
        return I18n.text("3 Toggle on/off the sky");
    }

    public String getWaterLevelText() {
        return I18n.text("Water level");
    }

    public String getGeoidLevelText() {
        return I18n.text("Geoid level");
    }

    public String getMaximumTerrainHeightText() {
        return I18n.text("Maximum terrain height");
    }

    public String getMinimumTerrainHeightText() {
        return I18n.text("Minimum terrain height");
    }

    @Override
    public void onStartScreen() {
    }

    @Override
    public void onEndScreen() {
    }

    
}
