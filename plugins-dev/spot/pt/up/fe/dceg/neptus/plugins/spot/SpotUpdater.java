package pt.up.fe.dceg.neptus.plugins.spot;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class SpotUpdater {


    public static Vector<SpotMessage> get(String url) throws ParserConfigurationException, SAXException, IOException {

		Vector<SpotMessage> updates = new Vector<SpotMessage>();

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(url);
        System.out.println(doc.toString());
		NodeList nlist = doc.getFirstChild().getChildNodes();
		
		for (int i = 1; i < nlist.getLength(); i++) {
			String tagName = nlist.item(i).getNodeName();
			if (tagName.equals("message")) {
				double lat = 0,lon = 0;
				String id = "SPOT";
				long timestamp = System.currentTimeMillis();

				NodeList elems = nlist.item(i).getChildNodes();
				for (int j = 0; j < elems.getLength(); j++) {
					String tag = elems.item(j).getNodeName();

					if (tag.equals("latitude"))
						lat = Double.parseDouble(elems.item(j).getTextContent());
					else if (tag.equals("longitude"))
						lon = Double.parseDouble(elems.item(j).getTextContent());
					else if (tag.equals("esnName"))
						id = elems.item(j).getTextContent();
					else if (tag.equals("timeInGMTSecond")) {
						timestamp = Long.parseLong(elems.item(j).getTextContent());
						timestamp *= 1000; //secs to millis
					}
				}
				updates.add(new SpotMessage(lat, lon, timestamp, id));
			}			
		}
		return updates;
	}

    public static String post(SpotMessage update, String postUrl) throws IOException {

		String xml = "<MissionState>\n"+update.asVehicleState()+"</MissionState>\n";

		String data = URLEncoder.encode("type", "UTF-8") + "=" + URLEncoder.encode("state", "UTF-8");
		data += "&" + URLEncoder.encode("xml", "UTF-8") + "=" + URLEncoder.encode(xml, "UTF-8");

		System.out.println("Posting:\n"+xml);
		
		// Send data
		URL url = new URL(postUrl);
		URLConnection conn = url.openConnection();
		conn.setDoOutput(true);
		OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
		wr.write(data);
		wr.flush();

		// Get the response
		BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		String line;
		String resp = "Response: \n";
		while ((line = rd.readLine()) != null) {
			resp += line;
		}
		//Logger.getLogger(SpotUpdater.class.getName()).info(resp);

		wr.close();
		rd.close();
		return resp;
	}

    public static SpotMessage update(String getUrl, String postUrl) throws ParserConfigurationException, SAXException,
            IOException {
		Vector<SpotMessage> updates = get(getUrl);
		if (!updates.isEmpty()) {
			Collections.sort(updates);
			SpotMessage mostRecent = updates.lastElement();
			post(mostRecent, postUrl);
			return mostRecent;
		}
		return null;
	}

	public static void main(String[] args) throws Exception {
		
		String getUrl = "http://tiny.cc/spot1";
		String postUrl = "http://whale.fe.up.pt/neptleaves/state";
		
		while(true) {
			update(getUrl, postUrl);
			System.out.println("Sleeping for 3 minutes...");
			Thread.sleep(1000*60*3);
		}
	}
}
