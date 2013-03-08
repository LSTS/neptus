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
 * Sep 5, 2012
 * $Id:: PoFile.java 9615 2012-12-30 23:08:28Z pdias                            $:
 */
package pt.up.fe.dceg.neptus.i18n;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
                    case ID:
                        curEntry.msgid += text;
                    default:
                        break;
                }
            }        
        }
        reader.close();
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
            System.out.println(key + "=" + file.translations.get(key));
        }

        System.out.println("\n\n______________________________________________________________\n\n");
        
        file = new PoFile();
        file.load(new File("conf/i18n/pt/neptus.po"));
        
        for (String key : file.translations.keySet()) {
            System.out.println(key + "=" + file.translations.get(key));
        }

    }
}
