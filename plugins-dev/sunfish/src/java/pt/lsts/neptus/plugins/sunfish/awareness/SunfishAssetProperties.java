/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
 * May 9, 2014
 */
package pt.lsts.neptus.plugins.sunfish.awareness;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URL;
import java.util.Collection;
import java.util.Vector;

import org.apache.commons.io.FileUtils;

/**
 * @author zp
 *
 */
public class SunfishAssetProperties {

    private static final String assets_url = "https://docs.google.com/spreadsheets/d/1N76OoEUYvejoRBO2xZ_SL3o34q8GaC36iYb_-N94Dkg/export?gid=0&format=csv";
    private Vector<AssetDesc> lastAssets = new Vector<AssetDesc>();
    
    private Color parseColor(String hexColor) {
        if (hexColor == null || !hexColor.startsWith("#"))
            return Color.black;
        try {
            int red = Integer.valueOf(hexColor.substring(1, 3), 16);
            int green = Integer.valueOf(hexColor.substring(3, 5), 16);
            int blue = Integer.valueOf(hexColor.substring(5, 7), 16);
            return new Color(red, green, blue);
        }
        catch (Exception e) {
            return Color.black;
        }        
    }
    
    public Collection<AssetDesc> fetchAssets() {
        try {
            URL urlAssets = new URL(assets_url);
            File tmp = File.createTempFile("neptus", "assets");
            FileUtils.copyURLToFile(urlAssets, tmp);
            
            BufferedReader reader = new BufferedReader(new FileReader(tmp));
            
            String line = reader.readLine(); // header line
            Vector<AssetDesc> assets = new Vector<AssetDesc>();
            
            while ((line = reader.readLine()) != null) {
                String parts[] = line.split(",");
                AssetDesc asset = new AssetDesc();
                asset.name = parts[0].trim();
                asset.description = parts[1].trim();
                asset.color = parseColor(parts[2].trim());
                asset.friendly = parts[3].trim();
                asset.url = parts[4].trim();
                assets.add(asset);
            }
            
            reader.close();
            lastAssets = assets;
        }
        catch (Exception e) {
            e.printStackTrace();            
        }
        return lastAssets;
    }
    
    public static void main(String args[]) {
        SunfishAssetProperties props = new SunfishAssetProperties();
        System.out.println(props.fetchAssets().size());
    }
    
    public static class AssetDesc {
        public String name, description, url, friendly;
        public Color color;
    }

}
