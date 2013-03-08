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
 * 4 de Set de 2012
 * $Id:: PropertiesUTF8LoaderWriter.java 9615 2012-12-30 23:08:28Z pdias        $:
 */
package pt.up.fe.dceg.neptus.util;

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
        
        System.out.println(prop1.getProperty("test 1"));
        System.out.println(propUtf8.getProperty("test 1"));
    }
}
