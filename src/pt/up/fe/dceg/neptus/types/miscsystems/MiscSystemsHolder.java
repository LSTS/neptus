/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
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
 * https://www.lsts.pt/neptus/licence.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: pdias
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
