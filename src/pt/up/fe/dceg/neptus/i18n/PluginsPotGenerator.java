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
 * Sep 11, 2012
 * $Id:: PluginsPotGenerator.java 9777 2013-01-28 14:43:48Z pdias               $:
 */
package pt.up.fe.dceg.neptus.i18n;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Vector;

import pt.up.fe.dceg.neptus.console.events.ConsoleEventVehicleStateChanged;
import pt.up.fe.dceg.neptus.console.events.ConsoleEventVehicleStateChanged.STATE;
import pt.up.fe.dceg.neptus.console.plugins.SystemsList;
import pt.up.fe.dceg.neptus.gui.PropertiesEditor;
import pt.up.fe.dceg.neptus.gui.system.SystemDisplayComparator;
import pt.up.fe.dceg.neptus.imc.Announce;
import pt.up.fe.dceg.neptus.imc.Goto;
import pt.up.fe.dceg.neptus.imc.Loiter;
import pt.up.fe.dceg.neptus.mp.ManeuverLocation.Z_UNITS;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.acoustic.LBLRangeDisplay.HideOrFadeRangeEnum;
import pt.up.fe.dceg.neptus.plugins.map.MapEditor;
import pt.up.fe.dceg.neptus.plugins.planning.MapPanel;
import pt.up.fe.dceg.neptus.types.map.AbstractElement;
import pt.up.fe.dceg.neptus.types.vehicle.VehicleType.SystemTypeEnum;
import pt.up.fe.dceg.neptus.types.vehicle.VehicleType.VehicleTypeEnum;
import pt.up.fe.dceg.neptus.util.FileUtil;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcSystem;

import com.l2fprod.common.propertysheet.DefaultProperty;


/**
 * @author zp
 *
 */
public class PluginsPotGenerator {

    protected static final String outFile = I18n.I18N_BASE_LOCALIZATION+"/neptus.pot";
    protected static final String inFile = "dev-scripts/i18n/empty.pot";
    
    public static Vector<AbstractElement> mapElements() {
        try {
            return new MapEditor(null).getElements();
        }
        catch (Exception e) {
            e.printStackTrace();
            return new Vector<>();
        }
    }
    
    // add here more enumerations that are used in the GUI
    public static Vector<Class<?>> enums() {
        Vector<Class<?>> enums = new Vector<>();
        enums.add(Announce.SYS_TYPE.class);
        enums.add(ConsoleEventVehicleStateChanged.STATE.class);
        enums.add(ImcSystem.IMCAuthorityState.class);
        enums.add(Goto.SPEED_UNITS.class);
        enums.add(Loiter.TYPE.class);
        enums.add(MapPanel.PlacementEnum.class);
        enums.add(STATE.class);
        enums.add(SystemDisplayComparator.OrderOptionEnum.class);
        enums.add(SystemsList.SortOrderEnum.class);
        enums.add(SystemsList.MilStd2525SymbolsFilledEnum.class);
        enums.add(SystemTypeEnum.class);
        enums.add(VehicleTypeEnum.class);
        enums.add(Z_UNITS.class);
        enums.add(HideOrFadeRangeEnum.class);
        
        return enums;
    }
    
    public static void listClasses(Vector<Class<?>> classes, String packageName, File dir) {
        for (File f : dir.listFiles()) {
            if (f.isDirectory()) {
                if (packageName.isEmpty())
                    listClasses(classes, f.getName(), f);
                else
                    listClasses(classes, packageName+"."+f.getName(), f);
            }
            else if (FileUtil.getFileExtension(f).equals("java")) {
                String className = (packageName.isEmpty()? "" : packageName+".")+f.getName().substring(0, f.getName().indexOf("."));
                try {
                    classes.add(Class.forName(className, false, Thread.currentThread().getContextClassLoader()));
                }
                catch (Exception e) {
                   // e.printStackTrace();
                }
                catch (Error e) {
                   // e.printStackTrace();
                }
                //System.err.println(className);
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
        //text = text.replaceAll("\\", "\\\\");
        return text;
    }

    public static void main(String[] args) throws Exception {
        
        BufferedWriter writer = new BufferedWriter(new FileWriter(new File(outFile)));
        
        BufferedReader reader = new BufferedReader(new FileReader(new File(inFile)));
        
        String line;
        while ((line = reader.readLine()) != null)
            writer.write(line+"\n");
        
        reader.close();
        
        Vector<Class<?>> classes = getAllClasses();
        
        for (Class<?> c : classes) {
            try {
                PluginDescription pd = c.getAnnotation(PluginDescription.class);
                if (!pd.name().isEmpty()) {
                    writer.write("#: Name of plugin "+c.getName()+"\n");
                    writer.write("msgid \""+escapeQuotes(pd.name())+"\"\n");
                    writer.write("msgstr \"\"\n\n");
                }
                if (!pd.description().isEmpty()) {
                    writer.write("#: Description of plugin "+c.getName()+"\n");
                    writer.write("msgid \""+escapeQuotes(pd.description())+"\"\n");
                    writer.write("msgstr \"\"\n\n");
                }
            }
            catch (Exception e) {
                //e.printStackTrace();
            }
            
            try {
                LinkedHashMap<String, DefaultProperty> props = getProperties(c);
                
                for (DefaultProperty dp : props.values()) {
                    writer.write("#: Property from "+c.getName()+"\n");
                    writer.write("msgid \""+escapeQuotes(dp.getName())+"\"\n");
                    writer.write("msgstr \"\"\n\n");                  
                    
                    if (!dp.getShortDescription().equals(dp.getName())) {
                        writer.write("#: Description of property '"+dp.getDisplayName()+"' from "+c.getName()+"\n");
                        writer.write("msgid \""+escapeQuotes(dp.getShortDescription())+"\"\n");
                        writer.write("msgstr \"\"\n\n");                        
                    }
                }
            }
            catch (Exception e) {
                //e.printStackTrace();
            }
            catch (Error e) {
                // TODO: handle exception
            }
        }
        
        for (AbstractElement elem : mapElements()) {
            writer.write("#: Map element type (class "+elem.getClass().getSimpleName()+")\n");
            writer.write("msgid \""+elem.getType()+"\"\n");
            writer.write("msgstr \"\"\n\n");                  
        }
        
        for (Class<?> enumClass : enums()) {
            for (Object o : enumClass.getEnumConstants()) {
                String name = enumClass.getSimpleName();
                if (enumClass.getEnclosingClass() != null) {
                    name = enumClass.getEnclosingClass().getSimpleName()+"."+name;
                }
                writer.write("#: Field from "+name+" enumeration\n");
                writer.write("msgid \""+o+"\"\n");
                writer.write("msgstr \"\"\n\n");
            }
        }
        
        writer.close();
    }

}
