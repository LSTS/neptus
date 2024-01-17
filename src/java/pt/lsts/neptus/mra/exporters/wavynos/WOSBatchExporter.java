package pt.lsts.neptus.mra.exporters.wavynos;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Base64;
import java.util.LinkedHashMap;

import javax.swing.ProgressMonitor;

import pt.lsts.imc.Distance;
import pt.lsts.imc.FuelLevel;
import pt.lsts.imc.GpsFix;
import pt.lsts.imc.RSSI;
import pt.lsts.imc.SonarData;
import pt.lsts.imc.Voltage;
import pt.lsts.imc.lsf.LsfIndex;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.exporters.BatchMraExporter;
import pt.lsts.neptus.mra.exporters.MRAExporter;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.types.coord.CoordinateUtil;
import pt.lsts.neptus.types.coord.LatLonFormatEnum;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.util.DateTimeUtil;
import pt.lsts.neptus.util.llf.LogUtils;

public class WOSBatchExporter implements MRAExporter {

    public WOSBatchExporter(IMraLogGroup source) {  
        // TODO Auto-generated constructor stub
    }

    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        return source.getLsfIndex().containsMessagesOfType("GpsFix");
    }

    public String getWavyId(String name) {
        String parts[] = name.split("-");
        if (parts.length == 3)
            name = parts[0].charAt(0) + "" + parts[1].charAt(0) + "" + parts[2].charAt(0);
        else if (parts.length == 2)
            name = "wn" + parts[1].charAt(0);
        else
            name = name.substring(0, 3);
        return name.toUpperCase();
    }

    public StringBuilder getGpsLine(String name, GpsFix fix) {
        StringBuilder sb = new StringBuilder("telemetry=");
        sb.append(name).append("|"); // wavy id
        sb.append("v1|"); // version
        sb.append(DateTimeUtil.timeFormatterNoMillis.format(fix.getDate())).append("|"); // timestamp
        sb.append(CoordinateUtil.latitudeAsPrettyString(fix.getLat(), LatLonFormatEnum.DECIMAL_DEGREES)).append("|"); // latitude
        sb.append(CoordinateUtil.longitudeAsPrettyString(fix.getLon(), LatLonFormatEnum.DECIMAL_DEGREES)).append("|"); // longitude
        sb.append(fix.getSatellites()).append("|"); // satellites
        sb.append(fix.getHdop()).append("|"); // hdop
        sb.append(fix.getHeight()).append("|"); // height
        sb.append(fix.getSog()).append("|"); // speed
        return sb;
    }
    
    public String getStatsLine(FuelLevel fuel, Voltage voltage, RSSI rssi) {
        StringBuilder sb = new StringBuilder();
        if (voltage != null)
            sb.append(voltage.getValue()).append("|"); // voltage
        else
            sb.append("NAN|"); // voltage

        if (fuel != null)
            sb.append(fuel.getValue()).append("|"); // fuel
        else
            sb.append("NAN|"); // fuel

        if (rssi != null)
            sb.append(rssi.getValue()).append(""); // rssi
        else
            sb.append("NAN"); // rssi       
    
        return sb.toString();
    }

    public String getSonarLine(String name, GpsFix fix, FuelLevel fuel, Voltage voltage, RSSI rssi, SonarData[] sonarData, Distance slantRange) {
        StringBuilder sb = getGpsLine(name, fix);

        if (sonarData.length > 0) {
            sb.append("GNSS,ECHOSOUNDER|");
            for (int i = 0; i < 2; i++) {
                SonarData sd = sonarData[i];
                Distance sr = slantRange;
                if (sd == null || sr == null)
                    continue;                
                sb.append(sd.getMaxRange()).append("|"); // max range
                sb.append(sd.getFrequency()).append("|"); // frequency
                sb.append(sr.getValue()).append("|"); // slant range
            }            
        }
        else 
            sb.append("GNSS|NAN|NAN|NAN|"); // payload    
        
        sb.append(getStatsLine(fuel, voltage, rssi));
        sb.append("\n");
        
        for (int i = 0; i < 2; i++) {
            
            if (sonarData[i] == null)
                continue;
            sb.append("freq"+(i+1)+"=");
            sb.append(Base64.getEncoder().encodeToString(sonarData[i].getData()));
            sb.append("\n");            
        }
        return sb.toString();
    }

    public String getTelemetryLine(String name, GpsFix fix, FuelLevel fuel, Voltage voltage, RSSI rssi) {
        StringBuilder sb = getGpsLine(name, fix);
        sb.append("GNSS|NAN|NAN|NAN|")
            .append(getStatsLine(fuel, voltage, rssi))
            .append("\n");
        return sb.toString();
    }

    @Override
    public String process(IMraLogGroup source, ProgressMonitor pmonitor) {
        String name = "WN1";
        VehicleType vehicle = LogUtils.getVehicle(source);
        if (vehicle != null)
            name = getWavyId(vehicle.getId());
        File out = new File(source.getFile("mra"), "data.wos");
        BufferedWriter writer;
        try {
            writer = new BufferedWriter(new FileWriter(out));          
        }
        catch (Exception e) {
            NeptusLog.pub().error(e);
            return I18n.textf("Error creating output file: %error", e.getMessage());            
        }

        File dir = new File(source.getFile("mra"), "csv");
        dir.mkdirs();

        LsfIndex index = source.getLsfIndex();
        GpsFix lastFix = null;
        FuelLevel lastFuel = null;
        Voltage lastVoltage = null;
        RSSI lastRSSI = null;
        SonarData[] lastSonar = {null, null};
        Distance lastSlantRange = null;
        pmonitor.setMaximum(index.getNumberOfMessages());
        LinkedHashMap<Integer, Integer> sonarIndexes = new LinkedHashMap<>();
          
        for (int i = 0; i < index.getNumberOfMessages(); i++) {
            if (pmonitor.isCanceled()) {
                try {
                    writer.close();
                }
                catch (Exception e) {
                    NeptusLog.pub().error(e);
                }
                return I18n.text("Operation canceled by user");
            }
            int type = index.typeOf(i);
            pmonitor.setProgress(i);
            pmonitor.setNote("Processing "+i+"/"+index.getNumberOfMessages());
            switch (type) {
                case GpsFix.ID_STATIC:
                    lastFix = (GpsFix) index.getMessage(i);
                    if (lastSonar[0] != null && lastSonar[1] != null) {
                        String str = getSonarLine(name, lastFix, lastFuel, lastVoltage, lastRSSI, lastSonar, lastSlantRange);
                        System.out.println(str);
                        try {
                            writer.write(str);
                        }
                        catch (Exception e) {
                            NeptusLog.pub().error(e);
                        }
                        lastSonar[0] = null;
                        lastSonar[1] = null;    
                    }
                    else {
                        String str = getTelemetryLine(name, lastFix, lastFuel, lastVoltage, lastRSSI);
                        System.out.println(str);
                        try {
                            writer.write(str);
                        }
                        catch (Exception e) {
                            NeptusLog.pub().error(e);
                        }
                    }
                        
                    lastFix = null;
                    break;
                case SonarData.ID_STATIC:
                    SonarData sd = (SonarData)index.getMessage(i);
                    if (!sonarIndexes.containsKey((int)sd.getSrcEnt()))
                        sonarIndexes.put((int)sd.getSrcEnt(), sonarIndexes.size());    
                    lastSonar[sonarIndexes.get((int)sd.getSrcEnt())] = sd;
                    if (lastFix != null && (lastSonar[0] != null || lastSonar[1] != null)) {
                        String str = getSonarLine(name, lastFix, lastFuel, lastVoltage, lastRSSI, lastSonar, lastSlantRange);
                        System.out.println(str);
                        try {
                            writer.write(str);
                        }
                        catch (Exception e) {
                            NeptusLog.pub().error(e);
                        }
                        lastSonar[0] = null;
                        lastSonar[1] = null;
                    }
                    break;
                case Distance.ID_STATIC:
                    Distance sr = (Distance)index.getMessage(i);
                    lastSlantRange = sr;
                    break;
                case FuelLevel.ID_STATIC:
                    lastFuel = (FuelLevel) index.getMessage(i);
                    break;
                case Voltage.ID_STATIC:
                    lastVoltage = (Voltage) index.getMessage(i);
                    break;
                case RSSI.ID_STATIC:
                    lastRSSI = (RSSI) index.getMessage(i);
                    break;
                default:
                    break;
            }
        }         
        return "done";       
    }


    public static void main(String[] args) {
        BatchMraExporter.apply(WOSBatchExporter.class);
    }
    
    
    
}
