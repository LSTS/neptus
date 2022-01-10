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
 * Author: Paulo Dias
 * 4 de Set de 2012
 */
package pt.lsts.neptus.util;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Properties;

import pt.lsts.neptus.NeptusLog;

/**
 * @author pdias
 *
 */
public class PropertiesUTF8LoaderWriter {
    // http://www.cs.technion.ac.il/~imaman/programs/unicodeprops.html
    
    private PropertiesUTF8LoaderWriter() {
        // TODO Auto-generated constructor stub
    }

    public static Properties loadProperties(String filePath) throws Exception {
        return loadProperties(new FileInputStream(new File(filePath)), "utf-8");
    }

    public static Properties loadProperties(File file) throws Exception {
        return loadProperties(new FileInputStream(file), "utf-8");
    }

    public static Properties loadProperties(InputStream is) throws Exception {
        return loadProperties(is, "utf-8");
    }

    private static Properties loadProperties(InputStream is, String encoding) throws IOException {
        StringBuilder sb = new StringBuilder();
        InputStreamReader isr = new InputStreamReader(is, encoding);
        while (true) {
            int temp = isr.read();
            if (temp < 0)
                break;

            char c = (char) temp;
            sb.append(c);
        }

        String inputString = escapifyStr(sb.toString());
        byte[] bs = inputString.getBytes("ISO-8859-1");
        ByteArrayInputStream bais = new ByteArrayInputStream(bs);

        Properties ps = new Properties();
        ps.load(bais);
        return ps;
    }

    private static char hexDigit(char ch, int offset) {
        int val = (ch >> offset) & 0xF;
        if (val <= 9)
            return (char) ('0' + val);

        return (char) ('A' + val - 10);
    }

    private static String escapifyStr(String str) {
        StringBuilder result = new StringBuilder();

        int len = str.length();
        for (int x = 0; x < len; x++) {
            char ch = str.charAt(x);
            if (ch <= 0x007e) {
                result.append(ch);
                continue;
            }

            result.append('\\');
            result.append('u');
            result.append(hexDigit(ch, 12));
            result.append(hexDigit(ch, 8));
            result.append(hexDigit(ch, 4));
            result.append(hexDigit(ch, 0));
        }
        return result.toString();
    }
    

    public static void storeProperties(Properties properties, String comments, String fileOutputPath) throws FileNotFoundException {
        FileOutputStream fos = new FileOutputStream(new File(fileOutputPath));
        storeProperties(properties, comments, fos);
    }

    private static void storeProperties(Properties properties, String comments, OutputStream out) {
        try {
            Writer writer = new BufferedWriter(new OutputStreamWriter(out, "utf-8"));
            properties.store(writer, comments);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static void main(String[] args) throws Exception {
        Properties prop1 = new Properties();
        Properties propUtf8 = new Properties();
        
        prop1.put("test\u00201", "test 1");
        prop1.put("test 1", "test 1 é € \u0409");

        propUtf8.put("test\u00201", "test 1");
        propUtf8.put("test 1", "test 1 é € \u0409");
        
        prop1.store(new FileOutputStream("prop1.properties"), "8889");
        storeProperties(propUtf8, "utf8", "propUtf8.properties");

        prop1.load(new FileInputStream("propUtf8.properties"));
        propUtf8 = loadProperties(new FileInputStream("propUtf8.properties"));
        
        NeptusLog.pub().info("<###> "+prop1.getProperty("test 1"));
        NeptusLog.pub().info("<###> "+propUtf8.getProperty("test 1"));
    }
}
