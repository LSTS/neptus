package pt.up.fe.dceg.neptus.controllers;

import java.io.File;
import java.util.LinkedHashMap;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.JoyEnvironment;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import pt.up.fe.dceg.neptus.NeptusLog;

/**
 * ControllerManager class 
 * Since controllerListener doesnt and listening thread
 * isn't feasible the
 * 
 * @author jqcorreia
 * 
 */
public class ControllerManager {
	private static String MAP_FILE_XML = "conf/controllers/mapping.xml";
	private LinkedHashMap<String, Controller> controllerList = new LinkedHashMap<String, Controller>();
	private LinkedHashMap<String, LinkedHashMap<String, String>> controllerMappings = new LinkedHashMap<String, LinkedHashMap<String, String>>();
	
	public ControllerManager() {
		loadMappingsXML();
		fetchControllers();
	}

	public void loadMappingsXML() {
		try {
			if(!new File(MAP_FILE_XML).isFile()) {
				NeptusLog.pub().info("<###>Error loading controllers mapping file");
				return;
			}
			Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(MAP_FILE_XML);
			NodeList controllerNodeList = doc.getElementsByTagName("controller");
			
			for(int c = 0; c < controllerNodeList.getLength(); c++) {
				Element node = (Element)controllerNodeList.item(c);
				NodeList list = node.getElementsByTagName("entry");
				
				// If this device isn't mapped yet (no loaded mapping)
				if (controllerMappings.get(node.getAttribute("name")) == null) {
					controllerMappings.put(node.getAttribute("name"),
							new LinkedHashMap<String, String>());
				}
				
				for(int i = 0; i < list.getLength(); i++) {
					Element e = (Element) list.item(i);
					controllerMappings.get(node.getAttribute("name")).put(e.getAttribute("value"),e.getAttribute("key"));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
//		NeptusLog.pub().info("<###> "+controllerMappings);
	}

	public void saveMappingsXML() {
		Document doc;
		
		NeptusLog.pub().info("<###>Saving XML mapping file");
		try {
			// Create a new XML document
			doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
			
			Element controllers = doc.createElement("controllers");
			doc.appendChild(controllers);
			
			for(String k : controllerMappings.keySet()) {
				Element controller = doc.createElement("controller");
				controller.setAttribute("name", k);
				controllers.appendChild(controller);
				for(String l : controllerMappings.get(k).keySet()) {
					Element entry = doc.createElement("entry");
					entry.setAttribute("key", controllerMappings.get(k).get(l));
					entry.setAttribute("value", l);
					controller.appendChild(entry);
				}
			}
			
			// Setup and save it
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(new File(MAP_FILE_XML));
			
			transformer.transform(source, result);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void fetchControllers() {
		// Copy current controllers to oldMap
		LinkedHashMap<String, Controller> oldMap = new LinkedHashMap<String, Controller>();
		for (String s : controllerList.keySet()) {
			oldMap.put(s, controllerList.get(s));
		}

		controllerList.clear();

		// Fectch controllers list
		Controller controllers[] = new JoyEnvironment().getControllers();
		System.out.println("very slow controllers loading ");
		// Create new controllerMap
		for (Controller c : controllers) {
			if(!c.getName().toLowerCase().contains("keyboard") && !c.getName().toLowerCase().contains("mouse"))
			    if(c.getType() == Controller.Type.GAMEPAD || c.getType() == Controller.Type.STICK)
			        controllerList.put(c.getName(), c);
		}

		// Look for changes
		for (String k : oldMap.keySet()) {
			if (!controllerList.containsKey(k)) {
				NeptusLog.pub().info("Removed " + oldMap.get(k).getName());
			}
		}
		for (String k : controllerList.keySet()) {
			if (!oldMap.containsKey(k)) {
				NeptusLog.pub().info("Added " + controllerList.get(k).getName());
			}
		}
	}

	public LinkedHashMap<String, Component> pollController(Controller c) {
		LinkedHashMap<String, Component> pollResult = new LinkedHashMap<String, Component>();
		c.poll();

		for (Component comp : c.getComponents()) {
			pollResult.put(comp.getName(), comp);
		}
		return pollResult;
	}

	public LinkedHashMap<String, Component> pollController(String device) {
		Controller c = controllerList.get(device);
		return pollController(c);
	}

	public LinkedHashMap<String, Controller> getControllerList() {
		return controllerList;
	}

	public static void main(String[] args) {
//		final ControllerManager manager = new ControllerManager();
//		final String device = "Logitech Logitech Dual Action";
//		
//		JFrame frame = new JFrame();
//		JButton b = new JButton(new AbstractAction("refresh") {
//
//            private static final long serialVersionUID = -2069772609771099451L;
//
//            @Override
//			public void actionPerformed(ActionEvent e) {
//				manager.fetchControllers();
//			}
//		});
//
//		frame.setSize(200, 200);
//		frame.setLayout(new MigLayout());
//		frame.add(b, "wrap");
//		
//		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//		frame.setVisible(true);
//		
//		while (true) {
//			LinkedHashMap<String, Component> res = manager
//					.pollMappedController(device);
//			if (res != null) {
//				for (String k : res.keySet()) {
//					System.out.print(k + " " + res.get(k).getPollData() + " ");
//				}
//				NeptusLog.pub().info("<###> "+);
//			}
//			
//			try {
//				Thread.sleep(500);
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
	}
}
