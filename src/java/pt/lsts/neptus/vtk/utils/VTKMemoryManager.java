/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: hfq
 * Jun 4, 2013
 */
package pt.lsts.neptus.vtk.utils;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import vtk.vtkJavaGarbageCollector;
import vtk.vtkObject;
import vtk.vtkObjectBase;

/**
 * @author hfq
 * 
 */
@SuppressWarnings("unchecked")
public final class VTKMemoryManager {
    private final static Logger LOGGER = Logger.getLogger(VTKMemoryManager.class.getName());

    public final static vtkJavaGarbageCollector GC;
    private final static Method DELETE_ALL_METHOD;
    private final static Object OBJECT_MANAGER;
    private final static Method UNREGISTER_METHOD;
    private final static ConcurrentHashMap<Long, WeakReference<?>> OBJECT_MAP;

    static {
        vtkJavaGarbageCollector lGC = null;
        Method lDeleteAll = null, lUnregisterMethod = null;
        Object lObjectManager = null;
        ConcurrentHashMap<Long, WeakReference<?>> lObjectMap = null;

        try {
            try {
                Class<?> javaHash = Class.forName("vtk.vtkGlobalJavaHash");
                Field f = javaHash.getDeclaredField("GarbageCollector");
                lGC = (vtkJavaGarbageCollector) f.get(null);
                lDeleteAll = javaHash.getDeclaredMethod("GC");
                lObjectMap = (ConcurrentHashMap<Long, WeakReference<?>>) javaHash.getDeclaredField("PointerToReference").get(null);

            }
            catch (ClassNotFoundException ex) {
                Field f = vtkObject.class.getDeclaredField("JAVA_OBJECT_MANAGER");
                lObjectManager = f.get(null);
                Method m = lObjectManager.getClass().getDeclaredMethod("getAutoGarbageCollector");
                lGC = (vtkJavaGarbageCollector) m.invoke(lObjectManager);
                lDeleteAll = lObjectManager.getClass().getDeclaredMethod("deleteAll");
                lUnregisterMethod = lObjectManager.getClass().getDeclaredMethod("unRegisterJavaObject", Long.class);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
            LOGGER.log(Level.SEVERE, null, ex);
        }

        GC = lGC;
        DELETE_ALL_METHOD = lDeleteAll;
        OBJECT_MANAGER = lObjectManager;
        UNREGISTER_METHOD = lUnregisterMethod;
        OBJECT_MAP = lObjectMap;
    }

    private VTKMemoryManager() {

    }

    public static void deleteAll() {
        try {
            DELETE_ALL_METHOD.invoke(OBJECT_MANAGER);
        }
        catch (IllegalAccessException e) {
            e.printStackTrace();
            LOGGER.log(Level.SEVERE, null, e);
        }
        catch (IllegalArgumentException e) {
            e.printStackTrace();
            LOGGER.log(Level.SEVERE, null, e);
        }
        catch (InvocationTargetException e) {
            e.printStackTrace();
            LOGGER.log(Level.SEVERE, null, e);
        }
    }

    /**
     * Tag a vtkObjectBase so it will be deleted at the native level at the next vtkGlobalJavaHash.GarbageCollector()
     * call. Such call is normally triggered by vtkJavaGarbageCollector As JVM doesn't monitor the native memory usage,
     * VTK object may never be deleted if the Java garbage collector is not triggered by other Java object creation.
     * This methods allows to mannually tag a vtkObject as removable without actually removing it
     * 
     * @param o
     */
    public static void delete(vtkObjectBase o) {
        if (OBJECT_MAP != null) {
            WeakReference<?> ref = OBJECT_MAP.get(o.GetVTKId());
            ref.clear();
        }
        else { // OBJECT_MANAGER != null
            try {
                UNREGISTER_METHOD.invoke(OBJECT_MANAGER, o.GetVTKId());
            }
            catch (IllegalAccessException e) {
                e.printStackTrace();
                LOGGER.log(Level.SEVERE, null, e);
            }
            catch (IllegalArgumentException e) {
                e.printStackTrace();
                LOGGER.log(Level.SEVERE, null, e);
            }
            catch (InvocationTargetException e) {
                e.printStackTrace();
                LOGGER.log(Level.SEVERE, null, e);
            }
        }
    }
}
