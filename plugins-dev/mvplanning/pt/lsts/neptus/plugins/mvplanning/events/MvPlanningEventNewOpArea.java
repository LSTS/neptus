package pt.lsts.neptus.plugins.mvplanning.events;

import pt.lsts.neptus.plugins.mvplanning.planning.mapdecomposition.GridArea;

/**
 * @author tsmarques
 * @date 29/06/16
 */
public class MvPlanningEventNewOpArea {
    private GridArea opArea;
    public MvPlanningEventNewOpArea(GridArea area) {
        opArea = area;
    }

    public GridArea getArea() {
        return opArea;
    }
}
