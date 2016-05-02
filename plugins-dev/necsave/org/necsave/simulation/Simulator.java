package org.necsave.simulation;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import net.miginfocom.swing.MigLayout;

public class Simulator extends JPanel {
	private static final long serialVersionUID = 1L;
	private Map<String, Data> list;
	private IniParser parser = null;
	private JPanel centerPanel;
	private JPanel hiddenPanel;
	private JDialog extraItemsDialog;

	//GUI Buttons
	private JButton btnResetConfig;
	private JButton btnSaveConfig;

	public Simulator(String config, String xmlFile) {
		super(new GridLayout(1, 1));

		XMLParser xmlParser = new XMLParser();

		list = xmlParser.parseXML(xmlFile);

		if (list != null)
			parser = new IniParser(config, this);

		if (parser != null) {
			setup();
			makePanel(getPlatformType());
		}

	}

	private void setup() {
		setLayout(new BorderLayout());
		centerPanel = new JPanel();
		hiddenPanel = new JPanel();
		extraItemsDialog = new JDialog(SwingUtilities.windowForComponent(this));
		extraItemsDialog.setVisible(false);
		extraItemsDialog.setResizable(false);

		JPanel buttonsPanel = new JPanel(new BorderLayout());
		JPanel leftSidePanel = new JPanel();

		leftSidePanel.setLayout(new MigLayout("", "[][]", "[]"));

		btnResetConfig = new JButton("Reset Config.");
		btnResetConfig.setFont(new Font("Dialog", Font.BOLD, 10));
		leftSidePanel.add(btnResetConfig, "cell 0 0");

		btnSaveConfig = new JButton("Save Config.");
		btnSaveConfig.setFont(new Font("Dialog", Font.BOLD, 10));
		leftSidePanel.add(btnSaveConfig, "cell 1 0");

		btnResetConfig.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (!parser.resetConfig()) 
					JOptionPane.showMessageDialog(null, "Error reseting config file.", "Error", JOptionPane.ERROR_MESSAGE);
				else 
					updateUI();
			}

		});

		btnSaveConfig.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				boolean res = parser.save();

				if (!res)
					JOptionPane.showMessageDialog(null, "Error saving config file.", "Error", JOptionPane.ERROR_MESSAGE);
			}
		});

		JPanel rightSidePanel = new JPanel();

		JButton showMoreBtn = new JButton("Additional Settings...");
		showMoreBtn.setFont(new Font("Dialog", Font.BOLD, 10));
		showMoreBtn.setHorizontalAlignment(SwingConstants.RIGHT);
		rightSidePanel.add(showMoreBtn);

		showMoreBtn.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				if (extraItemsDialog.isVisible()) {
					extraItemsDialog.setVisible(false);
					showMoreBtn.setText("Additional Settings...");
				}
				else {
					extraItemsDialog.setVisible(true);
					showMoreBtn.setText("Hide");
				}
			}
		});
		

        extraItemsDialog.addWindowListener(new WindowAdapter() 
        {
          public void windowClosed(WindowEvent e)
          {
            showMoreBtn.setText("Additional Settings...");
          }
         
          public void windowClosing(WindowEvent e)
          {
            showMoreBtn.setText("Additional Settings...");
          }
        });
        
        
		buttonsPanel.add(leftSidePanel, BorderLayout.WEST);
		buttonsPanel.add(rightSidePanel, BorderLayout.EAST);

		add(buttonsPanel, BorderLayout.SOUTH);

		add(centerPanel, BorderLayout.CENTER);
	}

	private void makePanel(String platformType) {
		int countCols = 0;
		int countRows = 0;
		int aux = 0;

		centerPanel.setLayout(new MigLayout("", "", ""));
		ArrayList<JPanel> jpanelList = new ArrayList<>();
		for (String sectionName : getVisibleSectionList()) {

			//System.out.println("Section: "+sectionName);
			int lineFieldsSize = getVisibleSectionList().size();
			StringBuilder lineFields = new StringBuilder();
			for (int i = 0 ; i < lineFieldsSize ; i++) {
				lineFields.append("[]");
			}
			JPanel panel = new JPanel();
			panel.setBorder(new TitledBorder(new LineBorder(new Color(184, 207, 229)), sectionName, 
					TitledBorder.LEADING, TitledBorder.TOP, null, Color.GRAY));
			panel.setLayout(new MigLayout("", "[][grow]", lineFields.toString()));

			int countFields = 0;
			for (String field : getSectionFieldList(sectionName)) {
				JLabel lblNewLabel = (JLabel) list.get(sectionName+"."+field).label;
				//System.out.println(lblNewLabel.getText());
				String constraintFields = "cell 0 "+countFields;
				constraintFields += ",alignx trailing";

				panel.add(lblNewLabel, constraintFields);

				//check if is JTextField / JTextArea or JCheckBox
				if (list.get(sectionName+"."+field).field instanceof JTextField) {
					JTextField fieldValueTxt = (JTextField) list.get(sectionName+"."+field).field;
					fieldValueTxt.setColumns(10);
					panel.add(fieldValueTxt, "cell 1 "+countFields);
				} else if (list.get(sectionName+"."+field).field instanceof JTextArea) {
					JTextArea fieldValueTxt = (JTextArea) list.get(sectionName+"."+field).field;
					Border border = BorderFactory.createLineBorder(Color.GRAY);
					fieldValueTxt.setRows(3);
					fieldValueTxt.setColumns(15);
					fieldValueTxt.setLineWrap(true);
					fieldValueTxt.setWrapStyleWord(true);

					JScrollPane scrollPanelSim = new JScrollPane(fieldValueTxt);
					fieldValueTxt.setBorder(BorderFactory.createCompoundBorder(border, 
							BorderFactory.createEmptyBorder(2, 2, 2, 2)));

					panel.add(scrollPanelSim, "cell 1 "+countFields+",growy");
				} else if (list.get(sectionName+"."+field).field instanceof JCheckBox) {
					JCheckBox fieldValueTxt = (JCheckBox) list.get(sectionName+"."+field).field;
					panel.add(fieldValueTxt, "cell 1 "+countFields);
				}

				countFields++;

			}

			jpanelList.add(panel);
			String constraint = "cell "+countCols+" "+countRows;
			centerPanel.add(jpanelList.get(aux), constraint+", grow");
			countCols++;
			aux++;
			if (aux % 3 == 0) {
				countRows++;
				countCols = 0;
			}
		}
		countCols = 0;
		countRows = 0;
		aux = 0;
		extraItemsDialog.add(hiddenPanel);
		
		hiddenPanel.setLayout(new MigLayout("", "", ""));
		ArrayList<JPanel> jpanelHiddenList = new ArrayList<>();
		for (String sectionName : getHiddenSectionList()) {

			//System.out.println("Section: "+sectionName);
			int lineFieldsSize = getHiddenSectionList().size();
			StringBuilder lineFields = new StringBuilder();
			for (int i = 0 ; i < lineFieldsSize ; i++) {
				lineFields.append("[]");
			}
			JPanel panel = new JPanel();
			panel.setBorder(new TitledBorder(new LineBorder(new Color(184, 207, 229)), sectionName, 
					TitledBorder.LEADING, TitledBorder.TOP, null, Color.GRAY));
			panel.setLayout(new MigLayout("", "[][grow]", lineFields.toString()));

			int countFields = 0;
			for (String field : getSectionFieldList(sectionName)) {
				JLabel lblNewLabel = (JLabel) list.get(sectionName+"."+field).label;
				//System.out.println(lblNewLabel.getText());
				String constraintFields = "cell 0 "+countFields;
				constraintFields += ",alignx trailing";

				panel.add(lblNewLabel, constraintFields);

				//check if is JTextField / JTextArea or JCheckBox
				if (list.get(sectionName+"."+field).field instanceof JTextField) {
					JTextField fieldValueTxt = (JTextField) list.get(sectionName+"."+field).field;
					fieldValueTxt.setColumns(10);
					panel.add(fieldValueTxt, "cell 1 "+countFields);
				} else if (list.get(sectionName+"."+field).field instanceof JTextArea) {
					JTextArea fieldValueTxt = (JTextArea) list.get(sectionName+"."+field).field;
					Border border = BorderFactory.createLineBorder(Color.GRAY);
					fieldValueTxt.setRows(3);
					fieldValueTxt.setColumns(15);
					fieldValueTxt.setLineWrap(true);
					fieldValueTxt.setWrapStyleWord(true);

					JScrollPane scrollPanelSim = new JScrollPane(fieldValueTxt);
					fieldValueTxt.setBorder(BorderFactory.createCompoundBorder(border, 
							BorderFactory.createEmptyBorder(2, 2, 2, 2)));

					panel.add(scrollPanelSim, "cell 1 "+countFields+",growy");
				} else if (list.get(sectionName+"."+field).field instanceof JCheckBox) {
					JCheckBox fieldValueTxt = (JCheckBox) list.get(sectionName+"."+field).field;
					panel.add(fieldValueTxt, "cell 1 "+countFields);
				}

				countFields++;

			}

			jpanelHiddenList.add(panel);
			String constraint = "cell "+countCols+" "+countRows;
			//System.out.println("Constraint : "+constraint);
			hiddenPanel.add(jpanelHiddenList.get(aux), constraint+", grow");
			countCols++;
			aux++;
			if (aux % 3 == 0) {
				countRows++;
				countCols = 0;
			}
		}
		extraItemsDialog.pack();
	}

	private ArrayList<String> getVisibleSectionList() {
		return getSectionList(true);
	}

	private ArrayList<String> getHiddenSectionList() {
		return getSectionList(false);
	}

	private ArrayList<String> getSectionList(boolean visible) {
		ArrayList<String> section = new ArrayList<>();

		for (Entry<String, Data> e : list.entrySet()) {
			//System.out.println("DEBUG: "+sec);
			String sectionName = e.getKey().substring(0, e.getKey().indexOf("."));
			if (!section.contains(sectionName) && (e.getValue().isShowOnMain() == visible)) {
				section.add(sectionName);
			}
		}
		//Collections.sort(section);
		return section;
	}

	public ArrayList<String> getSectionList() {
		ArrayList<String> section = new ArrayList<>();

		for (Entry<String, Data> e : list.entrySet()) {
			String sectionName = e.getKey().substring(0, e.getKey().indexOf("."));
			if (!section.contains(sectionName)) {
				section.add(sectionName);
			}
		}
		return section;
	}

	public ArrayList<String> getSectionFieldList(String section) {
		ArrayList<String> sectionFields = new ArrayList<>();

		for (String sec : list.keySet()) {
			String sectionName = sec.substring(0, sec.indexOf("."));
			String fieldName = sec.substring(sec.indexOf(".")+1, sec.length());
			if (sectionName.equals(section)) {
				if (!sectionFields.contains(fieldName)) {
					sectionFields.add(fieldName);
				}
			}
		}
		//Collections.sort(sectionFields);
		return sectionFields;
	}

	public String getFieldValue(String section, String field) {

		for (String fields : getSectionFieldList(section)) {
			if (fields.equalsIgnoreCase(field)) {
				Data storedValue = list.get(section.concat("."+field));

				if (storedValue.field instanceof JTextField) { 
					JTextField text = (JTextField) list.get(section.concat("."+field)).field;
					return text.getText();
				} else if (storedValue.field instanceof JTextArea) {
					JTextArea text = (JTextArea) list.get(section.concat("."+field)).field;
					return text.getText();
				}
				else if (storedValue.field instanceof JCheckBox) {
					JCheckBox text = (JCheckBox) list.get(section.concat("."+field)).field;

					if (text.isSelected())
						return "true";
					else
						return "false";

				}
			}
		}

		return null;
	}

	public void updateFieldWithValue(String section, String field, String value) {
		String key = section.concat("."+field);
		Data storedValue = list.get(key);

		if (storedValue.field instanceof JTextField) {
			JTextField text = (JTextField) storedValue.field;
			text.setText(value);
			storedValue.setField(text);
			list.put(key, storedValue);
		} else if (storedValue.field instanceof JTextArea) {
			JTextArea text = (JTextArea) storedValue.field;
			text.setText(value);
			storedValue.setField(text);
			list.put(key, storedValue);
		} else if (storedValue.field instanceof JCheckBox) {
			JCheckBox text = (JCheckBox) storedValue.field;

			if (value.equalsIgnoreCase("true"))
				text.setSelected(true);
			else if (value.equalsIgnoreCase("false"))
				text.setSelected(false);

			storedValue.setField(text);
			list.put(key, storedValue);
		}
	}


	public boolean isValidSimulator() {
		if (parser != null)
			return parser.validate();

		return false;
	}

	public String getConfigPath(){
		return parser.getPlatform().getConfigPath();
	}

	public String getName(){
		File file = new File(getConfigPath());

		String name = file.getName();
		int pos = name.lastIndexOf(".");
		if (pos > 0) {
			name = name.substring(0, pos);
		}

		return name;
	}

	public String getPlatformType(){
		return parser.getPlatform().getPlatformType();
	}

	public Map<String, Data> getList() {
		return list;
	}

	public void setList(Map<String, Data> newList) {
		list = newList;
	}

	public void updateSectionName(String oldName, String newName) {
		Map<String, Data> updatedList = Collections.synchronizedMap(new LinkedHashMap<>());

		for (Entry<String, Data> e : list.entrySet()) {
			String sec = e.getKey().substring(0, e.getKey().indexOf("."));
			String field = e.getKey().substring(e.getKey().indexOf(".")+1, e.getKey().length());

			if (e.getValue().getPlatform().equals("All") || newName.equalsIgnoreCase(e.getValue().getPlatform())) {
				if (sec.startsWith(oldName))
					updatedList.put(sec.replace(oldName, newName).concat("."+field), e.getValue());
				else
					updatedList.put(e.getKey(), e.getValue());
			}
		}

		list = updatedList;
	}

	private class Data {
		private JLabel label;
		private JComponent field;
		private boolean showOnMain;
		private String platformType;

		public Data(String label, JComponent value, boolean show, String platformType) {
			this.label = new JLabel(label);
			this.field = value;
			this.showOnMain = show;
			this.platformType = platformType;
		}

		public JLabel getLabel() {
			return label;
		}

		public void setLabel(JLabel label) {
			this.label = label;
		}

		public JComponent getField() {
			return field;
		}

		public void setField(JComponent value) {
			this.field = value;
		}

		public boolean isShowOnMain() {
			return showOnMain;
		}

		public String getPlatform() {
			return platformType;
		}

	}
	private class XMLParser {

		private Map<String, Data> parseXML(String arg) {
			Map<String, Data> mainList = Collections.synchronizedMap(new LinkedHashMap<>());

			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder;
			Document document = null;

			try {
				builder = factory.newDocumentBuilder();

				// Load the input XML document, parse it and return an instance of the
				// Document class.
				document = builder.parse(new File(arg));

			} catch (Exception e) {
				System.err.println("ERROR: "+e.getMessage() + "("+this.getClass().getCanonicalName()+")");
				return null;
			}

			try {
				NodeList nList = document.getElementsByTagName("Section");
				for (int temp = 0; temp < nList.getLength(); temp++) {

					Node nNode = nList.item(temp);
					String ID = nNode.getAttributes().getNamedItem("name").getNodeValue();

					String platDep = nNode.getAttributes().getNamedItem("platform").getNodeValue();
					String show = nNode.getAttributes().getNamedItem("show").getNodeValue();

					boolean showOnMainWindow = show.equalsIgnoreCase("true") ? true : false ;

					//System.out.println("\nCurrent Element: "+ ID);

					Element eElement = (Element) nNode;

					NodeList childList = eElement.getElementsByTagName("Field");
					for (int j=0; j < childList.getLength(); j++) {
						Node childNode = childList.item(j);
						String field = childNode.getAttributes().getNamedItem("name").getNodeValue();
						String type = childNode.getAttributes().getNamedItem("type").getNodeValue();

						//System.out.println("Field : " + field + " type: " + type);

						if (type.equals("textfield")) {
							mainList.put(ID.concat("."+field), new Data(field+":", new JTextField(), showOnMainWindow, platDep));

							//							System.out.println("["+ID+"] "+field + " PLATFORM:"+mainList.get(ID.concat("."+field)).getPlatform());
						} 
						else if (type.equals("textarea")) {
							mainList.put(ID.concat("."+field), new Data(field+":", new JTextArea(), showOnMainWindow, platDep));
							//							System.out.println("["+ID+"] "+field + " PLATFORM:"+mainList.get(ID.concat("."+field)).getPlatform());
						} 
						else if (type.equals("checkbox")) {
							mainList.put(ID.concat("."+field), new Data(field+":", new JCheckBox(), showOnMainWindow, platDep));
							//							System.out.println("["+ID+"] "+field + " PLATFORM:"+mainList.get(ID.concat("."+field)).getPlatform());
						}
					}

				}


				return mainList;

			} catch (Exception e) {
				System.err.println("ERROR: parsing XML file ("+this.getClass().getCanonicalName()+")");
				return null;
			}

		}
	}
}