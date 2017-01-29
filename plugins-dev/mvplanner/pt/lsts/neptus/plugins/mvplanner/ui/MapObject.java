package pt.lsts.neptus.plugins.mvplanner.ui;

import pt.lsts.neptus.plugins.mvplanner.jaxb.Profile;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.coord.PolygonType;

import java.util.ArrayList;
import java.util.List;

public class MapObject {
    /** @see getPolygon() **/
    private PolygonType polygon;

    /** @see isObstacle() **/
    private boolean isObstacle;

    /** @see isOpArea **/
    private boolean isOpArea;

    private Profile profile = null;

    public MapObject(PolygonType pType) {
        polygon = pType;
        isObstacle = false;
        isOpArea = false;
    }

    /**
     * Get the polygon that represents this object
     * */
    public String getId() {
        return polygon.getId();
    }

    /**
     * Polygon that represents this object
     * */
    public PolygonType getPolygon() {
        return polygon;
    }

    /**
     * Get vertices locations
     */
    public final List<LocationType> getLocations() {
        List<LocationType> locations = new ArrayList<>();

        for(PolygonType.Vertex v : polygon.getVertices())
            locations.add(new LocationType(v.getLocation()));

        return locations;
    }

    /**
     * If this object represents a map obstacle
     * */
    public boolean isObstacle() {
        return isObstacle;
    }

    /**
     * If this object represents the area of operation
     * */
    public boolean isOpArea() {
        return isOpArea;
    }

    /**
     * If this object represents a task then
     * associate a profile to it
     * */
    public void associateProfile(Profile profile) {
        this.profile = profile;
    }

    public Profile getSelectedProfile() {
        return profile;
    }


    /**
     * Set this object as an obstacle
     * */
    public void setAsObstacle() {
        isObstacle = true;
    }


    /**
     * Set this object as the area of operation
     * */
    public void setAsOpArea() {
        isOpArea = true;
    }

    /**
     * Returns the number of points of this
     * object
     *
     * @return number of points
     * */
    public int getNumberofPoints() {
        return polygon.getVertices().size();
    }
}
