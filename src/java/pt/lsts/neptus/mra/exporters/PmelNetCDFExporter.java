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
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import javax.swing.ProgressMonitor;

import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.mra.api.CorrectedPosition;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.AngleUtils;
import pt.lsts.neptus.util.DateTimeUtil;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.conf.ConfigFetch;
import pt.lsts.neptus.util.netcdf.NetCDFUtils;
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

    private static final double DEPTH_SCALE = 0.001;

    @SuppressWarnings("serial")
    public static final SimpleDateFormat dateTimeFormatterISO8601NoMillis = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", new Locale("en")) {{setTimeZone(TimeZone.getTimeZone("UTC"));}};
    
//    @NeptusProperty(name = "Write Enumerated and Bitfield as String", editable = true)
//    private boolean flagWriteEnumeratedAndBitfieldAsString = true;
//    @NeptusProperty(name = "Message List to Export", editorClass = StringListEditor.class,
//            description = "List of messages to export (comma separated values, no spaces). Use '!' at the begining to make it an exclude list.")
//    public String msgList = "";
    @NeptusProperty(name = "Use Corrected Positions")
    private boolean useCorrectedPositions = true; 
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
        
        String sysNameForFile = "";
        try {
            int logVehicleId = source.getVehicleSources().stream().findFirst().get();
            sysNameForFile = "-" + source.getSystemName(logVehicleId);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        File outFolder = new File(source.getFile("mra"), "netcdf");
        outFolder.mkdir();
        exportFile = new File(outFolder,
                "pmel-" + DateTimeUtil.dateTimeFormatterISO8601_2.format(new Date((long) (endTimeSec * 1E3)))
                        + sysNameForFile + ".nc");
        
        if (PluginUtils.editPluginProperties(this, ConfigFetch.getSuperParentAsFrame(),  true))
            return I18n.text("Cancelled by the user.");

        String location = exportFile.getName();
        try (NetcdfFileWriter writer = NetCDFExportWriter.createWriter(exportFile)) {
            int logVehicleId = source.getVehicleSources().stream().findFirst().get();
            
            NetCDFRootAttributes rootAttr = NetCDFRootAttributes.createDefault(location, location);
            rootAttr.setDateModified(new Date((long) (source.getLsfIndex().getEndTime() * 1E3)))
                    .setId(source.getSystemName(logVehicleId) + "-" + location)
                    .setAtribute("featureType", "trajectory");
            
            // Min sample rate is 1s
            dataSamplePeriod = Math.max(1, dataSamplePeriod);
            
            int obsNumber = (int) Math.round((endTimeSec - startTimeSec) / dataSamplePeriod);
            
            rootAttr.write(writer);
            
            // add dimensions
//             Dimension trajDim = writer.addDimension(null, "trajectory", 1);
            Dimension obsDim = writer.addDimension(null, "obs", obsNumber);
            Dimension nameStrlenDim = writer.addDimension(null, "name_strlen", 30);

            List<Dimension> dimsTraj = new ArrayList<Dimension>();
            dimsTraj.add(nameStrlenDim);

            List<Dimension> dims = new ArrayList<Dimension>();
            //ndims.add(trajDim);
            dims.add(obsDim);

            List<NetCDFVarElement> varsList = new ArrayList<>();

            Date startDate =  DateTimeUtil.REF_DATE; //new Date((long) (startTimeSec * 1E3));
            NetCDFVarElement timeVar = new NetCDFVarElement("time").setLongName("time").setStandardName("time")
                    .setUnits("seconds since " + dateTimeFormatterISO8601NoMillis.format(startDate))
                    .setDataType(DataType.DOUBLE).setDimensions(dims).setAtribute("axis", "T");
            // timeVar.createDataArray().setUnsigned(true);
            varsList.add(timeVar);

            NetCDFVarElement latVar = new NetCDFVarElement("lat").setLongName("latitude").setStandardName("latitude")
                    .setUnits("degrees_north").setDataType(DataType.DOUBLE).setDimensions(dims).setAtribute("axis", "Y")
                    .setAtribute(NetCDFUtils.NETCDF_ATT_FILL_VALUE, -9999.)
                    .setAtribute(NetCDFUtils.NETCDF_ATT_MISSING_VALUE, -9999.).setAtribute("valid_min", -90.)
                    .setAtribute("valid_max", 90.).setAtribute(NetCDFUtils.NETCDF_ATT_VALID_RANGE, new double[] {-90., 90.});
            varsList.add(latVar);

            NetCDFVarElement lonVar = new NetCDFVarElement("lon").setLongName("longitude").setStandardName("longitude")
                    .setUnits("degrees_east").setDataType(DataType.DOUBLE).setDimensions(dims).setAtribute("axis", "X")
                    .setAtribute(NetCDFUtils.NETCDF_ATT_FILL_VALUE, -9999.)
                    .setAtribute(NetCDFUtils.NETCDF_ATT_MISSING_VALUE, -9999.).setAtribute("valid_min", -180.)
                    .setAtribute("valid_max", 180.).setAtribute(NetCDFUtils.NETCDF_ATT_VALID_RANGE, new double[] {-180., 180.});
            varsList.add(lonVar);

            // scaled as 0.1
            NetCDFVarElement depthVar = new NetCDFVarElement("depth").setLongName("depth").setStandardName("depth")
                    .setUnits("m").setDataType(DataType.INT).setDimensions(dims).setAtribute("axis", "Z")
                    .setAtribute(NetCDFUtils.NETCDF_ATT_FILL_VALUE, -9999)
                    .setAtribute(NetCDFUtils.NETCDF_ATT_MISSING_VALUE, -9999).setAtribute("valid_min", 0)
                    .setAtribute("scale_factor", DEPTH_SCALE).setAtribute("positive", "down")
                    .setAtribute("_CoordinateAxisType", "Depth").setAtribute("positive", "down")
                    .setAtribute("coordinates", "time lat lon");
            varsList.add(depthVar);

            NetCDFVarElement trajVar = new NetCDFVarElement("trajectory").setLongName("trajectory")
                    .setDataType(DataType.CHAR).setDimensions(dimsTraj).setAtribute("cf_role", "trajectory_id");
            varsList.add(trajVar);

            NetCDFVarElement cogVar = new NetCDFVarElement("cog").setLongName("course over ground")
                    .setStandardName("platform_course").setUnits("degree_T").setDataType(DataType.FLOAT)
                    .setDimensions(dims).setAtribute(NetCDFUtils.NETCDF_ATT_FILL_VALUE, Float.NaN)
                    .setAtribute(NetCDFUtils.NETCDF_ATT_MISSING_VALUE, Float.NaN)
                    .setAtribute("coordinates", "time depth lat lon").setAtribute("valid_min", -180f)
                    .setAtribute("valid_max", 180f);
            varsList.add(cogVar);

            NetCDFVarElement hdgVar = new NetCDFVarElement("hdg").setLongName("Vehicle heading")
                    .setStandardName("platform_yaw_angle").setUnits("degree_T").setDataType(DataType.FLOAT)
                    .setDimensions(dims).setAtribute(NetCDFUtils.NETCDF_ATT_FILL_VALUE, Float.NaN)
                    .setAtribute(NetCDFUtils.NETCDF_ATT_MISSING_VALUE, Float.NaN)
                    .setAtribute("coordinates", "time depth lat lon").setAtribute("valid_min", -180f)
                    .setAtribute("valid_max", 180f);
            varsList.add(hdgVar);

            NetCDFVarElement rollVar = new NetCDFVarElement("roll").setLongName("Vehicle roll")
                    .setStandardName("platform_roll_angle").setUnits("degree").setDataType(DataType.FLOAT)
                    .setDimensions(dims).setAtribute(NetCDFUtils.NETCDF_ATT_FILL_VALUE, Float.NaN)
                    .setAtribute(NetCDFUtils.NETCDF_ATT_MISSING_VALUE, Float.NaN)
                    .setAtribute("coordinates", "time depth lat lon").setAtribute("valid_min", -180f)
                    .setAtribute("valid_max", 180f);
            varsList.add(rollVar);

            NetCDFVarElement pitchVar = new NetCDFVarElement("pitch").setLongName("Vehicle pitch")
                    .setStandardName("platform_pitch_angle").setUnits("degree").setDataType(DataType.FLOAT)
                    .setDimensions(dims).setAtribute(NetCDFUtils.NETCDF_ATT_FILL_VALUE, Float.NaN)
                    .setAtribute(NetCDFUtils.NETCDF_ATT_MISSING_VALUE, Float.NaN)
                    .setAtribute("coordinates", "time depth lat lon").setAtribute("valid_min", -180f)
                    .setAtribute("valid_max", 180f);
            varsList.add(pitchVar);

            NetCDFVarElement sogVar = new NetCDFVarElement("sog").setLongName("Speed over ground")
                    .setStandardName("platform_speed_wrt_ground").setUnits("m s-1").setDataType(DataType.FLOAT)
                    .setDimensions(dims).setAtribute(NetCDFUtils.NETCDF_ATT_FILL_VALUE, Float.NaN)
                    .setAtribute(NetCDFUtils.NETCDF_ATT_MISSING_VALUE, Float.NaN)
                    .setAtribute("coordinates", "time depth lat lon");
            varsList.add(sogVar);

            // pH sea_water_ph_reported_on_total_scale XP1 
            // redox (V) XP1
            // salinity (PSU) sea_water_practical_salinity or sea_water_salinity (1e-3) XP1 XP2 XP345 -> sal
            // temperature (Celcius) XP1 XP2 XP345 -> temp_cdt
            // water density (kg m-3) sea_water_density XP1
            // condutivity (S m-1) sea_water_electrical_conductivity XP1 XP2 XP345 -> cond
            // chrolophyl (ug l-1) mass_concentration_of_chlorophyll_in_sea_water (kg m-3) XP2 -> chlor
            // sound speed (m s-1) speed_of_sound_in_sea_water XP1 XP2 XP345
            // turbidity (ntu) sea_water_turbidity (sem unidade) XP2
            
            pmonitor.setMinimum(0);
            pmonitor.setMaximum(source.getLsfIndex().getNumberOfMessages());
            pmonitor.setNote(I18n.text("Processing..."));
            
            pmonitor.setNote(I18n.text("Generating corrected positions..."));
            pmonitor.setProgress(1);
            CorrectedPosition cp = new CorrectedPosition(source);
            
            pmonitor.setNote("Exporting...");
            
            boolean containsCondutivity = source.getLsfIndex().containsMessagesOfType("Conductivity");
            boolean containsTemperature = source.getLsfIndex().containsMessagesOfType("Temperature");
            boolean containsSalinity = source.getLsfIndex().containsMessagesOfType("Salinity");
            boolean containsWaterDensity = source.getLsfIndex().containsMessagesOfType("WaterDensity");
            boolean containsChlorophyll = source.getLsfIndex().containsMessagesOfType("Chlorophyll");
            boolean containsTurbidity = source.getLsfIndex().containsMessagesOfType("Turbidity");
            boolean containsPH = source.getLsfIndex().containsMessagesOfType("PH");
            boolean containsRedox = source.getLsfIndex().containsMessagesOfType("Redox");
            boolean containsSoundSpeed = source.getLsfIndex().containsMessagesOfType("SoundSpeed");
            
            boolean containsDissolvedOrganicMatter = source.getLsfIndex().containsMessagesOfType("DissolvedOrganicMatter");
            boolean containsDissolvedOxygen = source.getLsfIndex().containsMessagesOfType("DissolvedOxygen");
            boolean containsAirSaturation = source.getLsfIndex().containsMessagesOfType("AirSaturation"); // %
            boolean containsOpticalBackscatter = source.getLsfIndex().containsMessagesOfType("OpticalBackscatter"); // m^⁻1

            NetCDFVarElement condVar = null;
            if (containsCondutivity) {
                condVar = new NetCDFVarElement("cond").setLongName("Conductivity")
                        .setStandardName("sea_water_electrical_conductivity").setUnits("S m-1").setDataType(DataType.FLOAT)
                        .setDimensions(dims).setAtribute(NetCDFUtils.NETCDF_ATT_FILL_VALUE, Float.NaN)
                        .setAtribute(NetCDFUtils.NETCDF_ATT_MISSING_VALUE, Float.NaN)
                        .setAtribute("coordinates", "time depth lat lon");
                varsList.add(condVar);
            }
            NetCDFVarElement tempVar = null;
            if (containsTemperature) {
                tempVar = new NetCDFVarElement("temp_ctd").setLongName("Temperature CTD")
                        .setStandardName("sea_water_temperature").setUnits("degree_C").setDataType(DataType.FLOAT)
                        .setDimensions(dims).setAtribute(NetCDFUtils.NETCDF_ATT_FILL_VALUE, Float.NaN)
                        .setAtribute(NetCDFUtils.NETCDF_ATT_MISSING_VALUE, Float.NaN)
                        .setAtribute("coordinates", "time depth lat lon");
                varsList.add(tempVar);
            }
            NetCDFVarElement salVar = null;
            if (containsSalinity) {
                salVar = new NetCDFVarElement("sal").setLongName("Salinity")
                        .setStandardName("sea_water_practical_salinity").setUnits("PSU").setDataType(DataType.FLOAT)
                        .setDimensions(dims).setAtribute(NetCDFUtils.NETCDF_ATT_FILL_VALUE, Float.NaN)
                        .setAtribute(NetCDFUtils.NETCDF_ATT_MISSING_VALUE, Float.NaN)
                        .setAtribute("coordinates", "time depth lat lon");
                varsList.add(salVar);
            }
            NetCDFVarElement waterDensityVar = null;
            if (containsWaterDensity) {
                waterDensityVar = new NetCDFVarElement("waterdensity").setLongName("Sea Water Density")
                        .setStandardName("sea_water_density").setUnits("kg m-3").setDataType(DataType.FLOAT)
                        .setDimensions(dims).setAtribute(NetCDFUtils.NETCDF_ATT_FILL_VALUE, Float.NaN)
                        .setAtribute(NetCDFUtils.NETCDF_ATT_MISSING_VALUE, Float.NaN)
                        .setAtribute("coordinates", "time depth lat lon");
                varsList.add(waterDensityVar);
            }
            NetCDFVarElement chlorophyllVar = null;
            if (containsChlorophyll) {
                chlorophyllVar = new NetCDFVarElement("chlor").setLongName("Chlorophyll")
                        .setStandardName("mass_concentration_of_chlorophyll_in_sea_water").setUnits("ug l-1").setDataType(DataType.FLOAT)
                        .setDimensions(dims).setAtribute(NetCDFUtils.NETCDF_ATT_FILL_VALUE, Float.NaN)
                        .setAtribute(NetCDFUtils.NETCDF_ATT_MISSING_VALUE, Float.NaN)
                        .setAtribute("coordinates", "time depth lat lon");
                varsList.add(chlorophyllVar);
            }
            NetCDFVarElement turbidityVar = null;
            if (containsTurbidity) {
                turbidityVar = new NetCDFVarElement("turbidity").setLongName("Turbidity")
                        .setStandardName("sea_water_turbidity").setUnits("NTU").setDataType(DataType.FLOAT)
                        .setDimensions(dims).setAtribute(NetCDFUtils.NETCDF_ATT_FILL_VALUE, Float.NaN)
                        .setAtribute(NetCDFUtils.NETCDF_ATT_MISSING_VALUE, Float.NaN)
                        .setAtribute("coordinates", "time depth lat lon");
                varsList.add(turbidityVar);
            }
            NetCDFVarElement phVar = null;
            if (containsPH) {
                phVar = new NetCDFVarElement("ph").setLongName("pH")
                        .setStandardName("sea_water_ph_reported_on_total_scale").setUnits("").setDataType(DataType.FLOAT)
                        .setDimensions(dims).setAtribute(NetCDFUtils.NETCDF_ATT_FILL_VALUE, Float.NaN)
                        .setAtribute(NetCDFUtils.NETCDF_ATT_MISSING_VALUE, "NaN")
                        .setAtribute("coordinates", "time depth lat lon");
                varsList.add(phVar);
            }
            NetCDFVarElement redoxVar = null;
            if (containsRedox) {
                redoxVar = new NetCDFVarElement("redox").setLongName("Redox")
                        .setStandardName("").setUnits("V").setDataType(DataType.FLOAT)
                        .setDimensions(dims).setAtribute(NetCDFUtils.NETCDF_ATT_FILL_VALUE, Float.NaN)
                        .setAtribute(NetCDFUtils.NETCDF_ATT_MISSING_VALUE, Float.NaN)
                        .setAtribute("coordinates", "time depth lat lon");
                varsList.add(redoxVar);
            }
            NetCDFVarElement soundSpeedVar = null;
            if (containsSoundSpeed) {
                soundSpeedVar = new NetCDFVarElement("soundspeed").setLongName("Sound Speed in the Sea Water")
                        .setStandardName("speed_of_sound_in_sea_water").setUnits("m s-1").setDataType(DataType.FLOAT)
                        .setDimensions(dims).setAtribute(NetCDFUtils.NETCDF_ATT_FILL_VALUE, Float.NaN)
                        .setAtribute(NetCDFUtils.NETCDF_ATT_MISSING_VALUE, Float.NaN)
                        .setAtribute("coordinates", "time depth lat lon");
                varsList.add(soundSpeedVar);
            }

            NetCDFVarElement cdomVar = null;
            if (containsDissolvedOrganicMatter) {
                cdomVar = new NetCDFVarElement("cdom").setLongName("CDOM")
                        .setStandardName("concentration_of_colored_dissolved_organic_matter_in_sea_water_expressed_as_equivalent_mass_fraction_of_quinine_sulfate_dihydrate")
                        .setUnits("").setDataType(DataType.FLOAT)
                        .setDimensions(dims).setAtribute(NetCDFUtils.NETCDF_ATT_FILL_VALUE, Float.NaN)
                        .setAtribute(NetCDFUtils.NETCDF_ATT_MISSING_VALUE, Float.NaN)
                        .setAtribute("coordinates", "time depth lat lon");
                varsList.add(cdomVar);
            }
            NetCDFVarElement dissolvedOxygenVar = null;
            if (containsDissolvedOxygen) {
                dissolvedOxygenVar = new NetCDFVarElement("dissolved_oxygen").setLongName("Dissolved Oxygen")
                        .setStandardName("mole_concentration_of_dissolved_molecular_oxygen_in_sea_water")
                        .setUnits("\u00B5mol l-1").setDataType(DataType.FLOAT)
                        .setDimensions(dims).setAtribute(NetCDFUtils.NETCDF_ATT_FILL_VALUE, Float.NaN)
                        .setAtribute(NetCDFUtils.NETCDF_ATT_MISSING_VALUE, Float.NaN)
                        .setAtribute("coordinates", "time depth lat lon");
                varsList.add(dissolvedOxygenVar);
            }
            NetCDFVarElement airSaturationVar = null;
            if (containsAirSaturation) {
                airSaturationVar = new NetCDFVarElement("air_saturation").setLongName("Air Saturation")
                        // .setStandardName("")
                        .setUnits("%").setDataType(DataType.FLOAT)
                        .setDimensions(dims).setAtribute(NetCDFUtils.NETCDF_ATT_FILL_VALUE, Float.NaN)
                        .setAtribute(NetCDFUtils.NETCDF_ATT_MISSING_VALUE, Float.NaN)
                        .setAtribute("coordinates", "time depth lat lon");
                varsList.add(airSaturationVar);
            }
            NetCDFVarElement opticalBackscatterVar = null;
            if (containsOpticalBackscatter) {
                opticalBackscatterVar = new NetCDFVarElement("optical_backscatter").setLongName("Optical Backscatter")
                        // .setStandardName("")
                        .setUnits("m-1").setDataType(DataType.FLOAT)
                        .setDimensions(dims).setAtribute(NetCDFUtils.NETCDF_ATT_FILL_VALUE, Float.NaN)
                        .setAtribute(NetCDFUtils.NETCDF_ATT_MISSING_VALUE, Float.NaN)
                        .setAtribute("coordinates", "time depth lat lon");
                varsList.add(opticalBackscatterVar);
            }

            trajVar.insertData(source.getSystemName(logVehicleId).toCharArray());
            
            int curIndex = 0;
            for (int idx = 0; startTimeSec + idx < endTimeSec; idx++) {

                pmonitor.setProgress(curIndex);

                double time = startTimeSec + idx;
                
                IMCMessage m = getNextMessage(source, logVehicleId, curIndex, time, dataSamplePeriod, "EstimatedState");
                if (m != null) {
                    curIndex = source.getLsfIndex().getMsgIndexAt("EstimatedState", time);
                    
                    double lat = Math.toDegrees(m.getDouble("lat"));
                    double lon = Math.toDegrees(m.getDouble("lon"));
                    
                    LocationType loc = new LocationType(lat, lon);
                    loc.translatePosition(m.getDouble("x"), m.getDouble("y"), 0);            
                    loc.convertToAbsoluteLatLonDepth();
                    if (useCorrectedPositions && cp != null) {
                        SystemPositionAndAttitude pos = cp.getPosition(m.getTimestamp());
                        if (pos != null)
                            loc = pos.getPosition().getNewAbsoluteLatLonDepth();
                    }
                    
                    timeVar.insertData((time * 1E3 - startDate.getTime()) / 1E3, idx);
                    latVar.insertData(loc.getLatitudeDegs(), idx);
                    lonVar.insertData(loc.getLongitudeDegs(), idx);
                    depthVar.insertData(m.getDouble("depth") / DEPTH_SCALE, idx);
                    
                    double vx = m.getDouble("vx");
                    double vy = m.getDouble("vy");
                    // double vz = m.getDouble("vz");
                    
                    double courseRad = AngleUtils.calcAngle(0, 0, vy, vx);
                    double groundSpeed = Math.sqrt(vx * vx + vy * vy);
                    // double verticalSpeed = vz;
                    
                    cogVar.insertData(AngleUtils.nomalizeAngleDegrees180(Math.toDegrees(courseRad)), idx);
                    hdgVar.insertData(AngleUtils.nomalizeAngleDegrees180(Math.toDegrees(m.getDouble("psi"))), idx);
                    rollVar.insertData(AngleUtils.nomalizeAngleDegrees180(Math.toDegrees(m.getDouble("phi"))), idx);
                    pitchVar.insertData(AngleUtils.nomalizeAngleDegrees180(Math.toDegrees(m.getDouble("theta"))), idx);
                    
                    sogVar.insertData(groundSpeed, idx);
                    
                    int entityForTemp = -1;
                    if (containsCondutivity) {
                        IMCMessage msg = getNextMessage(source, logVehicleId, curIndex, time, dataSamplePeriod, "Conductivity");
                        if (msg != null) {
                            condVar.insertData(msg.getFloat("value"), idx);
                            entityForTemp = entityForTemp >= 0 ? entityForTemp : msg.getSrcEnt();
                        }
                        else {
                            condVar.insertData(Float.NaN, idx);
                        }
                    }
                    if (containsSalinity) {
                        IMCMessage msg = getNextMessage(source, logVehicleId, curIndex, time, dataSamplePeriod, "Salinity");
                        if (msg != null) {
                            salVar.insertData(msg.getFloat("value"), idx);
                            entityForTemp = entityForTemp >= 0 ? entityForTemp : msg.getSrcEnt();
                        }
                        else {
                            salVar.insertData(Float.NaN, idx);
                        }
                    }
                    if (containsWaterDensity) {
                        IMCMessage msg = getNextMessage(source, logVehicleId, curIndex, time, dataSamplePeriod, "WaterDensity");
                        if (msg != null) {
                            waterDensityVar.insertData(msg.getFloat("value"), idx);
                            entityForTemp = entityForTemp >= 0 ? entityForTemp : msg.getSrcEnt();
                        }
                        else {
                            waterDensityVar.insertData(Float.NaN, idx);
                        }
                    }
                    if (containsChlorophyll) {
                        IMCMessage msg = getNextMessage(source, logVehicleId, curIndex, time, dataSamplePeriod, "Chlorophyll");
                        if (msg != null) {
                            chlorophyllVar.insertData(msg.getFloat("value"), idx);
                            entityForTemp = entityForTemp >= 0 ? entityForTemp : msg.getSrcEnt();
                        }
                        else {
                            chlorophyllVar.insertData(Float.NaN, idx);
                        }
                    }
                    if (containsTurbidity) {
                        IMCMessage msg = getNextMessage(source, logVehicleId, curIndex, time, dataSamplePeriod, "Turbidity");
                        if (msg != null) {
                            turbidityVar.insertData(msg.getFloat("value"), idx);
                            entityForTemp = entityForTemp >= 0 ? entityForTemp : msg.getSrcEnt();
                        }
                        else {
                            turbidityVar.insertData(Float.NaN, idx);
                        }
                    }
                    if (containsPH) {
                        IMCMessage msg = getNextMessage(source, logVehicleId, curIndex, time, dataSamplePeriod, "PH");
                        if (msg != null) {
                            phVar.insertData(msg.getFloat("value"), idx);
                            entityForTemp = entityForTemp >= 0 ? entityForTemp : msg.getSrcEnt();
                        }
                        else {
                            phVar.insertData(Float.NaN, idx);
                        }
                    }
                    if (containsRedox) {
                        IMCMessage msg = getNextMessage(source, logVehicleId, curIndex, time, dataSamplePeriod, "redox");
                        if (msg != null) {
                            redoxVar.insertData(msg.getFloat("value"), idx);
                            entityForTemp = entityForTemp >= 0 ? entityForTemp : msg.getSrcEnt();
                        }
                        else {
                            redoxVar.insertData(Float.NaN, idx);
                        }
                    }
                    if (containsSoundSpeed) {
                        IMCMessage msg = getNextMessage(source, logVehicleId, curIndex, time, dataSamplePeriod, "SoundSpeed");
                        if (msg != null) {
                            soundSpeedVar.insertData(msg.getFloat("value"), idx);
                            entityForTemp = entityForTemp >= 0 ? entityForTemp : msg.getSrcEnt();
                        }
                        else {
                            soundSpeedVar.insertData(Float.NaN, idx);
                        }
                    }
                    if (containsTemperature) {
                        if (entityForTemp >= 0) {
                            IMCMessage msg = getNextMessage(source, logVehicleId, entityForTemp, curIndex, time, dataSamplePeriod, "Temperature");
                            if (msg != null) {
                                tempVar.insertData(msg.getFloat("value"), idx);
                                entityForTemp = entityForTemp >= 0 ? entityForTemp : msg.getSrcEnt();
                            }
                            else {
                                tempVar.insertData(Float.NaN, idx);
                            }
                        }
                        else {
                            tempVar.insertData(Float.NaN, idx);
                        }
                    }
                    if (containsDissolvedOrganicMatter) {
                        IMCMessage msg = getNextMessage(source, logVehicleId, curIndex, time, dataSamplePeriod, "DissolvedOrganicMatter");
                        if (msg != null) {
                            cdomVar.insertData(msg.getFloat("value"), idx);
                            entityForTemp = entityForTemp >= 0 ? entityForTemp : msg.getSrcEnt();
                        }
                        else {
                            cdomVar.insertData(Float.NaN, idx);
                        }
                    }
                    if (containsDissolvedOxygen) {
                        IMCMessage msg = getNextMessage(source, logVehicleId, curIndex, time, dataSamplePeriod, "DissolvedOxygen");
                        if (msg != null) {
                            cdomVar.insertData(msg.getFloat("value"), idx);
                            entityForTemp = entityForTemp >= 0 ? entityForTemp : msg.getSrcEnt();
                        }
                        else {
                            cdomVar.insertData(Float.NaN, idx);
                        }
                    }
                    if (containsAirSaturation) {
                        IMCMessage msg = getNextMessage(source, logVehicleId, curIndex, time, dataSamplePeriod, "AirSaturation");
                        if (msg != null) {
                            cdomVar.insertData(msg.getFloat("value"), idx);
                            entityForTemp = entityForTemp >= 0 ? entityForTemp : msg.getSrcEnt();
                        }
                        else {
                            cdomVar.insertData(Float.NaN, idx);
                        }
                    }
                    if (containsOpticalBackscatter) {
                        IMCMessage msg = getNextMessage(source, logVehicleId, curIndex, time, dataSamplePeriod, "OpticalBackscatter");
                        if (msg != null) {
                            cdomVar.insertData(msg.getFloat("value"), idx);
                            entityForTemp = entityForTemp >= 0 ? entityForTemp : msg.getSrcEnt();
                        }
                        else {
                            cdomVar.insertData(Float.NaN, idx);
                        }
                    }
                }
                else {
                    timeVar.insertData((time * 1E3 - startDate.getTime()) / 1E3, idx);
                    latVar.insertData(-9999, idx);
                    lonVar.insertData(-9999, idx);
                    depthVar.insertData(-9999, idx);

                    cogVar.insertData(Float.NaN, idx);
                    hdgVar.insertData(Float.NaN, idx);
                    rollVar.insertData(Float.NaN, idx);
                    pitchVar.insertData(Float.NaN, idx);

                    sogVar.insertData(Float.NaN, idx);

                    if (containsCondutivity)
                        condVar.insertData(Float.NaN, idx);
                    if (containsTemperature)
                        tempVar.insertData(Float.NaN, idx);
                    if (containsSalinity)
                        salVar.insertData(Float.NaN, idx);
                    if (containsWaterDensity)
                        waterDensityVar.insertData(Float.NaN, idx);
                    if (containsChlorophyll)
                        chlorophyllVar.insertData(Float.NaN, idx);
                    if (containsTurbidity)
                        turbidityVar.insertData(Float.NaN, idx);
                    if (containsPH)
                        phVar.insertData(Float.NaN, idx);
                    if (containsRedox)
                        redoxVar.insertData(Float.NaN, idx);
                    if (containsSoundSpeed)
                        soundSpeedVar.insertData(Float.NaN, idx);
                    if (containsDissolvedOrganicMatter)
                        cdomVar.insertData(Float.NaN, idx);
                    if (containsDissolvedOxygen)
                        dissolvedOxygenVar.insertData(Float.NaN, idx);
                    if (containsAirSaturation)
                        airSaturationVar.insertData(Float.NaN, idx);
                    if (containsOpticalBackscatter)
                        opticalBackscatterVar.insertData(Float.NaN, idx);
                }
            }
             
            // Now writing data
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

    /**
     * @param source
     * @param logVehicleId
     * @param curIndex
     * @param time
     * @param dataSamplePeriod
     * @param messageType
     * @return
     */
    private IMCMessage getNextMessage(IMraLogGroup source, int logVehicleId, int curIndex, double time, int dataSamplePeriod,
            String messageType) {
        return getNextMessage(source, logVehicleId, -1, curIndex, time, dataSamplePeriod, messageType);
    }

    /**
     * @param source
     * @param logVehicleId
     * @param entity
     * @param curIndex
     * @param time
     * @param dataSamplePeriod
     * @param messageType
     * @return
     */
    private IMCMessage getNextMessage(IMraLogGroup source, int logVehicleId, int entity, int curIndex, double time,
            int dataSamplePeriod, String messageType) {
        Iterable<IMCMessage> iterator = source.getLsfIndex().getIterator(messageType, curIndex, (long) (time * 1E3));
        if (iterator != null) {
            Iterator<IMCMessage> it = iterator.iterator();
            while (it.hasNext()) {
                IMCMessage m = it.next();
                if (m.getSrc() != logVehicleId)
                    continue;
                if (entity >= 0 && m.getSrcEnt() != entity)
                    continue;
                if (m.getTimestamp() > time + dataSamplePeriod)
                    return null;
                else
                    return m;
            }
        }
        return null;
    }
}
