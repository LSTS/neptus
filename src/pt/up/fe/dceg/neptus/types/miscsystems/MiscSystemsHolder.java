/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by pdias
 * 2005/10/09
 */
package pt.up.fe.dceg.neptus.types.miscsystems;

import java.util.LinkedHashMap;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Node;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.types.miscsystems.MiscSystems.MiscSystemTypeEnum;
import pt.up.fe.dceg.neptus.util.FileUtil;
import pt.up.fe.dceg.neptus.util.conf.ConfigFetch;

/**
 * @author Paulo Dias
 *
 */
public class MiscSystemsHolder {
    private static LinkedHashMap<String, MiscSystems> miscSystemsList = new LinkedHashMap<String, MiscSystems>();
    private static boolean miscSystemsLoaded = false;
    
    private static Document doc = null;
    
    /**
     * 
     */
    public MiscSystemsHolder() {
        super();
        loadMiscSystems();
    }

    
    public static int size() {
        if (!miscSystemsLoaded) {
            miscSystemsLoaded = true;
            loadMiscSystems();
        }
        
        return miscSystemsList.size();
    }
    
    
    /**
     * 
     */
    public static boolean loadMiscSystems() {
        if (miscSystemsLoaded)
            return false;
        
        String filePath = ConfigFetch.getMiscSystemsConfigLocation();
        String fileAsString = FileUtil.getFileAsString(filePath);
        
        try {
            doc = DocumentHelper.parseText(fileAsString);
            
            List<?> lt = doc.selectNodes("//acoustic-transponders/transponder");
            for(Object obj : lt) {
                String transXML = ((Node) obj).asXML();
                AcusticTransponder at = new AcusticTransponder(transXML);
                if (at.isLoadOk()) {
                    MiscSystemsHolder.getMiscSystemsList().put(at.getId(), at);
                }
            }

          List<?> lpl = doc.selectNodes("//payload");
          for(Object obj : lpl) {
              String transXML = ((Node) obj).asXML();
              MiscSystems at = new MiscSystems(transXML);
              if (at.isLoadOk()) {
                  MiscSystemsHolder.getMiscSystemsList().put(at.getId(), at);
              }
          }

            miscSystemsLoaded = true;

        } catch (DocumentException e) {
            NeptusLog.pub().error("loadMiscSystems", e);
            miscSystemsLoaded = false;
        }
        
        NeptusLog.pub().debug("MiscSystemsHolder #: " + MiscSystemsHolder.size());
        return miscSystemsLoaded;
    }


    /**
     * @return Returns the miscSystemsList.
     */
    public static LinkedHashMap<String, MiscSystems> getMiscSystemsList() {
        return miscSystemsList;
    }

    public static LinkedHashMap<String, MiscSystems> getPayloadList() {
        LinkedHashMap<String, MiscSystems> ret = new LinkedHashMap<String, MiscSystems>();
        for (String id : miscSystemsList.keySet()) {
            MiscSystems ms = miscSystemsList.get(id);
            if (ms.type == MiscSystemTypeEnum.Payload)
                ret.put(id, ms);
        }
        return ret;
    }
    
    
    public static void main(String[] args) {
		ConfigFetch.initialize();
		MiscSystemsHolder.loadMiscSystems();
		
		for (String key : MiscSystemsHolder.getMiscSystemsList().keySet()) {
			MiscSystems ms = MiscSystemsHolder.getMiscSystemsList().get(key);
			System.out.printf("%s : %s : %s\n", ms.getId(), ms.getName(), ms.getModel());
		}
	}
}
