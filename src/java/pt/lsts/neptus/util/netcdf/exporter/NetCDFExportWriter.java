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
 * Author: pdias
 * 17/05/2018
 */
package pt.lsts.neptus.util.netcdf.exporter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import pt.lsts.neptus.util.netcdf.NetCDFUtils;
import ucar.ma2.Array;
import ucar.ma2.ArrayDouble;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;

/**
 * @author pdias
 *
 */
public class NetCDFExportWriter {

    public static NetcdfFileWriter createWriter(File location) throws IOException {
        return createWriter(location.getAbsolutePath());
    }

    public static NetcdfFileWriter createWriter(String location) throws IOException {
        return NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf3, location, null);
    }
    
    public static void main(String[] args) throws Exception {
        String location = "testWrite.nc";

        try (NetcdfFileWriter writer = createWriter(location)) {
            NetCDFRootAttributes rootAttr = NetCDFRootAttributes.createDefault(location, location);
            rootAttr.write(writer);
            
            // add dimensions
            Dimension latDim = writer.addDimension(null, "lat", 64);
            Dimension lonDim = writer.addDimension(null, "lon", 128);
            
            // add Variable double temperature(lat,lon)
            List<Dimension> dims = new ArrayList<Dimension>();
            dims.add(latDim);
            dims.add(lonDim);
            
            NetCDFVarElement tempVar = new NetCDFVarElement("temperature").setLongName("temperature").setUnits("K")
                    .setDataType(DataType.DOUBLE).setDimensions(dims);        
            
            ArrayDouble tempArray = (ArrayDouble) tempVar.createDataArray();
            int[] counter = new int[tempArray.getShape().length];
            Arrays.fill(counter, 0);
            double d = 15.2;
            do {
                tempVar.insertData(d++, counter);
            } while(NetCDFUtils.advanceLoopCounter(tempArray.getShape(), counter) != null);
            
            tempVar.writeVariable(writer);
            
            writer.create();
            
            tempVar.writeData(writer);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        
    }
    public static void main1(String[] args) throws IOException {
        String location = "testWrite.nc";
        NetcdfFileWriter writer = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf3, location, null);

        // add dimensions
        Dimension latDim = writer.addDimension(null, "lat", 64);
        Dimension lonDim = writer.addDimension(null, "lon", 128);

        // add Variable double temperature(lat,lon)
        List<Dimension> dims = new ArrayList<Dimension>();
        dims.add(latDim);
        dims.add(lonDim);
        Variable t = writer.addVariable(null, "temperature", DataType.DOUBLE, dims);
        t.addAttribute(new Attribute("units", "K")); // add a 1D attribute of length 3
        Array data = Array.factory(DataType.INT, new int[] { 3 }, new int[] { 1, 2, 3 });
        t.addAttribute(new Attribute("scale", data));
        
        writer.addDimension(null, "svar_len", 80);
        writer.addVariable(null, "svar", DataType.CHAR, "svar_len");

        writer.addDimension(null, "names", 3);
        writer.addVariable(null, "names", DataType.CHAR, "names svar_len");

        // add a scalar variable
        writer.addVariable(null, "scalar", DataType.DOUBLE, new ArrayList<Dimension>());

        // add global attributes
        writer.addGroupAttribute(null, new Attribute("yo", "face"));
        writer.addGroupAttribute(null, new Attribute("versionD", 1.2));
        writer.addGroupAttribute(null, new Attribute("versionF", (float) 1.2));
        writer.addGroupAttribute(null, new Attribute("versionI", 1));
        writer.addGroupAttribute(null, new Attribute("versionS", (short) 2));
        writer.addGroupAttribute(null, new Attribute("versionB", (byte) 3));

        // create the file
        try {
            writer.create();
        }
        catch (IOException e) {
            System.err.printf("ERROR creating file %s%n%s", location, e.getMessage());
        }
        writer.close();
    }

}
