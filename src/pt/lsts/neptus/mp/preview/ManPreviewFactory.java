/*
 * Copyright (c) 2004-2017 Universidade do Porto - Faculdade de Engenharia
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
 * http://ec.europa.eu/idabc/eupl.html.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: José Pinto
 * Oct 11, 2011
 */
package pt.lsts.neptus.mp.preview;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.mp.Maneuver;
import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.mp.maneuvers.FollowTrajectory;
import pt.lsts.neptus.mp.maneuvers.Launch;

/**
 * @author zp
 * @author pdias
 */
public class ManPreviewFactory {

    private static Map<Class<Maneuver>, Class<IManeuverPreview<?>>> previewMatchList = Collections.synchronizedMap(new HashMap<>());
    
    /**
     * If finds and instantiate a preview for the maneuver. It looks for a preview in the current package of the
     * maneuver class and in the sibling package "preview". The name of the preview class should be the name of the
     * maneuver class suffixed by the "Preview" string.
     * 
     * @param maneuver The maneuver to preview.
     * @param vehicleId The vehicle ID.
     * @param state The vehicle current {@link SystemPositionAndAttitude}.
     * @param manState The current maneuver state.
     * @return
     */
    @SuppressWarnings("unchecked")
    public static IManeuverPreview<?> getPreview(Maneuver maneuver, String vehicleId, SystemPositionAndAttitude state,
            Object manState) {
        if (maneuver == null)
            return null;

        synchronized (previewMatchList) {
            if (previewMatchList.containsKey(maneuver.getClass())) {
                Class<IManeuverPreview<?>> prevClass = previewMatchList.get(maneuver.getClass());
                if (prevClass == null)
                    return null;
                
                try {
                    IManeuverPreview<Maneuver> prevG = ((IManeuverPreview<Maneuver>) prevClass.newInstance());
                    prevG.init(vehicleId, maneuver, state, manState);
                    return prevG;
                }
                catch (Exception e) {
                    NeptusLog.pub().error(e.getMessage());
                    return null;
                }
            }
            
            if (FollowTrajectory.class.isAssignableFrom(maneuver.getClass())) {
                FollowTrajectoryPreview prev = new FollowTrajectoryPreview();
                prev.init(vehicleId, (FollowTrajectory) maneuver, state, manState);
                IManeuverPreview<?> p = (IManeuverPreview<?>) prev;
                previewMatchList.put((Class<Maneuver>) maneuver.getClass(), (Class<IManeuverPreview<?>>) p.getClass());
                return prev;
            }
            else if (Launch.class.isAssignableFrom(maneuver.getClass())) {
                GotoPreview prev = new GotoPreview();
                prev.init(vehicleId, (Launch) maneuver, state, manState);
                IManeuverPreview<?> p = (IManeuverPreview<?>) prev;
                previewMatchList.put((Class<Maneuver>) maneuver.getClass(), (Class<IManeuverPreview<?>>) p.getClass());
                return prev;
            }
            
            String pkn = maneuver.getClass().getPackage().getName();
            ClassLoader cl = ClassLoader.getSystemClassLoader();
            Class<?> prevClass = null;
            try {
                prevClass = cl.loadClass(pkn.replaceAll("\\.[A-Za-z0-9]+$", "") + ".preview."
                        + maneuver.getClass().getSimpleName() + "Preview");
            }
            catch (ClassNotFoundException e) {
                try {
                    prevClass = cl.loadClass(pkn + "." + maneuver.getClass().getSimpleName() + "Preview");
                }
                catch (Exception e1) {
                    NeptusLog.pub().error(e1.getMessage());
                }
            }
            catch (Exception e) {
                NeptusLog.pub().error(e.getMessage());
            }
            
            if (prevClass != null) {
                try {
                    IManeuverPreview<Maneuver> prevG = (IManeuverPreview<Maneuver>) prevClass.newInstance();
                    prevG.init(vehicleId, maneuver, state, manState);
                    previewMatchList.put((Class<Maneuver>) maneuver.getClass(), (Class<IManeuverPreview<?>>) prevG.getClass());
                    return prevG;
                }
                catch (Exception e) {
                    NeptusLog.pub().error(e.getMessage());
                }
            }
            
            previewMatchList.put((Class<Maneuver>) maneuver.getClass(), (Class<IManeuverPreview<?>>) null);
            return null;
        }
    }
    
    /**
     * Utility class to register a preview for a maneuver.
     * Only if preview is not register or register as null is register.
     * 
     * @param maneuver
     * @param preview
     * @return
     */
    public static boolean registerPreview(Class<Maneuver> maneuver, Class<IManeuverPreview<?>> preview) {
        if (maneuver == null || preview == null)
            return false;
        
        synchronized (previewMatchList) {
            if (previewMatchList.containsKey(maneuver)) {
                if (previewMatchList.get(maneuver) == null) {
                    previewMatchList.put(maneuver, preview);
                    return true;
                }
                else {
                    return false;
                }
            }
            else {
                previewMatchList.put(maneuver, preview);
                return true;
            }
        }
    }
}