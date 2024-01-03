/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
 * Author: Renato Campos
 * 14 Nov 2018
 */
package pt.lsts.neptus.mra.importers.i872;

import java.io.File;
import java.util.ArrayList;

import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.mra.api.SidescanLine;
import pt.lsts.neptus.mra.api.SidescanParameters;
import pt.lsts.neptus.mra.api.SidescanParser;
import pt.lsts.neptus.mra.api.SidescanUtil;

public class Imagenex872SidescanParser implements SidescanParser {

    private Imagenex872Parser parser;
    
    public Imagenex872SidescanParser(File f) {
        parser = new Imagenex872Parser(f);
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.mra.api.SidescanParser#firstPingTimestamp()
     */
    @Override
    public long firstPingTimestamp() {
        return parser.getFirstTimestamp();
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.mra.api.SidescanParser#lastPingTimestamp()
     */
    @Override
    public long lastPingTimestamp() {
        return parser.getLastTimestamp();
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.mra.api.SidescanParser#getLinesBetween(long, long, int, pt.lsts.neptus.mra.api.SidescanParameters)
     */
    @Override
    public ArrayList<SidescanLine> getLinesBetween(long timestamp1, long timestamp2, int subsystem,
            SidescanParameters config) {
        ArrayList<SidescanLine> list = new ArrayList<SidescanLine>();
        Imagenex872Ping currentPing;
        if (timestamp1 < 0 || timestamp2 < 0) {
            return list;
        }
        currentPing = parser.getPingAt(timestamp1);
        if (currentPing == null) {
            return list;
        }
        
        while(currentPing.getTimestamp() < timestamp2) {
            double fData[] = new double[Imagenex872Ping.DATA_SIZE*2];
            int[] portData = currentPing.getPortData();
            int[] starboardData = currentPing.getStarboardData();
            for (int i = 0; i < Imagenex872Ping.DATA_SIZE; i++) {
                fData[i] = portData[i];
            }
            for (int i = 0; i < Imagenex872Ping.DATA_SIZE; i++) {
                fData[i + Imagenex872Ping.DATA_SIZE] = starboardData[i];
            }
            SystemPositionAndAttitude pose = new SystemPositionAndAttitude();
            
            pose.getPosition().setLatitudeDegs(currentPing.getLatitudeDegs());
            pose.getPosition().setLongitudeDegs(currentPing.getLongitudeDegs());
            
            fData = SidescanUtil.applyNormalizationAndTVG(fData, currentPing.getRange(), config);
            
            list.add(new SidescanLine(currentPing.getTimestamp(), currentPing.getRange(), pose, currentPing.getFrequency(), fData));
            currentPing = parser.getPingAt(currentPing.getTimestamp()+1);
        }
        return list;
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.mra.api.SidescanParser#getSubsystemList()
     */
    @Override
    public ArrayList<Integer> getSubsystemList() {
        ArrayList<Integer> subsystems = new ArrayList<Integer>();
        subsystems.add(0);
        return subsystems;
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.mra.api.SidescanParser#cleanup()
     */
    @Override
    public void cleanup() {
        parser.cleanup();
        parser = null;
    }

}
