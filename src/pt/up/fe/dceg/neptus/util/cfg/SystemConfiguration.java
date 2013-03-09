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
 * Dec 14, 2012
 */
package pt.up.fe.dceg.neptus.util.cfg;

import java.io.File;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.ini4j.Ini;
import org.ini4j.Profile.Section;
import org.ini4j.Wini;

import pt.up.fe.dceg.neptus.gui.PropertiesEditor;
import pt.up.fe.dceg.neptus.gui.PropertiesProvider;
import pt.up.fe.dceg.neptus.imc.IMCUtil;
import pt.up.fe.dceg.neptus.imc.Parameter;
import pt.up.fe.dceg.neptus.imc.ParameterControl;
import pt.up.fe.dceg.neptus.util.cfg.ConfigParameter.TYPE;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

/**
 * @author zp
 */
public class SystemConfiguration implements PropertiesProvider {

    protected LinkedHashMap<String, ConfigParameter> params = new LinkedHashMap<>();
    protected String systemName = "System";

    public SystemConfiguration() {

    }

    public SystemConfiguration(ParameterControl paramsListMsg) {
        this();
        setMessage(paramsListMsg);
    }

    public void loadFile(Ini ini) throws Exception {
        Pattern p = Pattern.compile("(.*)(\\(\\w+\\))");
        
        for (String section : ini.keySet()) {
            Section sec = ini.get(section);
            for (String name: sec.keySet()) {
                String value = sec.get(name);
                
                ConfigParameter param = new ConfigParameter(section, name, value);
                Matcher m = p.matcher(value);
                if (m.matches()) {
                    String type = m.group(2).toLowerCase();
                    String val = m.group(1);
                    switch (type) {
                        case "integer":
                        case "int":
                            param.setValue(val, TYPE.INTEGER);
                            break;
                        case "real":
                        case "double":
                        case "float":
                            param.setValue(val, TYPE.REAL);
                            break;
                        case "string list":
                            param.setValue(val, TYPE.STRING_LIST);
                            break;
                        default:
                            param.setValue(val, TYPE.STRING);
                            break;
                    }
                }                
            }
        }
    }
    
    public void saveFile() throws Exception {
        File f = new File("conf/params");
        f.mkdirs();
        final Wini ini = new Wini(f);
        for (ConfigParameter p : params.values()) {
            switch (p.getType()) {
                case INTEGER:
                    ini.put(p.getSection(), p.getName(), p.getValue()+" (integer)");
                    break;
                case REAL:
                    ini.put(p.getSection(), p.getName(), p.getValue()+" (real)");
                    break;
                case STRING:
                    ini.put(p.getSection(), p.getName(), p.getValue()+" (string)");
                    break;
                case STRING_LIST:
                    ini.put(p.getSection(), p.getName(), p.getValue()+" (string list)");
                    break;
                default:
                    break;
            }
        }
        
        ini.store(new File(f, systemName+".ini"));        
    }

    public void addParameter(String section, String name, String value) {
        ConfigParameter p = new ConfigParameter(section, name, value);
        params.put(name, p);
    }
    
    public void addParameter(String section, String name, int value) {
        ConfigParameter p = new ConfigParameter(section, name, ""+value);
        p.setType(TYPE.INTEGER);
        params.put(name, p);
    }

    public void addParameter(String section, String name, double value) {
        ConfigParameter p = new ConfigParameter(section, name, ""+value);
        p.setType(TYPE.REAL);
        params.put(name, p);
    }
    
    public void addParameter(String section, String name, Collection<String> value) {
        
        java.util.Iterator<String> it = value.iterator();
        String val = "";
        if (it.hasNext())
            val = it.next();
        
        while (it.hasNext())
            val += ","+it.next();
        
        ConfigParameter p = new ConfigParameter(section, name, val);
        p.setType(TYPE.STRING_LIST);
        params.put(name, p);
    }
    
    public void setMessage(ParameterControl paramsListMsg) {
        systemName = paramsListMsg.getSourceName();
        for (Parameter p : paramsListMsg.getParams())
            params.put(p.getParam(), new ConfigParameter(p));   
    }

    public ParameterControl getMessage() {
        ParameterControl msg = new ParameterControl();
        Vector<Parameter> msgParams = new Vector<>();

        for (ConfigParameter p : params.values())
            msgParams.add(p.getMessage());

        msg.setParams(msgParams);
        msg.setOp(ParameterControl.OP.SET_PARAMS);

        return msg;        
    }

    @Override
    public DefaultProperty[] getProperties() {

        Vector<DefaultProperty> props = new Vector<>();
        for (ConfigParameter p : params.values())
            props.add(p.asProperty());

        return props.toArray(new DefaultProperty[0]);
    }

    @Override
    public String getPropertiesDialogTitle() {
        return systemName+" parameters";
    }

    @Override
    public String[] getPropertiesErrors(Property[] properties) {
        return null;
    }

    @Override
    public void setProperties(Property[] properties) {
        for (Property p : properties)
            params.put(p.getName(), new ConfigParameter(p.getCategory(), p.getName(), p.getValue().toString()));
    }
    
    public static void main(String[] args) throws Exception {
        SystemConfiguration config = new SystemConfiguration();
        config.systemName = "lauv-seacon-5";
        config.addParameter("HTTP Server", "Bind port", 8080);
        config.addParameter("HTTP Server", "Number of threads", 2);
        PropertiesEditor.editProperties(config, true);
        config.saveFile();
        IMCUtil.debug(config.getMessage());        
    }
}
