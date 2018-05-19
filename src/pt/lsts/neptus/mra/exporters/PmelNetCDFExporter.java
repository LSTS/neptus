/*
 * Copyright (c) 2004-2018 Universidade do Porto - Faculdade de Engenharia
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
 * May 19, 2018
 */
package pt.lsts.neptus.mra.exporters;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import javax.swing.ProgressMonitor;

import com.google.common.collect.Lists;

import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.gui.editor.StringListEditor;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.importers.IMraLog;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.AngleUtils;
import pt.lsts.neptus.util.DateTimeUtil;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.netcdf.exporter.NetCDFExportWriter;
import pt.lsts.neptus.util.netcdf.exporter.NetCDFRootAttributes;
import pt.lsts.neptus.util.netcdf.exporter.NetCDFVarElement;
import ucar.ma2.DataType;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriter;

/**
 * @author pdias
 *
 */
@PluginDescription(name = "PMEL netCDF Exporter")
public class PmelNetCDFExporter extends MRAExporterFilter {

    @SuppressWarnings("serial")
    public static final SimpleDateFormat dateTimeFormatterISO8601NoMillis = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", new Locale("en")) {{setTimeZone(TimeZone.getTimeZone("UTC"));}};
    
//    @NeptusProperty(name = "Write Enumerated and Bitfield as String", editable = true)
//    private boolean flagWriteEnumeratedAndBitfieldAsString = true;
    @NeptusProperty(name = "Message List to Export", editorClass = StringListEditor.class,
            description = "List of messages to export (comma separated values, no spaces). Use '!' at the begining to make it an exclude list.")
    public String msgList = "";
    @NeptusProperty(name = "Sample Period", units = "s", description = "Sample period to use for data (min = 1s)")
    private int dataSamplePeriod = 1; 
    @NeptusProperty(name = "Exported file")
    private File exportFile = new File("pmel.nc"); 

    private IMraLogGroup source;
    
    /**
     * @param source
     */
    public PmelNetCDFExporter(IMraLogGroup source) {
        super(source);
    }

    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        return true;
    }

    @Override
    public String process(IMraLogGroup logSource, ProgressMonitor pmonitor) {
        this.source = logSource;
        double startTimeSec = Math.ceil(source.getLsfIndex().getStartTime());
        double endTimeSec = Math.floor(source.getLsfIndex().getEndTime());
        
        File outFolder = new File(source.getDir(), "mra/netcdf");
        outFolder.mkdir();
        exportFile = new File(outFolder, "pmel-" + DateTimeUtil.dateTimeFormatterISO8601_2.format(new Date((long) (endTimeSec * 1E3))) + "nc");
        
        if (PluginUtils.editPluginProperties(this,  true))
            return I18n.text("Cancelled by the user.");

        String tmpList = msgList.trim();
        boolean includeList = true;
        if (tmpList.isEmpty() || tmpList.startsWith("!")) {
            includeList = false;
            if (!tmpList.isEmpty())
                tmpList = tmpList.replaceFirst("!", "");
        }
        String[] lst = tmpList.split(",");
        List<String> messagesList = Lists.newArrayList(lst);

        Collection<String> logList = source.getLsfIndex().getDefinitions().getMessageNames();
        IMraLog parser;

        String location = exportFile.getName();
        try (NetcdfFileWriter writer = NetCDFExportWriter.createWriter(exportFile)) {
            NetCDFRootAttributes rootAttr = NetCDFRootAttributes.createDefault(location, location);

            int logVehicleId = source.getVehicleSources().stream().findFirst().get();
            
            // Min sample rate is 1s
            dataSamplePeriod = Math.max(1, dataSamplePeriod);
            
            int obsNumber = (int) Math.round((endTimeSec - startTimeSec) / dataSamplePeriod);
            
            rootAttr.write(writer);
            
            // add dimensions
            Dimension trajDim = writer.addDimension(null, "trajectory", 1);
            Dimension obsDim = writer.addDimension(null, "obs", obsNumber);

            List<Dimension> dimsTraj = new ArrayList<Dimension>();
            dimsTraj.add(trajDim);

            List<Dimension> dims = new ArrayList<Dimension>();
            dims.add(trajDim);
            dims.add(obsDim);

            List<NetCDFVarElement> varsList = new ArrayList<>();
            
            Date startDate = new Date((long) (startTimeSec * 1E3));
            NetCDFVarElement timeVar = new NetCDFVarElement("time").setLongName("time").setStandardName("time")
                    .setUnits("seconds since " + dateTimeFormatterISO8601NoMillis.format(startDate))
                    .setDataType(DataType.INT).setDimensions(dims).setAtribute("axis", "T");
            timeVar.createDataArray().setUnsigned(true);
            varsList.add(timeVar);

            NetCDFVarElement latVar = new NetCDFVarElement("lat").setLongName("latitude").setStandardName("latitude")
                    .setUnits("degrees_north").setDataType(DataType.DOUBLE).setDimensions(dims).setAtribute("axis", "Y")
                    .setAtribute("_FillValue", "-9999").setAtribute("valid_min", "-90").setAtribute("valid_max", "90");
            varsList.add(latVar);

            NetCDFVarElement lonVar = new NetCDFVarElement("lon").setLongName("longitude").setStandardName("longitude")
                    .setUnits("degrees_east").setDataType(DataType.DOUBLE).setDimensions(dims).setAtribute("axis", "X")
                    .setAtribute("_FillValue", "-9999").setAtribute("valid_min", "-180")
                    .setAtribute("valid_max", "180");
            varsList.add(lonVar);

            // scaled as 0.1
            NetCDFVarElement depthVar = new NetCDFVarElement("depth").setLongName("depth").setStandardName("depth")
                    .setUnits("m").setDataType(DataType.INT).setDimensions(dims).setAtribute("axis", "Z")
                    .setAtribute("_FillValue", "-9999").setAtribute("valid_min", "0").setAtribute("scale_factor", "0.1")
                    .setAtribute("positive", "down").setAtribute("_CoordinateAxisType", "Depth")
                    .setAtribute("_CoordinateZisPositive", "down");
            varsList.add(depthVar);

            NetCDFVarElement trajVar = new NetCDFVarElement("trajectory").setLongName("trajectory")
                    .setDataType(DataType.INT).setDimensions(dimsTraj);
            varsList.add(trajVar);

            trajVar.insertData(logVehicleId, 0);

            NetCDFVarElement cogVar = new NetCDFVarElement("cog").setLongName("course over ground")
                    .setStandardName("platform_course").setUnits("degree_T").setDataType(DataType.FLOAT)
                    .setDimensions(dims).setAtribute("_FillValue", Float.toString(Float.MAX_VALUE))
                    .setAtribute("coordinates", "time lat lon depth").setAtribute("valid_min", "-180")
                    .setAtribute("valid_max", "180");
            varsList.add(cogVar);

            NetCDFVarElement hdgVar = new NetCDFVarElement("hdg").setLongName("Vehicle heading")
                    .setStandardName("platform_yaw_angle").setUnits("degree_T").setDataType(DataType.FLOAT)
                    .setDimensions(dims).setAtribute("_FillValue", Float.toString(Float.MAX_VALUE))
                    .setAtribute("coordinates", "time lat lon depth").setAtribute("valid_min", "-180")
                    .setAtribute("valid_max", "180");
            varsList.add(hdgVar);

            NetCDFVarElement rollVar = new NetCDFVarElement("roll").setLongName("Vehicle roll")
                    .setStandardName("platform_roll_angle").setUnits("degree").setDataType(DataType.FLOAT)
                    .setDimensions(dims).setAtribute("_FillValue", Float.toString(Float.MAX_VALUE))
                    .setAtribute("coordinates", "time lat lon depth").setAtribute("valid_min", "-180")
                    .setAtribute("valid_max", "180");
            varsList.add(rollVar);

            NetCDFVarElement pitchVar = new NetCDFVarElement("pitch").setLongName("Vehicle pitch")
                    .setStandardName("platform_pitch_angle").setUnits("degree").setDataType(DataType.FLOAT)
                    .setDimensions(dims).setAtribute("_FillValue", Float.toString(Float.MAX_VALUE))
                    .setAtribute("coordinates", "time lat lon depth").setAtribute("valid_min", "-180")
                    .setAtribute("valid_max", "180");
            varsList.add(pitchVar);

            NetCDFVarElement sogVar = new NetCDFVarElement("sog").setLongName("Speed over ground")
                    .setStandardName("latform_speed_wrt_ground").setUnits("m s-1").setDataType(DataType.FLOAT)
                    .setDimensions(dims).setAtribute("_FillValue", Float.toString(Float.MAX_VALUE))
                    .setAtribute("coordinates", "time lat lon depth");
            varsList.add(sogVar);

            // pH sea_water_ph_reported_on_total_scale XP1 
            // redox (V) XP1
            // salinity (PSU) sea_water_practical_salinity or sea_water_salinity (1e-3) XP1 XP2 XP345 -> sal
            // temperature (Celcius) XP1 XP2 XP345 -> temp_cdt
            // water density (kg m-3) ea_water_density XP1
            // condutivity (S m-1) sea_water_electrical_conductivity XP1 XP2 XP345 -> cond
            // chrolophyl (ug l-1) ass_concentration_of_chlorophyll_in_sea_water (kg m-3) XP2 -> chlor
            // sound speed (m s-1) speed_of_sound_in_sea_water XP1 XP2 XP345
            // turbidity (ntu) sea_water_turbidity (sem unidade) XP2
            
            int stateId = source.getLsfIndex().getDefinitions().getMessageId("EstimatedState");
            int curIndex = 0;
            for (int idx = 0; startTimeSec + idx < endTimeSec; idx++) {
                double time = startTimeSec + idx;
                curIndex = source.getLsfIndex().getMessageAtOrAfer(stateId, 255, curIndex, time);
                if (curIndex == -1)
                    break;
                
                IMCMessage m = source.getLsfIndex().getMessage(curIndex);
                double lat = Math.toDegrees(m.getDouble("lat"));
                double lon = Math.toDegrees(m.getDouble("lon"));
                
                LocationType loc = new LocationType(lat, lon);
                loc.translatePosition(m.getDouble("x"), m.getDouble("y"), 0);            
                loc.convertToAbsoluteLatLonDepth();

                timeVar.insertData(idx, 0, idx);
                latVar.insertData(loc.getLatitudeDegs(), 0, idx);
                lonVar.insertData(loc.getLongitudeDegs(), 0, idx);
                depthVar.insertData(m.getDouble("depth") * 10, 0, idx);
                
                double vx = m.getDouble("vx");
                double vy = m.getDouble("vy");
                double vz = m.getDouble("vz");

                double courseRad = AngleUtils.calcAngle(0, 0, vy, vx);
                double groundSpeed = Math.sqrt(vx * vx + vy * vy);
                double verticalSpeed = vz;

                cogVar.insertData(AngleUtils.nomalizeAngleDegrees180(Math.toDegrees(courseRad)), 0, idx);
                hdgVar.insertData(AngleUtils.nomalizeAngleDegrees180(Math.toDegrees(m.getDouble("psi"))), 0, idx);
                rollVar.insertData(AngleUtils.nomalizeAngleDegrees180(Math.toDegrees(m.getDouble("phi"))), 0, idx);
                pitchVar.insertData(AngleUtils.nomalizeAngleDegrees180(Math.toDegrees(m.getDouble("theta"))), 0, idx);

                sogVar.insertData(groundSpeed, 0, idx);

//                for (ImcField f : scalarsToExport) {
//                    int idx = index.getMessageAtOrAfer(index.getDefinitions().getMessageId(f.getMessage()),
//                            index.getEntityId(f.getEntity()), lsfIndexes.get(f.getVarName()), time);
//                    lsfIndexes.put(f.getVarName(), idx);
//                    try {
//                        if (idx != -1)
//                            scalars.get(f.getVarName()).add(index.getMessage(idx).getDouble(f.getField()));
//                        else
//                            scalars.get(f.getVarName()).add(0d);
//                    }
//                    catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                }
            }
            
            varsList.stream().forEach(v -> v.writeVariable(writer));
            writer.create();
            varsList.stream().forEach(v -> v.writeData(writer));
        }
        catch (Exception e) {
            NeptusLog.pub().error("Error processing . " + e.getMessage() + "!");
            String txt = I18n.textf("Error processing with %error error.", e.getMessage());
            if (pmonitor != null)
                pmonitor.setNote(txt);
            GuiUtils.errorMessage(this.getClass().getSimpleName(), txt);
        }
        catch (OutOfMemoryError e) {
            NeptusLog.pub().error("Error processing . OutOfMemoryError!");
            String txt = I18n.textf("Error processing with %error error.", "OutOfMemoryError");
            if (pmonitor != null)
                pmonitor.setNote(txt);
            GuiUtils.errorMessage(this.getClass().getSimpleName(), txt);
        }
        
        if (pmonitor != null) {
            pmonitor.setNote(I18n.text("Log exported to PMEL netCDF successfully"));
            pmonitor.setProgress(100);
        }
        return I18n.text("Log exported to PMEL netCDF successfully");

    }
}
