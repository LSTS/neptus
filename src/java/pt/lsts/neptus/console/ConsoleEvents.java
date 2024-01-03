/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
 * Modified European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the Modified EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://github.com/LSTS/neptus/blob/develop/LICENSE.md
 * and http://ec.europa.eu/idabc/eupl.html.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: José Pinto
 * Nov 18, 2012
 */
package pt.lsts.neptus.console;

import com.google.common.eventbus.Subscribe;

import pt.lsts.imc.CcuEvent;
import pt.lsts.imc.LogBookEntry;
import pt.lsts.imc.MapFeature;
import pt.lsts.imc.MapPoint;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.MapGroup;
import pt.lsts.neptus.types.map.MarkElement;
import pt.lsts.neptus.types.mission.plan.PlanType;

/**
 * @author zp
 *
 */
@PluginDescription(name="CCU Event Handler")
public class ConsoleEvents extends ConsolePanel {

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
                            loc.setLatitudeDegs(Math.toDegrees(point.getLat()));
                            loc.setLongitudeDegs(Math.toDegrees(point.getLon()));
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
     * @see pt.lsts.neptus.plugins.SimpleSubPanel#initSubPanel()
     */
    @Override
    public void initSubPanel() {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.SimpleSubPanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
        // TODO Auto-generated method stub
        
    }
}
