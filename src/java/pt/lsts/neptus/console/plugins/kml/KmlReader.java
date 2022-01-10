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
 * Author: zp
 * Apr 23, 2015
 */
package pt.lsts.neptus.console.plugins.kml;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import de.micromata.opengis.kml.v_2_2_0.Document;
import de.micromata.opengis.kml.v_2_2_0.Feature;
import de.micromata.opengis.kml.v_2_2_0.Folder;
import de.micromata.opengis.kml.v_2_2_0.GroundOverlay;
import de.micromata.opengis.kml.v_2_2_0.Kml;
import de.micromata.opengis.kml.v_2_2_0.Placemark;

/**
 * @author zp
 *
 */
public class KmlReader {

    protected Kml kml;
    protected boolean streamIsOpen;

    public KmlReader(URL url, boolean fromFile) {
        streamIsOpen = unmarshalStream(url, fromFile);
    }

    public boolean streamIsOpen() {
        return streamIsOpen;
    }
    
    private boolean unmarshalStream(URL url, boolean fromFile) {
        boolean zipped;
        
        if(!fromFile)
            zipped = true;
        else
            zipped = fileIsZipped(url);
        
        try {
            if (!zipped)
                kml = Kml.unmarshal( url.openStream());
            else {            
                ZipInputStream zip = new ZipInputStream(url.openStream());
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null)
                    if (entry.getName().endsWith(".kml")) {
                        kml = Kml.unmarshal(zip);
                        break;
                    }
            }
            
            if(kml == null)
                return false;
        }
        catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
    
    private boolean fileIsZipped(URL url) {
        return url.getPath().toString().endsWith(".kmz");
    }

    public TreeMap<String, Feature> extractFeatures() {       
        List<Feature> features = listSupportedFearures("", kml.getFeature());
        TreeMap<String, Feature> f = new TreeMap<>();

        for (Feature pm : features) {
            String featureName = parseFeatureName(pm);
            if (!f.containsKey(featureName)) {
                f.put(featureName, pm);
            }
            else {
                int maxTries = 100;
                for (int i = 1; i < maxTries; i++) {
                    String alternative = featureName + "_" + i;
                    if (!f.containsKey(alternative)) {
                        f.put(alternative, pm);
                        break;
                    }
                }
            }
        }

        return f;
    }
    
    private String parseFeatureName(Feature pm) {
        String str = pm.getName().substring(pm.getName().lastIndexOf("/") + 1);
        String featName = str.split(",")[0];
        featName = featName.trim();
        
        if (featName.isEmpty())
            featName = "id" + pm.getId();
        
        return featName;
    }

    private List<Feature> listSupportedFearures(String path, Feature f) {
        if (f instanceof Placemark || f instanceof GroundOverlay) {
            f.setName(path + f.getName());
            return Arrays.asList(f);
        }

        else if (f instanceof Folder) {
            ArrayList<Feature> ret = new ArrayList<>();
            Folder folder = (Folder)f;
            for (Feature j : folder.getFeature())
                ret.addAll(listSupportedFearures(path+folder.getName()+"/", j));  
            return ret;
        }

        else if (f instanceof Document) {
            ArrayList<Feature> ret = new ArrayList<>();
            Document d = (Document)f;
            for (Feature j : d.getFeature())
                ret.addAll(listSupportedFearures("/", j));
            return ret;
        }
        else 
            return new ArrayList<>();
    }   

    public static void main(String[] args) throws Exception {
        KmlReader browser = new KmlReader(new URL("https://www.google.com/maps/d/kml?mid=z4oHb_uriB5A.kLTuB2xlrlcc"), false);
        browser.extractFeatures();
    }
}
