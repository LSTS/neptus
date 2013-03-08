/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by zp
 * Nov 18, 2012
 * $Id:: ConsoleEvents.java 9615 2012-12-30 23:08:28Z pdias                     $:
 */
package pt.up.fe.dceg.neptus.console;

import pt.up.fe.dceg.neptus.imc.CcuEvent;
import pt.up.fe.dceg.neptus.imc.LogBookEntry;
import pt.up.fe.dceg.neptus.imc.MapFeature;
import pt.up.fe.dceg.neptus.imc.MapPoint;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.SimpleSubPanel;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.types.map.MapGroup;
import pt.up.fe.dceg.neptus.types.map.MarkElement;
import pt.up.fe.dceg.neptus.types.mission.plan.PlanType;
import pt.up.fe.dceg.neptus.util.comm.IMCUtils;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcMsgManager;

import com.google.common.eventbus.Subscribe;

/**
 * @author zp
 *
 */
@PluginDescription(name="CCU Event Handler")
public class ConsoleEvents extends SimpleSubPanel {

    private static final long serialVersionUID = 1L;

    public ConsoleEvents(ConsoleLayout console) {
        super(console);
    }
    
    @Subscribe
    public void consume(CcuEvent event) {
        
        if (event.getSrc() == ImcMsgManager.getManager().getLocalId().intValue())
            return;
        
        switch (event.getType()) {
            case PLAN_ADDED:
            case PLAN_CHANGED:
                PlanType pt = IMCUtils.parsePlanSpecification(getConsole().getMission(), event.getArg());
                getConsole().getMission().getIndividualPlansList().put(event.getId(), pt);
                getConsole().getMission().save(false);
                getConsole().warnMissionListeners();
//                info(I18n.textf("Plan %planid, edited by %console is now updated.", event.getId(), event.getSourceName()));
                break;
            case PLAN_REMOVED:
                //getConsole().getMission().getIndividualPlansList().remove(event.getId());
                //getConsole().warnMissionListeners();
//                info(I18n.textf("Plan %planid was removed by %console.", event.getId(), event.getSourceName()));
                break;
            case LOG_ENTRY:
                try {
                    event.getArg(LogBookEntry.class);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case TELEOPERATION_ENDED:
//                info(I18n.textf("%console has ended teleoperation", event.getSourceName()));
                break;
            case TELEOPERATION_STARTED:
//                info(I18n.textf("%console has started teleoperation", event.getSourceName()));
                break;
            case MAP_FEATURE_ADDED:
                try {
                    MapFeature feature = event.getArg(MapFeature.class);
                    String fname = feature.getId();
                    MapGroup mg = MapGroup.getMapGroupInstance(getConsole().getMission());
                    if (mg.getMapObject(fname) != null)
                        return;
                    switch (feature.getFeatureType()) {
                        case POI:
                            MarkElement elem = new MarkElement(mg, mg.getMaps()[0]);
                            elem.setId(fname);
                            MapPoint point = feature.getFeature().firstElement();
                            LocationType loc = new LocationType();
                            loc.setLatitude(Math.toDegrees(point.getLat()));
                            loc.setLongitude(Math.toDegrees(point.getLon()));
                            loc.setHeight(point.getAlt());
                            elem.setCenterLocation(loc);
                            elem.getParentMap().addObject(elem);
                            getConsole().getMission().save(false);
                            getConsole().warnMissionListeners();
//                            info(I18n.textf("Feature $feature_id was added by %console.", event.getId(), event.getSourceName()));
                            break;
                        default:
                            break;
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            default:
                break;
        }
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.plugins.SimpleSubPanel#initSubPanel()
     */
    @Override
    public void initSubPanel() {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.plugins.SimpleSubPanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
        // TODO Auto-generated method stub
        
    }
}
