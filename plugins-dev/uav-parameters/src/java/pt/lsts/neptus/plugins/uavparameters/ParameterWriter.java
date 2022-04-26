/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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
 * Author: dronekit.io, Manuel R.
 * Nov 22, 2016
 */
package pt.lsts.neptus.plugins.uavparameters;

import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ParameterWriter {
    private List<Parameter> parameterList;
    private DecimalFormat df = null;

    public ParameterWriter(List<Parameter> param) {
        this.parameterList = param;
        df = new DecimalFormat("0.#########", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
    }

    public boolean saveParametersToFile(String file) {
        if (file == null)
            return false;
        try {
            FileOutputStream out = new FileOutputStream(file);
            writeFirstLine(out);

            writeWaypointsLines(out);
            out.close();

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private void writeFirstLine(FileOutputStream out) throws IOException {
        SimpleDateFormat sdfDate = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");//dd/MM/yyyy
        Date now = new Date();
        String strDate = sdfDate.format(now);

        out.write((("#NOTE: " + strDate + "\n").getBytes()));
    }

    private void writeWaypointsLines(FileOutputStream out) throws IOException {
        for (Parameter param : parameterList) {
            out.write(String.format(Locale.ENGLISH, "%s,%s\n", param.name, df.format(param.value))
                    .getBytes());
        }
    }
}
