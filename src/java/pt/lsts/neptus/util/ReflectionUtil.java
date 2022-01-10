/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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
 * Author: 
 * 20??/??/??
 */
package pt.lsts.neptus.util;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Vector;
import java.util.jar.JarFile;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.mp.Maneuver;
import pt.lsts.neptus.mp.templates.AbstractPlanTemplate;
import pt.lsts.neptus.mra.visualizations.MRAVisualization;
import pt.lsts.neptus.plugins.MapTileProvider;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginsRepository;
import pt.lsts.neptus.renderer2d.tiles.Tile;

public class ReflectionUtil {

    public static List<Class<?>> getClassesForPackage(String pckgname) throws ClassNotFoundException {
        return getClassesForPackage(pckgname, true);
    }

    /**
     * Attempts to list all the classes in the specified package as determined by the context class loader
     * 
     * @param pckgname the package name to search
     * @return a list of classes that exist within that package
     * @throws ClassNotFoundException if something went wrong
     */
    public static List<Class<?>> getClassesForPackage(String pckgname, boolean recursive) throws ClassNotFoundException {
        // This will hold a list of directories matching the pckgname. There may be more than one if a package is split
        // over multiple jars/paths
        ArrayList<File> directories = new ArrayList<File>();
        ArrayList<Class<?>> classes = new ArrayList<Class<?>>();

        // NeptusLog.pub().info("<###>evaluating "+pckgname);

        try {
            ClassLoader cld = Thread.currentThread().getContextClassLoader();
            if (cld == null) {
                throw new ClassNotFoundException("Can't get class loader.");
            }
            String path = pckgname.replace('.', '/');
            // System.err.println(path);

            // Ask for all resources for the path
            Enumeration<URL> resources = cld.getResources(path);
            while (resources.hasMoreElements()) {
                String res = resources.nextElement().getPath();
                if (res.contains(".jar!")) {
                    // res = res.replaceAll("%20", " ");
                    // String path = res.substring(0, res.indexOf("r!")).replaceAll(".", replacement)
                    JarFile zf = new JarFile(URLDecoder.decode(res.substring(5, res.indexOf("!")), "UTF-8"));
                    Enumeration<?> enumer = zf.entries();
                    while (enumer.hasMoreElements()) {

                        String nextPath = enumer.nextElement().toString();
                        String next = nextPath.replaceAll("/", ".");
                        if (next.startsWith(pckgname) && next.endsWith(".class")) {
                            String clazz = next.substring(0, next.length() - 6);
                            classes.add(Class.forName(clazz));
                        }
                        else if (recursive) {

                            classes.addAll(getClassesForPackage(res, true));
                        }
                    }
                    zf.close();
                }
                else
                    directories.add(new File(URLDecoder.decode(res, "UTF-8")));
            }
        }
        catch (NullPointerException x) {
            throw new ClassNotFoundException(pckgname
                    + " does not appear to be a valid package (Null pointer exception)");
        }
        catch (UnsupportedEncodingException encex) {
            throw new ClassNotFoundException(pckgname + " does not appear to be a valid package (Unsupported encoding)");
        }
        catch (IOException ioex) {
            ioex.printStackTrace();
            throw new ClassNotFoundException("IOException was thrown when trying to get all resources for " + pckgname);
        }
        catch (Exception e) {
            e.printStackTrace(System.err);
        }

        for (File directory : directories) {
            // NeptusLog.pub().info("<###> "+directory);
            if (directory.exists()) {
                // Get the list of the files contained in the package
                String[] files = directory.list();
                for (String file : files) {
                    // we are only interested in .class files
                    if (file.endsWith(".class")) {
                        // removes the .class extension
                        classes.add(Class.forName(pckgname + '.' + file.substring(0, file.length() - 6)));
                    }
                    else if (recursive) {
                        classes.addAll(getClassesForPackage(pckgname + '.' + file, true));
                    }
                }
            }
            else {
                throw new ClassNotFoundException(pckgname + " (" + directory.getPath()
                        + ") does not appear to be a valid package");
            }
        }
        return classes;
    }

    public static List<Class<?>> getImplementationsForPackage(String pckgname, Class<?> iface)
            throws ClassNotFoundException {
        ArrayList<Class<?>> allClasses = new ArrayList<Class<?>>();
        allClasses.addAll(getClassesForPackage(pckgname));
        for (Class<?> c : allClasses) {
            boolean hasInterface = false;
            for (Class<?> itf : c.getInterfaces()) {
                if (itf.equals(iface)) {
                    hasInterface = true;
                    break;
                }
            }
            if (!hasInterface)
                allClasses.remove(c);
        }
        return allClasses;
    }

    public static String getCallerStamp() {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();

        if (stack.length < 4)
            return "(?:?)";
        return "(" + stack[3].getFileName() + ":" + stack[3].getLineNumber() + ")";
    }

    @SuppressWarnings("unchecked")
    public static ArrayList<Class<Maneuver>> listManeuvers() {
        ArrayList<Class<Maneuver>> maneuvers = new ArrayList<Class<Maneuver>>();
        try {
            List<Class<?>> classes = ReflectionUtil.getClassesForPackage(Maneuver.class.getPackage().getName());

            for (Class<?> c : classes) {
                if (c == null || c.getSimpleName().length() == 0)
                    continue;

                if (hasAnySuperClass(c, Maneuver.class)) {
                    maneuvers.add((Class<Maneuver>) c);
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return maneuvers;
    }

    public static Class<?>[] listPlanTemplates() {

        Vector<Class<?>> templates = new Vector<Class<?>>();
        try {
            List<Class<?>> classes = ReflectionUtil.getClassesForPackage(AbstractPlanTemplate.class.getPackage()
                    .getName());

            for (Class<?> c : classes) {
                if (c == null || c.getSimpleName().length() == 0)
                    continue;

                if (isSubclass(c, AbstractPlanTemplate.class) && c.getAnnotation(PluginDescription.class) != null) {
                    templates.add(c);
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return templates.toArray(new Class<?>[0]);

    }

    public static Class<?>[] listTileProviders() {
        Vector<Class<?>> tileProviders = new Vector<Class<?>>();
        try {
            List<Class<?>> classes = ReflectionUtil.getClassesForPackage(Tile.class.getPackage().getName());

            for (Class<?> c : classes) {
                if (c == null || c.getSimpleName().length() == 0)
                    continue;

                if (hasAnnotation(c, MapTileProvider.class)) {
                    tileProviders.add(c);
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return tileProviders.toArray(new Class<?>[0]);
    }
    
    public static Collection<Method> getMethodsAnnotatedWith(Class<? extends Annotation> ann, Object o) {
        Class<?> c;
        if (o instanceof Class<?>)
            c = (Class<?>)o;
        else
            c = o.getClass();
        
        HashSet<Method> methods = new HashSet<>(); 
        for (Method m : c.getMethods()) {
            if (m.getAnnotation(ann) != null)
                methods.add(m);
        }
        for (Method m : c.getDeclaredMethods()) {
            if (m.getAnnotation(ann) != null) {
                m.setAccessible(true);
                methods.add(m);
            }
        }
        return methods;
    }
    
    public static Collection<Field> getFieldsAnnotatedWith(Class<? extends Annotation> ann, Object o) {
        Class<?> c;
        if (o instanceof Class<?>)
            c = (Class<?>)o;
        else
            c = o.getClass();
        
        HashSet<Field> fields = new HashSet<>(); 
        for (Field f : c.getFields())
            if (f.getAnnotation(ann) != null)
                fields.add(f);
        for (Field f : c.getDeclaredFields()) {
            if (f.getAnnotation(ann) != null) {
                f.setAccessible(true);
                fields.add(f);
            }
        }
        return fields;
    }
    

    public static boolean hasAnnotation(Class<?> clazz, Class<? extends Annotation> annotation) {
        return clazz.isAnnotationPresent(annotation);
    }

    public static boolean hasInterface(Class<?> clazz, Class<?> interf) {
        return interf.isAssignableFrom(clazz);
    }

    public static boolean isSubclass(Class<?> clazz, Class<?> superClass) {
        return superClass.isAssignableFrom(clazz);
    }

    public static boolean hasAnySuperClass(Class<?> clazz, Class<?> superClass) {
        return superClass.isAssignableFrom(clazz);
//        if (clazz == null || clazz.equals(Object.class))
//            return false;
//
//        if (clazz.getSuperclass() != null && clazz.getSuperclass().equals(superClass))
//            return true;
//        else if (clazz.getSuperclass() != null && !clazz.getSuperclass().equals(superClass)) {
//            return hasAnySuperClass(clazz.getSuperclass(), superClass);
//        }
//        return hasInterface(clazz.getSuperclass(), superClass);
    }

    public static void main(String[] args) throws Exception {

        for (Class<?> c : listPlanTemplates()) {
            NeptusLog.pub().info("<###> "+c.getName());
        }

        for (Class<?> c : PluginsRepository.listExtensions(MRAVisualization.class).values()) {
            NeptusLog.pub().info("<###> "+c.getName());
        }

        for (Class<?> c : listManeuvers()) {
            NeptusLog.pub().info("<Maneuver> "+c.getName());
        }
    }
}
