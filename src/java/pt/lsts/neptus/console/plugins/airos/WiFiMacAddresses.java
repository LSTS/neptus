/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: zp
 * Dec 22, 2015
 */
package pt.lsts.neptus.console.plugins.airos;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.io.Serializable;
import java.net.URL;
import java.net.URLConnection;
import java.util.LinkedHashMap;

import javax.swing.JOptionPane;

import com.csvreader.CsvReader;

/**
 * @author zp
 *
 */
public class WiFiMacAddresses implements Serializable {
    private static final long serialVersionUID = 8281634897552653842L;
    private static final String CONF_FILE = "conf/wifimac.obj";

    private LinkedHashMap<String, String> addresses = new LinkedHashMap<>();
    private static WiFiMacAddresses instance = null;
    private WiFiMacAddresses() {

    }

    public static String resolve(String mac) {
        return getInstance().addresses.get(mac);    
    }

    public static synchronized WiFiMacAddresses getInstance() {
        if (instance == null) {
            try {
                if (!new File(CONF_FILE).canRead())
                    throw new IOException("Can't read file "+CONF_FILE);
                ObjectInputStream ois = new ObjectInputStream(new FileInputStream(new File(CONF_FILE)));
                instance = (WiFiMacAddresses) ois.readObject();
                ois.close();
            }
            catch (Exception e) {
                e.printStackTrace();
                instance = new WiFiMacAddresses();            
            }
        }

        return instance;
    }
    
    public static void parseAddresses(Reader reader) throws IOException {
        WiFiMacAddresses addrs = new WiFiMacAddresses();
        CsvReader csv = new CsvReader(reader);
        csv.skipLine();
        while (csv.readRecord()) {
            String mac = csv.get(2).trim();
            String name = csv.get(3).trim();
            if (!mac.isEmpty() && !name.isEmpty())
                addrs.addresses.put(mac.toUpperCase(), name);
        }
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(new File(CONF_FILE)));
        oos.writeObject(addrs);
        reader.close();
        oos.close();
        instance = addrs;
    }

    public static void downloadAddresses(URL url) throws IOException {
        URLConnection conn = url.openConnection();
        InputStreamReader reader = new InputStreamReader(conn.getInputStream());
        parseAddresses(reader);
    }

    public static void main(String[] args) throws Exception {
        String res = JOptionPane.showInputDialog("Please enter addresses URL");
        if (res != null)
            downloadAddresses(new URL(res));        
    }
}
