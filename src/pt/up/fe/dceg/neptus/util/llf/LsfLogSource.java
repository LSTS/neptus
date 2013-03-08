/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by jqcorreia
 * May 4, 2012
 * $Id:: LsfLogSource.java 9802 2013-01-30 00:34:23Z zepinto                    $:
 */
package pt.up.fe.dceg.neptus.util.llf;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Vector;

import pt.up.fe.dceg.neptus.imc.IMCDefinition;
import pt.up.fe.dceg.neptus.imc.gz.MultiMemberGZIPInputStream;
import pt.up.fe.dceg.neptus.imc.lsf.LsfIndex;
import pt.up.fe.dceg.neptus.imc.lsf.LsfIndexListener;
import pt.up.fe.dceg.neptus.mra.importers.IMraLog;
import pt.up.fe.dceg.neptus.mra.importers.IMraLogGroup;
import pt.up.fe.dceg.neptus.mra.importers.lsf.LsfMraLog;

/**
 * @author jqcorreia
 *
 */
public class LsfLogSource implements IMraLogGroup {

    IMCDefinition defs;
    LsfIndex index;
    File lsfFile;
    LsfIndexListener listener = null;
    String[] existingMessages = null;
    Collection<Integer> vehicleSources;
    
    
    public LsfLogSource(String filename, LsfIndexListener listener) throws Exception {
        this(new File(filename), listener);
    }

    public LsfLogSource(File file, LsfIndexListener listener) throws Exception {
        this.listener = listener;
        loadLog(file);
    }
    
    public void cleanup() {
        if (index != null)
            index.cleanup();
    }

    private void loadLog(File f) throws Exception {
        if(!f.canRead())
            throw(new IOException());
        
        if (f.getName().endsWith(".lsf.gz")) {
            
            MultiMemberGZIPInputStream mmgis = new MultiMemberGZIPInputStream(new FileInputStream(f));
            File outFile = new File(f.getAbsolutePath().replaceAll("\\.gz$", ""));
            outFile.createNewFile();
            FileOutputStream outStream = new FileOutputStream(outFile);
            try {
                byte[] extra = new byte[50000];
                int ret = 0;
                for (;;) {
                    ret = mmgis.read(extra);
                    if (ret != -1) {
                        byte[] extra1 = new byte[ret];
                        System.arraycopy(extra, 0, extra1, 0, ret);
                        outStream.write(extra1);
                        outStream.flush();
                    }
                    else {
                        break;
                    }
                }
                f = outFile;
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }

        lsfFile = f;
        File defsFile = new File(f.getParent()+"/IMC.xml");
        if(defsFile.canRead()) {
            defs = new IMCDefinition(new FileInputStream(defsFile));
        }
        else {
            defs = IMCDefinition.getInstance(); // If IMC.xml isn't present use the default ones
        }

        index = new LsfIndex(lsfFile, defs, listener);
    }


    @Override
    public String name() {
        return lsfFile.getParentFile().getName();
    }

    @Override
    public LinkedHashMap<String, Object> metaInfo() {
        return null;
    }

    /**
     * @return The IMraLog object related to the type of message stated in logName 
     */
    @Override
    public IMraLog getLog(String logName) {

        if (index == null)
            return null;
        
        try {
            int i = index.getFirstMessageOfType(logName);
            if(i != -1)
                return new LsfMraLog(index, logName);
            else
                return null;
        }
        catch (Exception e) {
            System.out.println("Index is: " + index);
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public boolean parse(URI uri) {
        return false;
    }



    @Override
    public String[] listLogs() {

        if (existingMessages == null) {
            Vector<String> list = new Vector<String>();
            Vector<Integer> indexes = new Vector<Integer>();

            for(int i = 0; i < index.getNumberOfMessages(); i++) {
                int type = index.typeOf(i);
                if (!indexes.contains(type)) {
                    indexes.add(type);
                    String msgName = index.getDefinitions().getMessageName(type);
                    if (msgName == null) {
                        System.err.println("Message type not found in the definitions: "+type);
                    }
                    else
                        list.add(index.getDefinitions().getMessageName(type));
                }
            }
            existingMessages = list.toArray(new String[list.size()]);
        }
        return existingMessages;
    }

    @Override
    public File getFile(String name) {
        if (lsfFile != null) {
            File f = new File(lsfFile.getParentFile(), name);
            if(f.canRead())
                return f;
            else 
                return null;
        }
        return null;
    }
    
    @Override
    public String getEntityName(int src, int src_ent) {
        return index.getEntityName(src, src_ent);
    }
    
    @Override
    public String getSystemName(int src) {
        return index.getSystemName(src);
    }
    
    public Collection<Integer> getMessageGenerators(String msgType) {
        Vector<Integer> sources = new Vector<Integer>();
        int idx = 0;
        while((idx = index.getNextMessageOfType(msgType,idx))!=-1) {
            if(!sources.contains(index.sourceOf(idx))) {
                sources.add(index.sourceOf(idx));
            }
        }
        return sources;
    }
    
    public Collection<Integer> getVehicleSources() {
        // Actually only vehicles generate EntityInfo messages so it is a good way to  
        // differentiate between vehicles and other nodes.
        if(vehicleSources == null)
            vehicleSources = getMessageGenerators("EntityInfo"); // Just generate this collection once
        
        // Filter out Id's over 0x4000 (CCU id range start value)
        ArrayList<Integer> toRemove = new ArrayList<Integer>();
        for(Integer v: vehicleSources) {
            if(v > 0x4000) {
                toRemove.add(v);
            }
        }
        
        vehicleSources.removeAll(toRemove);
        return vehicleSources; 
    }
    
    @Override
    public LsfIndex getLsfIndex() {
        return index;
    }
    
    public static void main(String[] args) throws Exception {
        LsfLogSource source = new LsfLogSource("/home/jqcorreia/Desktop/merge2/Data.lsf", null);
//        System.out.println(source.listLogs());
        System.out.println(source.getVehicleSources().size()+ "sfsfsf ");
    }
}
