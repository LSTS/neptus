/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by Paulo Dias
 * 12/Abr/2005
 * $Id:: LocationsHolder.java 9616 2012-12-30 23:23:22Z pdias             $:
 */
package pt.up.fe.dceg.neptus.types.coord;

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
