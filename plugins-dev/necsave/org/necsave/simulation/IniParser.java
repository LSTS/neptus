package org.necsave.simulation;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import org.ini4j.Ini;
import org.ini4j.InvalidFileFormatException;

public class IniParser {	

	private Platform plat;
	private HashMap<SectionField, FileValue> readValues;
	private boolean isValidConfig;
    private static final boolean DEBUG = false;
    private ArrayList<File> backupFile = new ArrayList<>();
	private String configFile;
	private LinkedHashMap<SectionField, FileValue> newValues = new LinkedHashMap<>();
	private LinkedHashMap<String, Ini> iniTable = new LinkedHashMap<>();
	private Simulator simulator;

	public IniParser(String configPath, Simulator sim) {
		this.simulator = sim;

		loadConfiguration(configPath, true);
	}

	private void loadConfiguration(String configFilePath, boolean backup) {
		plat = new Platform(configFilePath);
		Ini mainIniFile;
		readValues = new HashMap<>();

		try {
			mainIniFile = new Ini();
			mainIniFile.getConfig().setEscape(false);
			mainIniFile.getConfig().setInclude(true);
			mainIniFile.setFile(new File(configFilePath));
			mainIniFile.load(new File(configFilePath));

            if (isDummyPlatform(mainIniFile))
                plat.setPlatformType("DummyPlatform");
            else if (isCalPlatform(mainIniFile))
                plat.setPlatformType("CalPlatform");
			else
                plat.setPlatformType("DunePlatform");

			updateList(plat.getPlatformType());
            checkRequirements(mainIniFile, iniTable);
			
			for (Entry<String, Ini> conf : iniTable.entrySet()) {
				Ini file = conf.getValue();
				
				if (DEBUG) {
					System.out.println("--------------DEBUG--------------");
                    System.out.println("From file: " + file.getFile().getAbsolutePath());
				}
                readValues(file, plat.getPlatformType());
			}

			if (DEBUG) {
				System.out.println("--------------DEBUG--------------");
                System.out.println("From file: " + mainIniFile.getFile().getAbsolutePath());
			}

			//add main ini file to the map
			configFile = mainIniFile.getFile().getName();
            iniTable.put(mainIniFile.getFile().getAbsolutePath(), mainIniFile);

            readValues(mainIniFile, plat.getPlatformType());

			if (backup)
                backupFiles();

			isValidConfig = true;

		} catch (IOException e) {
			isValidConfig = false;
			e.printStackTrace();
		}
	}


	public boolean save() {

		for (String section: simulator.getSectionList()) {
			for (String field : simulator.getSectionFieldList(section)) {

				FileValue stored = readValues.get(new SectionField(section, field));
				String value = simulator.getFieldValue(section, field);

				//System.out.println("Section: "+ section + " field "+ field + " value "+value);
				if (value == null) 
					return false;

				if (stored != null) {
					if (!stored.value.equalsIgnoreCase(value))  {
						newValues.put(new SectionField(section, field), new FileValue(stored.file, value));
						readValues.put(new SectionField(section, field), new FileValue(stored.file, value));
					}
				} else {
					if (!value.isEmpty()) {
                        newValues.put(new SectionField(section, field), new FileValue(plat.getConfigPath(), value));
                        readValues.put(new SectionField(section, field), new FileValue(plat.getConfigPath(), value));
					}
				}
			}
		}

		return writeChanges();
	}

	private boolean writeChanges() {
		boolean done = false;

		ArrayList<String> filesToUpdate = new ArrayList<>();

		if (newValues.isEmpty())
			return true;
		
		for (Entry<SectionField, FileValue> struct : newValues.entrySet()) {
			if (!filesToUpdate.contains(struct.getValue().file))
				filesToUpdate.add(struct.getValue().file);
		}
		for (String updateFile : filesToUpdate) {
		    if (DEBUG)
		        System.out.println("Updating: " + updateFile);
		    
			ArrayList<ArrayList<String>> list = new ArrayList<>();

			for (Entry<SectionField, FileValue> struct : newValues.entrySet()) {
				String file = struct.getValue().file;
				String section = struct.getKey().section;
				String field = struct.getKey().field;
				String value = struct.getValue().value;

				if (updateFile.equals(file)) {
					list.add(new ArrayList<>(Arrays.asList(section, field, value)));
                    if (DEBUG)
                        System.out.println("[" + section + "] " + field + " = " + value);
				}
			}

			Ini ini;
			ini = iniTable.get(updateFile);

			for (ArrayList<String> l : list) {
				String section = l.get(0);
				String field = l.get(1);
				String value = l.get(2);

				if (DEBUG)
				    System.out.println(ini.getFile().getAbsolutePath() +" ["+section+"] " + field + " = " + value);
				
				ini.put(section, field, value);

			}
			try {
			    //before saving, comment requires lines so they don't get messed up
			    BufferedReader read = new BufferedReader(new FileReader(iniTable.get(updateFile).getFile().getAbsolutePath()));
			    ArrayList<String> reqlines = new ArrayList<>();

			    String dataRow = read.readLine(); 
			    while (dataRow != null){
			        if (dataRow != null) {
			            if (dataRow.startsWith("[Require")) {
			                reqlines.add(dataRow);
			            }
			        }
			        dataRow = read.readLine(); 
			    }
			    read.close();

			    //now store the changes
			    ini.store();

			    read = new BufferedReader(new FileReader(iniTable.get(updateFile).getFile().getAbsolutePath()));
			    ArrayList<String> lines = new ArrayList<>();

			    dataRow = read.readLine(); 
			    while (dataRow != null){
			        lines.add(dataRow);
			        dataRow = read.readLine(); 
			    }
			    read.close();
			    
			    FileWriter writer = new FileWriter(iniTable.get(updateFile).getFile().getAbsolutePath());
			    
			    for (int i = 0; i < reqlines.size(); i++){
                    writer.append((String) reqlines.get(i));
                    writer.append(System.getProperty("line.separator"));
                }
			    
			    for (int i = 0; i < lines.size(); i++){
			        writer.append(System.getProperty("line.separator"));
			        writer.append((String) lines.get(i));
			    }
			    writer.flush();
			    writer.close();

				done = true;

			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}

		}

        loadConfiguration(iniTable.get(plat.getConfigPath()).getFile().getAbsolutePath(), false);
		newValues.clear();
		return done;
	}

	@SuppressWarnings("resource")
    private void backupFiles() {
        for (Entry<String, Ini> e : iniTable.entrySet()) {
            Ini ini = e.getValue();

            if (DEBUG)
                System.out.println("Backup ... " + ini.getFile().getAbsolutePath());
            // copy backup
            File backup = new File(ini.getFile().getPath().concat(".bak"));
            FileChannel source = null;
            FileChannel destination = null;

            try {
                if (backup.exists())
                    backup.delete();
                else
                    backup.createNewFile();

                source = new FileInputStream(ini.getFile()).getChannel();
                destination = new FileOutputStream(backup).getChannel();
                destination.transferFrom(source, 0, source.size());

                backupFile.add(backup);
            }
            catch (FileNotFoundException e1) {
                e1.printStackTrace();
            }
            catch (IOException e1) {
                e1.printStackTrace();
            }

            finally {
                try {
                    if (source != null)
                        source.close();

                    if (destination != null)
                        destination.close();

                }
                catch (IOException e1) {
                    e1.printStackTrace();
                }
            }

        }
	}

	public boolean resetConfig() {

        if (!backupFile.isEmpty()) {

            for (File f : backupFile) {
                if (DEBUG)
                    System.out.println("Restoring " + f.getAbsolutePath());
                Path sourcePath = Paths.get(f.getAbsolutePath());
                Path destinationPath = Paths.get(f.getAbsolutePath().replace(".bak", ""));

                try {
                    Path ret = Files.copy(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
                }
                catch (IOException e) {
                    return false;
                }
            }

            loadConfiguration(iniTable.get(plat.getConfigPath()).getFile().getAbsolutePath(), false);

			return true;
		}
		else 
			return false;

	}
	
	private void checkRequirements(Ini confFile, LinkedHashMap<String, Ini> toIncludeConfs) {

	    for (String section : confFile.keySet()) {
	        if (section.contains("Require ") && section.contains(".ini")) {
	            String[] line = section.split(" ");
	            StringBuilder toInclude = new StringBuilder();

	            for (int i=1; i < line.length; i++) {
	                toInclude.append(confFile.getFile().getParentFile().getAbsolutePath().concat("/"+line[i]));
	                Ini conf = null;
	                try {
	                    File toBeIncluded = new File(toInclude.toString());
	                    conf = new Ini(toBeIncluded);
                        checkRequirements(conf, toIncludeConfs);
                        toIncludeConfs.put(toBeIncluded.getAbsolutePath(), conf);

	                } catch (InvalidFileFormatException e) {
	                    e.printStackTrace();
	                } catch (IOException e) {
	                    e.printStackTrace();
	                }
	            }
	        }
	    }
	   
	}

	private void readValues(Ini file, String platformType) {
		for (String section : simulator.getSectionList()) {
			Ini.Section readSection = file.get(section);

            if (DEBUG)
                System.out.println("[" + section + "]");

			if (readSection != null) {
				for (String field : simulator.getSectionFieldList(section)) {
					String value = readSection.get(field);
					if (value != null) {
						simulator.updateFieldWithValue(section, field, value);
                        readValues.put(new SectionField(section, field),
                                new FileValue(file.getFile().getAbsolutePath(), value));
                        if (DEBUG)
                            System.out.println(field + " = " + value);
					}
				}
			}

            if (DEBUG)
                System.out.println();
		}

		simulator.updateUI();
	}

	private void updateList(String platformName) {
	
		simulator.updateSectionName("$PLATFORM$", platformName);
	}

	public boolean validate() {
		return isValidConfig;
	}

	public Platform getPlatform() {
		return plat;
	}

    public ArrayList<File> getBackupFile() {
		return backupFile;
	}

	private boolean isDummyPlatform(Ini file) {
		for (String section : file.keySet()) {
			if (section.contains("DummyPlatform")) {
				return true;
			}
		}
		return false;
	}

    private boolean isCalPlatform(Ini file) {
        for (String section : file.keySet()) {
            if (section.contains("CalPlatform")) {
                return true;
            }
        }
        return false;
    }

	private class SectionField {
		private String section;
		private String field;

		public SectionField(String section, String field) {
			this.field = field;
			this.section = section;
		}

		public String toString() {
			return new String("["+section+"] "+ field);
		}

		private IniParser getOuterType() {
			return IniParser.this;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((field == null) ? 0 : field.hashCode());
			result = prime * result + ((section == null) ? 0 : section.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			SectionField other = (SectionField) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (field == null) {
				if (other.field != null)
					return false;
			} else if (!field.equals(other.field))
				return false;
			if (section == null) {
				if (other.section != null)
					return false;
			} else if (!section.equals(other.section))
				return false;
			return true;
		}
	}

	private class FileValue {

		private String file;
		private String value;

		public FileValue(String file, String value) {
			this.file = file;
			this.value = value;
		}

		public String toString() {
			return new String(file+ " "+ value+"\n");
		}

		private IniParser getOuterType() {
			return IniParser.this;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((file == null) ? 0 : file.hashCode());
			result = prime * result + ((value == null) ? 0 : value.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			FileValue other = (FileValue) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (file == null) {
				if (other.file != null)
					return false;
			} else if (!file.equals(other.file))
				return false;
			if (value == null) {
				if (other.value != null)
					return false;
			} else if (!value.equals(other.value))
				return false;
			return true;
		}
	}



}