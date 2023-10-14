/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
 * Author: Paulo Dias
 * 12/Abr/2005
 */
package pt.lsts.neptus.types.coord;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Vector;

/**
 * @author Paulo Dias
 */
public class LocationsHolder {

    /**
     * <code>locationsList</code> is of type {@link LocationType}. LinkedHashMap<String id, AbstractLocationPoint>
     */
    private static LinkedHashMap<String, LocationType> locationsList = new LinkedHashMap<String, LocationType>();

    /**
     * <code>refsList</code> List of references of a location HashMap<String id, Vector<String ids>>
     */
    private static HashMap<String, Vector<String>> refsList = new HashMap<String, Vector<String>>();

    /**
     * @param point
     * @return
     */
    public static boolean putAbstractLocationPoint(LocationType point) {
        Object retO = locationsList.get(point.getId());
        if (retO != null)
            return false;
        locationsList.put(point.getId(), point);
        refsList.put(point.getId(), new Vector<String>());
        return true;
    }

    /**
     * @param parentId
     * @param childId
     * @return
     */
    public static boolean addRefToAbstractLocationPoint(String parentId, String childId) {
        LocationType retO = locationsList.get(parentId);
        if (retO == null)
            return false;
        retO = locationsList.get(childId);
        if (retO == null)
            return false;
        Vector<String> ret1 = refsList.get(parentId);
        if (ret1 == null) {
            refsList.put(parentId, new Vector<String>());
            ret1 = refsList.get(parentId);
        }
        Vector<String> refs = ret1;
        refs.add(childId);
        return true;
    }

    /**
     * @param parentId
     * @param childId
     * @return
     */
    public static boolean removeRefToAbstractLocationPoint(String parentId, String childId) {
        LocationType retO = locationsList.get(parentId);
        if (retO == null)
            return false;
        retO = locationsList.get(childId);
        if (retO == null)
            return false;
        Vector<String> ret1 = refsList.get(parentId);
        if (ret1 == null)
            return false;
        Vector<String> refs = ret1;
        refs.remove(childId);
        return true;
    }

    /**
     * @param id
     * @return
     */
    public static LocationType getAbstractLocationPointById(String id) {
        LocationType ret = locationsList.get(id);
        if (ret == null)
            return null;
        // AbstractLocationPoint ret = (AbstractLocationPoint) retO;
        return ret;
    }

    /**
     * @param point
     * @return
     */
    public static boolean updateAbstractLocationPoint(LocationType point) {
        LocationType retO = locationsList.get(point.getId());
        if (retO == null)
            return false;
        else {
            locationsList.remove(point.getId());
            locationsList.put(point.getId(), point);
            return true;
        }
    }

    /**
     * @param point
     * @return
     */
    public static boolean removeAbstractLocationPoint(LocationType point) {
        return removeAbstractLocationPoint(point.getId());
    }

    /**
     * @param id
     * @return
     */
    public static boolean removeAbstractLocationPoint(String id) {
        LocationType retO = locationsList.get(id);
        if (retO == null)
            return false;
        else {
            Vector<String> re = refsList.get(id);
            if (re == null) {
                locationsList.remove(id);
                return true;
            }
            Vector<String> refs = re;
            if (refs.size() == 0) {
                locationsList.remove(id);
                refsList.remove(id);
                return true;
            }
            else
                return false;
        }
    }

    /**
     * @return
     */
    public static String generateReport() {
        String report = "Abstract locations:\n";
        Iterator<String> it = locationsList.keySet().iterator();
        while (it.hasNext()) {
            LocationType point = locationsList.get((String) it.next());
            report += point.getName() + " (" + point.getId() + ")\n";
        }
        report += "\n\n";
        report += "References list:\n";
        it = refsList.keySet().iterator();
        while (it.hasNext()) {
            String id = (String) it.next();
            Vector<String> refs = refsList.get(id);
            report += "Id: " + id + "\n";
            Iterator<String> it1 = refs.iterator();
            while (it1.hasNext()) {
                String ref = (String) it1.next();
                report += "   " + ref + "\n";
            }
            report += "\n";
        }
        return report;
    }

}
