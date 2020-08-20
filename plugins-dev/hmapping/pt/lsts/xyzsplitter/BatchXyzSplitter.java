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
 * Author: zp
 * Aug 18, 2020
 */
package pt.lsts.xyzsplitter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginUtils;

/**
 * @author zp
 *
 */
public class BatchXyzSplitter {
    
    @NeptusProperty(name = "Output folder")
    File outputFolder = new File(".");
    
    @NeptusProperty(name = "Input folder")
    File inputRootFolder = new File(".");
    
    @NeptusProperty(name = "Recursive", description = "Add any files in the subfolders")
    boolean recursive = true;
    
    @NeptusProperty(name = "File extension to process")
    String extensionName = "xyz";
    
    @NeptusProperty(name = "Case insensitive extensions")
    boolean caseInsensitive = true;
         
    public BatchXyzSplitter() {
        
    }
    
    public void process (Path file, XyzFileSplitter splitter) throws Exception {
        System.err.println("Processing "+file);        
        Files.lines(file).parallel().filter(s -> !s.startsWith("#")).forEach(l-> {
            
        });
    }
    
    public void process(XyzFileSplitter splitter) throws IOException {
        findFiles(inputRootFolder).forEach(path -> {
            try {
                process(path, splitter);    
            }
            catch (Exception e) {
                e.printStackTrace();
            }            
        });
    }
    
    public Stream<Path> findFiles(File root) throws IOException {
        int depth = 1;
        if (recursive)
            depth = Integer.MAX_VALUE;
        return Files.find(root.toPath(), depth, (p, a) -> {
            if (caseInsensitive)
                return p.getFileName().toString().toUpperCase().endsWith(extensionName.toUpperCase());
            else
                return p.getFileName().toString().endsWith(extensionName);
        });
    }
    
    
    public static void main(String[] args) throws IOException {
        BatchXyzSplitter splitter = new BatchXyzSplitter();
        PluginUtils.editPluginProperties(splitter, true);
        splitter.process(new XyzLatLonSplitter());        
    }
}
