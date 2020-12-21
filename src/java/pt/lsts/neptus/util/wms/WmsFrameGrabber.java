package pt.lsts.neptus.util.wms;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.imageio.ImageIO;

import pt.lsts.neptus.util.VideoCreator;

public class WmsFrameGrabber {

	private static LinkedHashMap<String, String> cmems_defaults = new LinkedHashMap<>();
	private static LinkedHashMap<String, String> defaults = new LinkedHashMap<>();

	static {
		defaults.put("FORMAT", "image/png");
		defaults.put("TRANSPARENT", "true");
		defaults.put("SERVICE", "WMS");
		defaults.put("VERSION", "1.1.1");
		defaults.put("SRS", "EPSG:4326");
		defaults.put("BBOX", "-160,19,-100,42");
		defaults.put("WIDTH", "1920");
		defaults.put("HEIGHT", "1080");

		cmems_defaults.putAll(defaults);
		cmems_defaults.put("STYLES", "boxfill/rainbow");
		cmems_defaults.put("TIME", "2018-05-02T12:00:00.000Z");
		cmems_defaults.put("ELEVATION", "-0.49402499198913574");
		cmems_defaults.put("LOGSCALE", "false");

	}

	public static BufferedImage wmsFetch(String baseUrl, Map<String, String> optionsMap, String... options) {
		String extra = "";

		LinkedHashMap<String, String> ops = new LinkedHashMap<>();
		ops.putAll(defaults);
		ops.putAll(optionsMap);
		for (int i = 0; i < options.length - 1; i += 2) {
			ops.put(options[i], options[i + 1]);
		}

		for (Entry<String, String> e : ops.entrySet()) {
			try {
				extra += "&" + URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8.toString()) + "="
						+ URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8.toString());
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		baseUrl += extra;
		try {
			URL url = new URL(baseUrl);
			System.out.println(url);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			if (conn.getResponseCode() == 200)
				return ImageIO.read(url.openStream());
			else {
				System.err.println("Response: " + conn.getResponseCode());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	public static BufferedImage gmrt(double latMin, double lonMin, double latMax, double lonMax, int width,
			int height) {

		String gmrtUrl = "https://www.gmrt.org/services/mapserver/wms_merc?request=GetMap";
		return WmsFrameGrabber.wmsFetch(gmrtUrl, defaults, "LAYERS", "gmrt", "VERSION", "1.0", "BBOX",
				lonMin + "," + latMin + "," + lonMax + "," + latMax, "WIDTH", "" + width, "HEIGHT", "" + height);
	}

	public static BufferedImage gmrt() {
		String gmrtUrl = "https://www.gmrt.org/services/mapserver/wms_merc?request=GetMap";
		return WmsFrameGrabber.wmsFetch(gmrtUrl, defaults, "LAYERS", "gmrt", "VERSION", "1.0");
	}

	public static BufferedImage salinity(Instant instant) {
		String time = "" + instant.truncatedTo(ChronoUnit.DAYS).plus(12, ChronoUnit.HOURS);
		String baseUrl = "http://nrt.cmems-du.eu/thredds/wms/global-analysis-forecast-phy-001-024?REQUEST=GetMap";
		return WmsFrameGrabber.wmsFetch(baseUrl, cmems_defaults, "LAYERS", "so", "COLORSCALERANGE", "30.0,36.0", "TIME",
				"" + time);
	}

	public static BufferedImage temperature(Instant instant) {
		String time = "" + instant.truncatedTo(ChronoUnit.DAYS).plus(12, ChronoUnit.HOURS);
		String baseUrl = "http://nrt.cmems-du.eu/thredds/wms/global-analysis-forecast-phy-001-024?REQUEST=GetMap";
		return WmsFrameGrabber.wmsFetch(baseUrl, cmems_defaults, "LAYERS", "thetao", "COLORSCALERANGE", "0,30", "TIME",
				"" + time);
	}

	public static BufferedImage salinity(Instant time, double latMin, double lonMin, double latMax, double lonMax,
			int width, int height) {
		String baseUrl = "http://nrt.cmems-du.eu/thredds/wms/global-analysis-forecast-phy-001-024?REQUEST=GetMap";
		time.truncatedTo(ChronoUnit.DAYS).plus(Duration.ofHours(12));
		return WmsFrameGrabber.wmsFetch(baseUrl, cmems_defaults, "LAYERS", "so", "TIME", "" + time);
	}
	
	public static void main(String[] args) throws Exception {

		Instant now = LocalDate.now().atStartOfDay(ZoneId.of("UTC")).toInstant();
		BufferedImage gmrt = gmrt();
		VideoCreator creator = new VideoCreator(new File("salinity.mp4"), 1920, 1080);

		for (int i = -100; i <= 0; i++) {
			Instant time = now.plus(i, ChronoUnit.DAYS);
			BufferedImage base = new BufferedImage(gmrt.getWidth(), gmrt.getHeight(), BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = base.createGraphics();
			g.drawImage(gmrt, 0, 0, gmrt.getWidth(), gmrt.getHeight(), null);
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8f));
			g.drawImage(salinity(time), 0, 0, gmrt.getWidth(), gmrt.getHeight(), null);
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g.setFont(new Font("Arial", Font.BOLD, 15));
			g.setColor(Color.white);
			g.drawString("" + time, 10, 20);
			creator.addFrame(base, i * 100);
		}
		creator.closeStreams();
	}
}
