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
 * Author: nachito
 * 23/04/2018
 */
package pt.lsts.neptus.plugins.gebcoimport;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


import javax.swing.JFileChooser;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.plugins.planning.SimulatedBathymetry;
import pt.lsts.neptus.data.Pair;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.AngleUtils;
import pt.lsts.neptus.util.ConsoleParse;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.netcdf.NetCDFUtils;
import ucar.ma2.Array;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

@PluginDescription(name = "NetCDF Tools")
public class GebcoBathymetryImport extends ConsolePanel {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;



    /**
     * 
     */

    /**
     * @param console
     */

    // TODO Auto-generated method stub

    public GebcoBathymetryImport(ConsoleLayout console) {

        super(console);
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.console.ConsolePanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
        removeMenuItem(I18n.text("Tools") + ">" + I18n.text("NetCDF"));

    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.console.ConsolePanel#initSubPanel()
     */
    @Override
    public void initSubPanel() {


        addMenuItem(I18n.text("Tools") + ">" + I18n.text("NetCDF"),
                ImageUtils.getIcon(PluginUtils.getPluginIcon(getClass())), new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {


                JFileChooser fileChooser = new JFileChooser();
                int returnValue = fileChooser.showOpenDialog(null);
                if (returnValue == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();


                    NetcdfFile dataFile = null;
                    try {
                        dataFile = NetcdfFile.open(selectedFile.getAbsolutePath());
                        System.out.println(dataFile.toString());
                        Pair<String, Variable> searchPair = NetCDFUtils.findVariableForStandardNameOrName(
                                dataFile, selectedFile.getAbsolutePath(), true, "latitude", "lat");

                        Variable latVar = searchPair.second();

                        searchPair = NetCDFUtils.findVariableForStandardNameOrName(dataFile,
                                selectedFile.getAbsolutePath(), true, "longitude", "lon");
                        Variable lonVar = searchPair.second();

                        searchPair = NetCDFUtils.findVariableForStandardNameOrName(dataFile,
                                selectedFile.getAbsolutePath(), true, "elevation",
                                "height_above_reference_ellipsoid");
                        Variable elevVar = searchPair.second();

                        searchPair = NetCDFUtils.findVariableForStandardNameOrName(dataFile,
                                selectedFile.getAbsolutePath(), true, "time");
                        Variable timeVar = searchPair == null ? null : searchPair.second();

                        searchPair = NetCDFUtils.findVariableForStandardNameOrName(dataFile,
                                selectedFile.getAbsolutePath(), false, "depth", "altitude");
                        Variable depthOrAltitudeVar = searchPair == null ? null : searchPair.second();

                        // Get the u (north) wind velocity Variables.
                        searchPair = NetCDFUtils.findVariableForStandardNameOrName(dataFile,
                                selectedFile.getAbsolutePath(), false, "x_wind", "grid_eastward_wind", "u");
                        Variable uVar = searchPair == null ? null : searchPair.second();

                        // Get the v (east) wind velocity Variables.
                        searchPair = NetCDFUtils.findVariableForStandardNameOrName(dataFile,
                                selectedFile.getAbsolutePath(), false, "y_wind", "grid_northward_wind", "v");
                        Variable vVar = searchPair == null ? null : searchPair.second();

                        if (latVar == null) {
                            System.out.println("Cant find Variable latitude");
                        }
                        if (lonVar == null) {
                            System.out.println("Cant find Variable longitude");
                        }
                        if (timeVar == null) {
                            System.out.println("Cant find Variable time");
                        }
                        if (uVar == null) {
                            System.out.println("Cant find Variable xWind");
                        }
                        if (vVar == null) {
                            System.out.println("Cant find Variable yWind");
                        }
                        if (elevVar == null) {
                            System.out.println("Cant find Variable elevation");
                        }

                        if (depthOrAltitudeVar == null) {
                            System.out.println("Cant find Variable depth or altitude");

                        }

                        //TODO add u and v
                        // Get the lat/lon data from the file.
                        Array latArray; // ArrayFloat.D1
                        Array lonArray; // ArrayFloat.D1
                        Array elevArray;

                        latArray = latVar.read();
                        lonArray = lonVar.read();
                        elevArray = elevVar.read();



                        double lat = 0;
                        double lon = 0;
                        double elev=0;

                 
                        Map<LocationType, Double> data= new HashMap<>();


                        for(int i = 0; i < elevArray.getSize(); i++) {
                            lat = AngleUtils.nomalizeAngleDegrees180((latArray.getDouble((int) (i/lonArray.getSize()))));

                            lon = AngleUtils.nomalizeAngleDegrees180(lonArray.getDouble((int) (i%lonArray.getSize())));

                            elev = elevArray.getDouble(i);
                            LocationType l = new LocationType();
                            l.setLatitudeDegs(lat);
                            l.setLongitudeDegs(lon);
                            l.setDepth(elev);
                            System.out.println("l: " + l);

                            data.put(l, l.getDepth());
                        }
                        
                 
                        SimulatedBathymetry.getInstance().addSoundings(data);

                    }
                    catch (java.io.IOException e1) {
                        System.out.println(" fail = " + e1);
                        e1.printStackTrace();
                    }
                    catch (Exception e1) {
                        e1.printStackTrace();
                    }
                    finally {
                        if (dataFile != null)
                            try {
                                dataFile.close();
                            }
                        catch (IOException ioe) {
                            ioe.printStackTrace();
                        }
                    }

                }



            }
        });

    }



    public static void main(String[] args) {
        ConsoleParse.testSubPanel(GebcoBathymetryImport.class);
    }
}