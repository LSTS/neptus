/*
 * Copyright (c) 2004-2015 Universidade do Porto - Faculdade de Engenharia
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
 * European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://www.lsts.pt/neptus/licence.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: zp
 * Dec 4, 2013
 */
package pt.lsts.neptus.util.bathymetry;

import java.io.File;
import java.util.Date;

import pt.lsts.imc.lsf.LsfIndex;
import pt.lsts.neptus.mra.importers.IMraLogGroup;

/**
 * @author zp
 *
 */
public class TidePredictionFactory {

    public static TidePredictionFinder create(IMraLogGroup source) {
        TidePredictionFinder finder = null;
        File f = source.getFile("mra/tides.txt");
        if (f.canRead())
            finder = new LocalData(f);
        else {
            CachedData data = new CachedData();
            if (data.contains(new Date((long)(source.getLsfIndex().getStartTime())* 1000)))
                finder = data;
            else
                finder = null;                    
        }
        return finder;
    }
    
    public static TidePredictionFinder create(LsfIndex source) {
        TidePredictionFinder finder = null;
        File f = new File(source.getLsfFile().getParentFile(), ("mra/tides.txt"));
        if (f.canRead())
            finder = new LocalData(f);
        else {
            CachedData data = new CachedData();
            if (data.contains(new Date((long)(source.getStartTime())* 1000)))
                finder = data;
            else
                finder = null;                    
        }
        return finder;
    }
    
    public static void main(String[] args) {
        for (int i = -10; i < 10; i++) {
            Date d = new Date(System.currentTimeMillis() + 1000 * 3600 * i);
            System.out.println(d+": "+TidePrediction.getTideLevel(d));
        }
    }
}
