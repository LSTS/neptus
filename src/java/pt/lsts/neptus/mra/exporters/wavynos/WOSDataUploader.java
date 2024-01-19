package pt.lsts.neptus.mra.exporters.wavynos;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

import javax.swing.ProgressMonitor;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import pt.lsts.imc.FuelLevel;
import pt.lsts.imc.GpsFix;
import pt.lsts.imc.RSSI;
import pt.lsts.imc.Voltage;
import pt.lsts.imc.lsf.LsfIndex;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.exporters.BatchMraExporter;
import pt.lsts.neptus.mra.exporters.MRAExporter;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.types.coord.CoordinateUtil;
import pt.lsts.neptus.types.coord.LatLonFormatEnum;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.util.DateTimeUtil;
import pt.lsts.neptus.util.llf.LogUtils;

public class WOSDataUploader implements MRAExporter {

    public WOSDataUploader(IMraLogGroup source) {

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
        StringBuilder sb = new StringBuilder();
        sb.append(name).append("|"); // wavy id
        sb.append("v1|"); // version
        sb.append(DateTimeUtil.dateTimeFormatterISO8601.format(fix.getDate())).append("|"); // timestamp
        double lat = Math.toDegrees(fix.getLat());
        double lon = Math.toDegrees(fix.getLon());
        if (lat < -90 || lat > 90)
            lat = Math.toRadians(lat);
        if (lon > 180 || lon < -180) 
            lon = Math.toRadians(lon);

        sb.append(CoordinateUtil.latitudeAsPrettyString(lat, LatLonFormatEnum.DECIMAL_DEGREES)).append("|"); // latitude
        sb.append(CoordinateUtil.longitudeAsPrettyString(lon, LatLonFormatEnum.DECIMAL_DEGREES)).append("|"); // longitude
        sb.append(fix.getSatellites()).append("|"); // satellites
        sb.append(String.format(Locale.US, "%.2f", fix.getHdop())).append("|"); // hdop
        sb.append(String.format(Locale.US,"%.2f", fix.getHeight())).append("|"); // height
        sb.append(String.format(Locale.US,"%.2f", fix.getSog())).append("|"); // speed
        return sb;
    }

    public String getStatsLine(FuelLevel fuel, Voltage voltage, RSSI rssi) {
        StringBuilder sb = new StringBuilder();
        if (voltage != null)
            sb.append(String.format(Locale.US,"%.2f", voltage.getValue())).append("|"); // voltage
        else
            sb.append("NAN|"); // voltage

        if (fuel != null)
            sb.append(String.format(Locale.US,"%.2f", fuel.getValue())).append("|"); // fuel
        else
            sb.append("NAN|"); // fuel

        if (rssi != null)
            sb.append(String.format(Locale.US,"%.2f", rssi.getValue())).append(""); // rssi
        else
            sb.append("NAN"); // rssi

        return sb.toString();
    }

    public String getTelemetryLine(String payload, String name, GpsFix fix, FuelLevel fuel, Voltage voltage,
            RSSI rssi) {
        StringBuilder sb = getGpsLine(name, fix);
        sb.append(payload).append("|"); // payload
        sb.append("NAN|NAN|NAN|NAN|")
                .append(getStatsLine(fuel, voltage, rssi))
                .append("|secret");
        return sb.toString();
    }

    @Override
    public String process(IMraLogGroup source, ProgressMonitor pmonitor) {
        String name = "WI1";
        LsfIndex index = source.getLsfIndex();
        GpsFix lastFix = null;
        FuelLevel lastFuel = null;
        Voltage lastVoltage = null;
        RSSI lastRSSI = null;
        CloseableHttpClient httpClient = HttpClients.createDefault();
        VehicleType vehicle = LogUtils.getVehicle(source);
        if (vehicle != null)
            name = getWavyId(vehicle.getId());

        File photosDir = source.getFile("Photos");
        File flacDir = source.getDir();

        if (photosDir != null && photosDir.isDirectory()) {

            for (File f : photosDir.listFiles()) {
                System.out.println("Processing " + f.getName());
                if (pmonitor.isCanceled()) {
                    return I18n.text("Operation canceled by user");
                }
                double timestamp = Double.parseDouble(f.getName().substring(0, f.getName().indexOf(".")));

                int index1 = index.advanceToTime(0, timestamp);
                try {
                    lastFix = index.getMessage(index.getNextMessageOfType(GpsFix.ID_STATIC, index1), GpsFix.class);
                    lastFuel = index.getMessage(index.getNextMessageOfType(FuelLevel.ID_STATIC, index1),
                            FuelLevel.class);
                    lastVoltage = index.getMessage(index.getNextMessageOfType(Voltage.ID_STATIC, index1),
                            Voltage.class);
                    lastRSSI = index.getMessage(index.getNextMessageOfType(RSSI.ID_STATIC, index1), RSSI.class);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                System.out.print(getTelemetryLine("GNSS,CAM", name, lastFix, lastFuel, lastVoltage, lastRSSI));
                System.out.print(getStatsLine(lastFuel, lastVoltage, lastRSSI));
                System.out.println("|" + f.getName());

                HttpPost post = new HttpPost("https://wavynos-rtds.inesctec.pt/api/observations/");
                // post.setHeader("Content-Type", "multipart/form-data");

                MultipartEntityBuilder multipartBuilder = MultipartEntityBuilder.create();
                String telem =  getTelemetryLine("GNSS,CAM", name, lastFix, lastFuel, lastVoltage, lastRSSI);
                System.out.println(telem);
                multipartBuilder.addTextBody("telemetry",
                        telem);
                multipartBuilder.addBinaryBody("payload", f, ContentType.IMAGE_JPEG, f.getName());
                
                post.setEntity(multipartBuilder.build());
                
                try {
                    System.out.println(multipartBuilder.build().getContentType());
                    //multipartBuilder.build().writeTo(System.out);                     
                    CloseableHttpResponse response = httpClient.execute(post);
                    System.out.println(response.getStatusLine().getStatusCode() + " : "
                            + response.getStatusLine().getReasonPhrase());
                    response.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        for (File f : flacDir.listFiles()) {
            if (pmonitor.isCanceled()) {
                return I18n.text("Operation canceled by user");
            }

            if (f.getName().endsWith(".flac")) {
                System.out.println("Processing " + f.getName());
                String dateTime = f.getName().substring(5, f.getName().length() - 5);
                System.out.println(dateTime);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                double timestamp = 0;

                try {
                    timestamp = sdf.parse(dateTime).toInstant().toEpochMilli() / 1000.0;
                }
                catch (Exception e) {
                    e.printStackTrace();
                    continue;
                }
                //double = sdf.parse(dateTime).toInstant().toEpochMilli() / 1000.0;
                //parse time like this:yyyymmddhhmmss
                
                
                int index1 = index.advanceToTime(0, timestamp);
                try {
                    lastFix = index.getMessage(index.getNextMessageOfType(GpsFix.ID_STATIC, index1), GpsFix.class);
                    lastFuel = index.getMessage(index.getNextMessageOfType(FuelLevel.ID_STATIC, index1),
                            FuelLevel.class);
                    lastVoltage = index.getMessage(index.getNextMessageOfType(Voltage.ID_STATIC, index1),
                            Voltage.class);
                    lastRSSI = index.getMessage(index.getNextMessageOfType(RSSI.ID_STATIC, index1), RSSI.class);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                System.out.print(getTelemetryLine("GNSS,HYDROPHONE", name, lastFix, lastFuel, lastVoltage, lastRSSI));
                System.out.print(getStatsLine(lastFuel, lastVoltage, lastRSSI));
                System.out.println("|" + f.getName());

                HttpPost post = new HttpPost("https://wavynos-rtds.inesctec.pt/api/observations/");
                // post.setHeader("Content-Type", "multipart/form-data");

                MultipartEntityBuilder multipartBuilder = MultipartEntityBuilder.create();
                multipartBuilder.addTextBody("telemetry",
                        getTelemetryLine("GNSS,HYDROPHONE", name, lastFix, lastFuel, lastVoltage, lastRSSI));
                multipartBuilder.addBinaryBody("payload", f, ContentType.create("audio/x-flac"), f.getName());

                post.setEntity(multipartBuilder.build());

                try {
                    System.out.println(post.toString());
                    CloseableHttpResponse response = httpClient.execute(post);
                    System.out.println(response.getStatusLine().getStatusCode() + " : "
                            + response.getStatusLine().getReasonPhrase());
                    response.close();
                    
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return "done";
    }

    public static void main(String[] args) {
        BatchMraExporter.apply(WOSDataUploader.class);
    }
}
