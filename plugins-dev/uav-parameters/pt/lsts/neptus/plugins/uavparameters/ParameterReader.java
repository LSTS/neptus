/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
 * Author: dronekit.io
 * Nov 22, 2016
 */
package pt.lsts.neptus.plugins.uavparameters;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class ParameterReader {
    private List<Parameter> parameters;

    public ParameterReader() {
        this.parameters = new ArrayList<Parameter>();
    }

    public boolean openFile(String file) {
        if (file == null) {
            return false;
        }
        try {
            FileInputStream in = new FileInputStream(file);
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));

            if (!isParameterFile(reader)) {
                in.close();
                return false;
            }
            parseWaypointLines(reader);

            in.close();

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    private void parseWaypointLines(BufferedReader reader) throws IOException {
        String line;
        parameters.clear();
        while ((line = reader.readLine()) != null) {
            try {
                parseLine(line);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void parseLine(String line) throws Exception {
        String[] rowData = splitLine(line);
        String name = rowData[0];
        Double value = Double.valueOf(rowData[1]);

        parameters.add(new Parameter(name, value));
    }

    private String[] splitLine(String line) throws Exception {
        String[] rowData = line.split(",");
        if (rowData.length != 2) {
            throw new Exception("Invalid Length");
        }
        rowData[0] = rowData[0].trim();
        return rowData;
    }

    private static boolean isParameterFile(BufferedReader reader) throws IOException {
        return reader.readLine().contains("#NOTE");
    }

    public List<Parameter> getParameters() {
        return parameters;
    }

    public static void main(String[] args) {
        ParameterReader reader = new ParameterReader();
        reader.openFile("/home/manuel/Downloads/mariner-01.param");
        System.out.println(reader.getParameters().size());

        ParameterWriter writer = new ParameterWriter(reader.getParameters());
        boolean r = writer.saveParametersToFile("/home/manuel/Downloads/mariner-01.copy");
        System.out.println(r);
    }
}
