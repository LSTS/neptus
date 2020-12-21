/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
 * Sep 11, 2012
 */
package pt.lsts.neptus.i18n;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Vector;

import org.reflections.Reflections;

import com.l2fprod.common.propertysheet.DefaultProperty;

import pt.lsts.imc.Loiter;
import pt.lsts.imc.PlanControlState;
import pt.lsts.imc.def.SpeedUnits;
import pt.lsts.imc.def.SystemType;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.console.events.ConsoleEventVehicleStateChanged;
import pt.lsts.neptus.console.plugins.SystemsList;
import pt.lsts.neptus.console.plugins.planning.MapPanel;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.gui.system.SystemDisplayComparator;
import pt.lsts.neptus.mp.ManeuverLocation.Z_UNITS;
import pt.lsts.neptus.plugins.NeptusMenuItem;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.renderer2d.tiles.Tile.TileState;
import pt.lsts.neptus.types.map.AbstractElement;
import pt.lsts.neptus.types.map.MapType;
import pt.lsts.neptus.types.vehicle.VehicleType.SystemTypeEnum;
import pt.lsts.neptus.types.vehicle.VehicleType.VehicleTypeEnum;
import pt.lsts.neptus.util.FileUtil;

/**
 * @author zp
 * 
 */
public class PluginsPotGenerator {

    protected static final String OUT_FILE = I18n.I18N_BASE_LOCALIZATION + "/neptus.pot";
    protected static final String IN_FILE = "dev-scripts/i18n/empty.pot";

    public static Vector<AbstractElement> mapElements() {
        return MapType.getMapElements();
    }

    // add here more enumerations that are used in the GUI
    public static Vector<Class<?>> enums() {
        Vector<Class<?>> enums = new Vector<>();
        enums.add(SystemType.class);
        enums.add(ConsoleEventVehicleStateChanged.STATE.class);
        enums.add(ImcSystem.IMCAuthorityState.class);
        enums.add(SpeedUnits.class);
        enums.add(Loiter.TYPE.class);
        enums.add(MapPanel.PlacementEnum.class);
        enums.add(PlanControlState.STATE.class);
        enums.add(SystemDisplayComparator.OrderOptionEnum.class);
        enums.add(SystemsList.SortOrderEnum.class);
        enums.add(SystemsList.MilStd2525SymbolsFilledEnum.class);
        enums.add(SystemTypeEnum.class);
        enums.add(VehicleTypeEnum.class);
        enums.add(Z_UNITS.class);
        enums.add(TileState.class);
        return enums;
    }

    public static List<String> customStrings() {
        List<String> strs = new ArrayList<>();
        strs.add("Normal");
        strs.add("Map");
        strs.add("Infinite Rectangle");
        strs.add("Rows Plan");
        strs.add("Console Settings");
//        strs.add("global");
//        strs.add("idle");
//        strs.add("plan");
//        strs.add("manuever");
        return strs;
    }

    public static void listClasses(Vector<Class<?>> classes, String packageName, File dir) {
        for (File f : dir.listFiles()) {
            if (f.isDirectory()) {
                if (packageName.isEmpty())
                    listClasses(classes, f.getName(), f);
                else
                    listClasses(classes, packageName + "." + f.getName(), f);
            }
            else if (FileUtil.getFileExtension(f).equals("java")) {
                String className = (packageName.isEmpty() ? "" : packageName + ".")
                        + f.getName().substring(0, f.getName().indexOf("."));
                try {
                    classes.add(Class.forName(className, false, Thread.currentThread().getContextClassLoader()));
                }
                catch (Exception e) {
                    // e.printStackTrace();
                }
                catch (Error e) {
                    // e.printStackTrace();
                }
                // System.err.println(className);
            }
        }
    }

    public static Vector<Class<?>> getAllClasses() {
        Vector<Class<?>> classes = new Vector<Class<?>>();
        listClasses(classes, "", new File("src"));

        File pluginsDir = new File("plugins-dev");
        for (File f : pluginsDir.listFiles())
            listClasses(classes, "", f);
        return classes;
    }

    public static LinkedHashMap<String, DefaultProperty> getProperties(Class<?> clazz) {
        LinkedHashMap<String, DefaultProperty> props = new LinkedHashMap<String, DefaultProperty>();

        for (Field f : clazz.getFields()) {

            NeptusProperty a = f.getAnnotation(NeptusProperty.class);

            if (a != null) {
                String name = a.name();
                String desc = a.description();
                String category = a.category();
                if (a.name().length() == 0) {
                    name = f.getName();
                }
                if (a.description().length() == 0) {
                    desc = name;
                }
                if (category == null || category.length() == 0) {
                    category = "Base";
                }
                props.put(name, PropertiesEditor.getPropertyInstance(name, category, String.class, "", true, desc));
            }
        }
        return props;
    }

    public static String escapeQuotes(String text) {
        text = text.replaceAll("\"", "'");
        text = text.replaceAll("\n", "\\\\n");
        // text = text.replaceAll("\\", "\\\\");
        return text;
    }

    public static void main(String[] args) throws Exception {
        String inFile = IN_FILE;
        String outFile = OUT_FILE;

        if (args.length == 1) {
            inFile = args[0];
        }
        else if (args.length == 2) {
            inFile = args[0];
            outFile = args[1];
        }

        BufferedWriter writer = new BufferedWriter(new FileWriter(new File(outFile)));

        BufferedReader reader = new BufferedReader(new FileReader(new File(inFile)));

        String line;
        while ((line = reader.readLine()) != null)
            writer.write(line + "\n");

        reader.close();

        Vector<Class<?>> classes = getAllClasses();

        System.out.println("Processing "+classes.size()+" classes...");
        
        for (Class<?> c : classes) {
            try {
                PluginDescription pd = c.getAnnotation(PluginDescription.class);
                if (!pd.name().isEmpty()) {
                    writer.write("#: Name of plugin " + c.getName() + "\n");
                    writer.write("msgid \"" + escapeQuotes(pd.name()) + "\"\n");
                    writer.write("msgstr \"\"\n\n");
                }
                if (!pd.description().isEmpty()) {
                    writer.write("#: Description of plugin " + c.getName() + "\n");
                    writer.write("msgid \"" + escapeQuotes(pd.description()) + "\"\n");
                    writer.write("msgstr \"\"\n\n");
                }
            }
            catch (Exception e) {
                // e.printStackTrace();
            }

            try {
                LinkedHashMap<String, DefaultProperty> props = getProperties(c);

                for (DefaultProperty dp : props.values()) {
                    writer.write("#: Property from " + c.getName() + "\n");
                    writer.write("msgid \"" + escapeQuotes(dp.getName()) + "\"\n");
                    writer.write("msgstr \"\"\n\n");

                    if (!dp.getShortDescription().equals(dp.getName())) {
                        writer.write("#: Description of property '" + dp.getDisplayName() + "' from " + c.getName()
                                + "\n");
                        writer.write("msgid \"" + escapeQuotes(dp.getShortDescription()) + "\"\n");
                        writer.write("msgstr \"\"\n\n");
                    }
                }
            }
            catch (Exception e) {
                // e.printStackTrace();
            }
            catch (Error e) {
                // TODO: handle exception
            }
            
            HashSet<String> addedMenuParts = new HashSet<>();
                        
            for (Method m : methodsAnnotatedWith(NeptusMenuItem.class, c)) {
                String path = m.getAnnotation(NeptusMenuItem.class).value();
                
                for (String part : path.split(">")) {
                    if (addedMenuParts.contains(part.trim()))
                        continue;
                    writer.write("#: Menu item path (class " + c.getSimpleName() + ", complete path: '"+path+"')\n");
                    writer.write("msgid \"" + part.trim() + "\"\n");
                    writer.write("msgstr \"\"\n\n");           
                }
            }
        }

        for (AbstractElement elem : mapElements()) {
            writer.write("#: Map element type (class " + elem.getClass().getSimpleName() + ")\n");
            writer.write("msgid \"" + elem.getType() + "\"\n");
            writer.write("msgstr \"\"\n\n");
        }

        Vector<Class<?>> enums = enums();
        Reflections ref = new Reflections("pt.lsts.neptus.plugins");
        
        for (Class<?> c : ref.getTypesAnnotatedWith(Translate.class)) {
            if (c.getEnumConstants() != null)
                enums.add(c);
        }        
        
        for (Class<?> enumClass : enums) {
            for (Object o : enumClass.getEnumConstants()) {
                String name = enumClass.getSimpleName();
                if (enumClass.getEnclosingClass() != null) {
                    name = enumClass.getEnclosingClass().getSimpleName() + "." + name;
                }
                writer.write("#: Field from " + name + " enumeration\n");
                writer.write("msgid \"" + o + "\"\n");
                writer.write("msgstr \"\"\n\n");
            }
        }
        for (String string : customStrings()) {
            writer.write("#: Custom String \n");
            writer.write("msgid \"" + string + "\"\n");
            writer.write("msgstr \"\"\n\n");
        }

        writer.close();
    }
    
    public static Collection<Method> methodsAnnotatedWith(Class<? extends Annotation> ann, Object o) {
        Class<?> c;
        if (o instanceof Class<?>)
            c = (Class<?>)o;
        else
            c = o.getClass();
        
        HashSet<Method> methods = new HashSet<>(); 
        
        try {
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
        }
        catch (Error e) {
            e.printStackTrace();
        }
        
        return methods;
    }

}
