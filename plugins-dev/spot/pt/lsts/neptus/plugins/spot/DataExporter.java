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
 * Author: meg
 * Jun 12, 2013
 */
package pt.lsts.neptus.plugins.spot;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;

import pt.lsts.neptus.NeptusLog;

/**
 * @author meg
 *
 */
public class DataExporter {

    public static boolean exportToCsv(HashMap<String, TreeSet<SpotMessage>> msgBySpot) {
        PrintWriter writer;
        try {
            writer = new PrintWriter("spotMessages.csv", "UTF-8");
            writer.println("Id;Timestamp;Latitude;Longitude");
            Collection<TreeSet<SpotMessage>> spotIds = msgBySpot.values();
            TreeSet<SpotMessage> msgTreeSet;
            // iterate over spots mentioned in messages
            for (Iterator<TreeSet<SpotMessage>> iterator = spotIds.iterator(); iterator.hasNext();) {
                msgTreeSet = iterator.next();
                for (SpotMessage spotMessage : msgTreeSet) {
                    writer.println(spotMessage.id + ";" + spotMessage.timestamp + ";" + spotMessage.latitude + ";"
                            + spotMessage.longitude + ";");
                }
                writer.println();
            }

            writer.close();
            return true;
        }
        catch (FileNotFoundException | UnsupportedEncodingException e) {
            NeptusLog.pub().error("Error printing spot data to cvs", e);
            return false;
        }
    }

}
