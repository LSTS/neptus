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
 * Nov 30, 2012
 * $Id:: NetcdfTimeSeriesExporter.java 9615 2012-12-30 23:08:28Z pdias          $:
 */
package pt.up.fe.dceg.neptus.plugins.netcdf;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Vector;

import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.imc.lsf.LsfIndex;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import ucar.ma2.DataType;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriteable;

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
        NetcdfFileWriteable file = new NetcdfFileWriteable(outputFile.getAbsolutePath(), true);
        Dimension timeDim = file.addDimension("time", (int) (endTime-startTime));
        Vector<LocationType> locations = new Vector<>();
        LinkedHashMap<String, Vector<Double>> scalars = new LinkedHashMap<>();
        LinkedHashMap<String, Integer> lsfIndexes = new LinkedHashMap<>();
        
        file.addVariable("latitude", DataType.DOUBLE, new Dimension[] { timeDim });
        file.addVariable("longitude", DataType.DOUBLE, new Dimension[] { timeDim });
        file.addVariable("depth", DataType.DOUBLE, new Dimension[] { timeDim });

        scalars.put("latitude", new Vector<Double>());
        scalars.put("longitude", new Vector<Double>());
        scalars.put("depth", new Vector<Double>());
        file.addVariableAttribute("latitude", "units", "degrees_north");
        file.addVariableAttribute("longitude", "units", "degrees_east");
        file.addVariableAttribute("depth", "units", "meters");

        for (ImcField f : scalarsToExport) {
            file.addVariable(f.getVarName(), DataType.DOUBLE, new Dimension[] { timeDim });
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
            scalars.get("latitude").add(loc.getLatitudeAsDoubleValue());
            scalars.get("longitude").add(loc.getLongitudeAsDoubleValue());
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
        
        System.out.println(scalars);
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
