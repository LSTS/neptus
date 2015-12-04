/*
 * Copyright (c) 2004-2015 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * 04/12/2015
 */
package pt.lsts.neptus.util.bathymetry;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Date;
import java.util.TreeSet;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.util.conf.ConfigFetch;
import pt.lsts.neptus.util.tid.TidReader;
import pt.lsts.neptus.util.tid.TidReader.Data;
import pt.lsts.neptus.util.tid.TidWriter;

/**
 * @author Paulo Dias
 *
 */
public class TidCachedData extends CachedData {

    /**
     * @param f
     */
    public TidCachedData(File f) {
        try {
            loadFile(f);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        // NeptusLog.pub().info("Loading finished");
        loading = false;
    }
    
    @Override
    public void loadFile(File f) throws Exception {
        cachedData = new TreeSet<>();
        if (f == null || ! f.canRead()) {
            NeptusLog.pub().error(new Exception("Tides file is not valid: "+f));
            return;
        }
        
        BufferedReader br = new BufferedReader(new FileReader(f));
        TidReader tidReader = new TidReader(br);
        
        Data data = tidReader.readData();

        while (data != null) {
            long unixTimeMillis = data.timeMillis;
            double height = data.height;
            Date d = new Date(unixTimeMillis);
            cachedData.add(new TidePeak(d, height));
            data = tidReader.readData();
        }
        
        name = tidReader.getHarbor();
        br.close();
    }

    @Override
    public void saveFile(String port, File f) throws Exception {
        if (!f.getParentFile().exists())
            f.getParentFile().mkdirs();
        
        BufferedWriter writer = new BufferedWriter(new FileWriter(f));
        TidWriter tidWriter = new TidWriter(writer, 2);
        if (port == null || port.isEmpty())
            tidWriter.writeHeader("Tides Data", name);
        else
            tidWriter.writeHeader("Tides Data", port);
        for (TidePeak tp : cachedData) {
            tidWriter.writeData(tp.date.getTime(), tp.height);
        }
        writer.close();
    }

    /**
     * @param portName
     * @return
     */
    @Override
    protected File getFileToSave(String portName) {
        return new File(ConfigFetch.getConfFolder() + "/tides/" + portName + ".tid");
    }

    @Override
    public Date fetchData(String portName, Date aroundDate) throws Exception {
        return super.fetchData(portName, aroundDate);
    }
}
