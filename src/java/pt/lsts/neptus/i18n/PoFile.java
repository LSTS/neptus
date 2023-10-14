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
 * Author: José Pinto
 * Sep 5, 2012
 */
package pt.lsts.neptus.i18n;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pt.lsts.neptus.NeptusLog;

/**
 * @author zp
 *
 */
public class PoFile {

    enum LAST_TYPE {ID, STR, CTXT};

    protected static Pattern pattern = Pattern.compile("\"([^\"]*)\"");
    protected LinkedHashMap<String, String> translations = new LinkedHashMap<String, String>();
    
    protected String getTranslation(String key) {
        return translations.get(key);
    }
    
    protected void addEntry(PoEntry entry) {
        if (entry.msgid == null || entry.msgid.isEmpty())
            return;
        
        translations.put(entry.getKey(), entry.getTranslation()); 
    }

    protected void load(File location) throws IOException {        
        // BufferedReader reader = new BufferedReader(new FileReader(location));
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(location), "UTF-8"));
        PoEntry curEntry = new PoEntry();
        String line;

        LAST_TYPE lastType = LAST_TYPE.CTXT;

        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) 
                continue;

            if (line.startsWith("msgid")) {
                lastType = LAST_TYPE.ID;                
                if (curEntry.msgid != null) {
                    addEntry(curEntry);
                    curEntry = new PoEntry();
                }
                Matcher matcher = pattern.matcher(line);
                if (matcher.find())
                    curEntry.msgid = matcher.group(1);                
                else
                    curEntry.msgid = "";
            }

            else if (line.startsWith("msgstr")) {
                lastType = LAST_TYPE.STR;

                Matcher matcher = pattern.matcher(line);
                if (matcher.find())
                    curEntry.msgstr = matcher.group(1);                
                else
                    curEntry.msgstr = "";
            }

            else if (line.startsWith("msgctxt")) {
                lastType = LAST_TYPE.CTXT;
                if (curEntry.msgctxt != null || curEntry.msgid != null) {
                    addEntry(curEntry);
                    curEntry = new PoEntry();
                }

                Matcher matcher = pattern.matcher(line);
                if (matcher.find())
                    curEntry.msgctxt = matcher.group(1);                
                else
                    curEntry.msgctxt = "";
            }

            else if (line.startsWith("\"")) {
                Matcher matcher = pattern.matcher(line);
                String text = "";
                if (matcher.find())
                    text = matcher.group(1);

                switch (lastType) {
                    case CTXT:
                        curEntry.msgctxt +=text;
                        break;
                    case STR:
                        curEntry.msgstr += text;
                        break;
                    case ID:
                        curEntry.msgid += text;
                        break;
                    default:
                        break;
                }
            }        
        }
        if (lastType == LAST_TYPE.STR && (curEntry.msgctxt != null || curEntry.msgid != null))
            addEntry(curEntry);

        reader.close();
        NeptusLog.pub().info("Loaded " + translations.size() + " translations.");
    }


    private class PoEntry implements Comparable<PoEntry> {
        public String msgid = null;
        public String msgstr = null;
        public String msgctxt = null;

        public String getKey() {
            if (msgctxt != null)
                return I18n.normalize(msgid) + I18n.normalize(msgctxt);
            else 
                return I18n.normalize(msgid);
        }
        
        public String getTranslation() {
            if (msgstr == null || msgstr.isEmpty())
                return msgid;
            else
                return msgstr;
        }

        @Override
        public int compareTo(PoEntry o) {
            return getKey().compareTo(o.getKey());
        }
        
        @Override
        public String toString() {            
            return("key="+getKey()+(msgctxt != null && msgctxt.length() > 0 ? "["+msgctxt+"]" : "")+"\nenglish="+msgid+"\ntranslation="+msgstr+"\n");
        }
    }

    public static void main(String[] args) throws Exception  {
        PoFile file = new PoFile();
        file.load(new File("conf/i18n/neptus.pot"));
        
        for (String key : file.translations.keySet()) {
            NeptusLog.pub().info("<###> "+key + "=" + file.translations.get(key));
        }

        NeptusLog.pub().info("<###>\n\n______________________________________________________________\n\n");
        
        file = new PoFile();
        file.load(new File("conf/i18n/pt/neptus.po"));
        
        for (String key : file.translations.keySet()) {
            NeptusLog.pub().info("<###> "+key + "=" + file.translations.get(key));
        }

    }
}
