/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
 * Nov 30, 2012
 */
package pt.lsts.neptus.plugins.netcdf;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Vector;

import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.lsf.LsfIndex;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.types.coord.LocationType;
import ucar.ma2.DataType;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriter;
//import ucar.nc2.NetcdfFileWriteable;

/**
 * @author zp
 */
@SuppressWarnings("deprecation")
public class NetcdfTimeSeriesExporter {

    protected LsfIndex index;
    protected Vector<ImcField> scalarsToExport = new Vector<>();
    protected double startTime, endTime;
    
    public NetcdfTimeSeriesExporter(LsfIndex index, final Vector<ImcField> ImcFieldsToExport) {
        this.index = index;
        this.scalarsToExport = ImcFieldsToExport;
        startTime = Math.ceil(index.getStartTime());
        endTime = Math.floor(index.getEndTime());
    }

    public void export(File outputFile) throws Exception {
        try(NetcdfFileWriter file = NetcdfFileWriter.createNew(outputFile.getAbsolutePath(), true)) {
            Dimension timeDim = file.addDimension("time", (int) (endTime-startTime));
            Vector<LocationType> locations = new Vector<>();
            LinkedHashMap<String, Vector<Double>> scalars = new LinkedHashMap<>();
            LinkedHashMap<String, Integer> lsfIndexes = new LinkedHashMap<>();
            
            List<Dimension> tmpDims = new ArrayList<Dimension>();
            tmpDims.add(timeDim);
            file.addVariable("latitude", DataType.DOUBLE, tmpDims);
            file.addVariable("longitude", DataType.DOUBLE, tmpDims);
            file.addVariable("depth", DataType.DOUBLE, tmpDims);
            
            scalars.put("latitude", new Vector<Double>());
            scalars.put("longitude", new Vector<Double>());
            scalars.put("depth", new Vector<Double>());
            file.addVariableAttribute("latitude", "units", "degrees_north");
            file.addVariableAttribute("longitude", "units", "degrees_east");
            file.addVariableAttribute("depth", "units", "meters");
            
            for (ImcField f : scalarsToExport) {
                file.addVariable(f.getVarName(), DataType.DOUBLE, tmpDims);
                scalars.put(f.getVarName(), new Vector<Double>());
                lsfIndexes.put(f.getVarName(), 0);
            }
            
            
            
            for (ImcField f : scalarsToExport) {
                if (index.getDefinitions().getType(f.getMessage()).getFieldUnits(f.getField()) != null)
                    file.addVariableAttribute(f.getVarName(), "units",
                            index.getDefinitions().getType(f.getMessage()).getFieldUnits(f.getField()));
            }
            
            
            int stateId = index.getDefinitions().getMessageId("EstimatedState");
            int curIndex = 0;
            for (double time = startTime; time < endTime; time++) {
                curIndex = index.getMessageAtOrAfer(stateId, 255, curIndex, time);
                if (curIndex == -1)
                    break;
                
                IMCMessage m = index.getMessage(curIndex);
                double lat = Math.toDegrees(m.getDouble("lat"));
                double lon = Math.toDegrees(m.getDouble("lon"));
                
                LocationType loc = new LocationType(lat, lon);
                loc.translatePosition(m.getDouble("x"), m.getDouble("y"), 0);            
                locations.add(loc);
                loc.convertToAbsoluteLatLonDepth();
                scalars.get("latitude").add(loc.getLatitudeDegs());
                scalars.get("longitude").add(loc.getLongitudeDegs());
                scalars.get("depth").add(m.getDouble("depth"));
                
                for (ImcField f : scalarsToExport) {
                    int idx = index.getMessageAtOrAfer(index.getDefinitions().getMessageId(f.getMessage()),
                            index.getEntityId(f.getEntity()), lsfIndexes.get(f.getVarName()), time);
                    lsfIndexes.put(f.getVarName(), idx);
                    try {
                        if (idx != -1)
                            scalars.get(f.getVarName()).add(index.getMessage(idx).getDouble(f.getField()));
                        else
                            scalars.get(f.getVarName()).add(0d);
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            NeptusLog.pub().info("<###> "+scalars);
        }
        catch (Exception e) {
            NeptusLog.pub().warn(e.getMessage());
        }
    }
    
    public static void main(String[] args) throws Exception {
        Vector<ImcField> fields = new Vector<>();
        fields.add(new ImcField("Temperature", "value", "CTD"));
        fields.add(new ImcField("Conductivity", "value", "CTD"));
        fields.add(new ImcField("Salinity", "value", "CTD"));
        LsfIndex index = new LsfIndex(new File("/home/zp/Desktop/logs/20121123/134346_rows_1mps_-3m/Data.lsf"));
        NetcdfTimeSeriesExporter exporter = new NetcdfTimeSeriesExporter(index, fields);
        exporter.export(new File("x.x"));
    }
}
