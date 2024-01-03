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
 * Author: zp
 * May 14, 2018
 */
package pt.lsts.neptus.soi;

import java.util.LinkedHashMap;

/**
 * @author zp
 *
 */
public class WmsLayerFactory {

    public static WmsLayer Salinity() {
        LinkedHashMap<String, String> options = new LinkedHashMap<>();
        options.put("LAYERS", "so");
        options.put("FORMAT", "image/png");
        options.put("TRANSPARENT", "true");
        options.put("SERVICE", "WMS");
        options.put("VERSION", "1.1.1");
        options.put("SRS", "EPSG:4326");
        options.put("STYLES", "boxfill/rainbow");
        options.put("TIME", "2018-05-02T12:00:00.000Z");
        options.put("ELEVATION", "-0.49402499198913574");
        options.put("BELOWMINCOLOR", "extend");
        options.put("BELOWMAXCOLOR", "extend");
        options.put("LOGSCALE", "false");
        options.put("COLORSCALERANGE", "30.0,38.0");

        WmsLayer layer = new WmsLayer(
                "http://nrt.cmems-du.eu/thredds/wms/global-analysis-forecast-phy-001-024?REQUEST=GetMap", options);
        return layer;
    }
    
    public static WmsLayer Temperature() {
        LinkedHashMap<String, String> options = new LinkedHashMap<>();
        options.put("LAYERS", "thetao");
        options.put("FORMAT", "image/png");
        options.put("TRANSPARENT", "true");
        options.put("SERVICE", "WMS");
        options.put("VERSION", "1.1.1");
        options.put("SRS", "EPSG:4326");
        options.put("STYLES", "boxfill/rainbow");
        options.put("TIME", "2018-05-02T12:00:00.000Z");
        options.put("ELEVATION", "-0.49402499198913574");
        options.put("BELOWMINCOLOR", "extend");
        options.put("BELOWMAXCOLOR", "extend");
        
        options.put("LOGSCALE", "false");
        options.put("COLORSCALERANGE", "0,30.0");

        WmsLayer layer = new WmsLayer(
                "http://nrt.cmems-du.eu/thredds/wms/global-analysis-forecast-phy-001-024?REQUEST=GetMap", options);
        return layer;
    }
    
    public static WmsLayer Wind() {
        LinkedHashMap<String, String> options = new LinkedHashMap<>();
        options.put("LAYERS", "wind");
        options.put("FORMAT", "image/png");
        options.put("TRANSPARENT", "true");
        options.put("SERVICE", "WMS");
        options.put("VERSION", "1.1.1");
        options.put("SRS", "EPSG:4326");
        options.put("STYLES", "vector/rainbow");
        options.put("TIME", "2018-05-02T12:00:00.000Z");
        options.put("ELEVATION", "10");
        options.put("BELOWMINCOLOR", "extend");
        options.put("BELOWMAXCOLOR", "extend");
        options.put("LOGSCALE", "false");
        options.put("COLORSCALERANGE", "0,25.0");

        WmsLayer layer = new WmsLayer(
                "http://nrt.cmems-du.eu/thredds/wms/CERSAT-GLO-BLENDED_WIND_L4-V5-OBS_FULL_TIME_SERIE?REQUEST=GetMap", options);
        return layer;
    }
    
}
