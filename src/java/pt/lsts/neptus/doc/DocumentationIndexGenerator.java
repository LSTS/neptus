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
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
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
 * Author: José Pinto
 * Aug 29, 2011
 */
package pt.lsts.neptus.doc;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Vector;

import pt.lsts.neptus.NeptusLog;


/**
 * This class provides a program that generates the manual index to be used in the Neptus Help window
 * @author zp
 */
public class DocumentationIndexGenerator {

    protected String[] sourceFolders = new String[] {"src", "plugins-dev"};
    protected Vector<DocumentationProvider> documentationProviders = new Vector<DocumentationProvider>();

    /**
     * This recursive method looks for Java classes that provide documentation
     * @param curPackage Current package
     * @param dir The folder where to look for classes
     * @param result Where to put the found {@link DocumentationProvider} classes
     */
    protected static void findClasses(String curPackage, File dir, Vector<DocumentationProvider> result) {
        for (File f : dir.listFiles()) {

            if (f.isFile() && f.getName().endsWith(".java")) {
                String className = f.getName().substring(0, f.getName().length()-5);
                if (curPackage.length() > 0)
                    className = curPackage+"."+className;
                NeptusLog.pub().info("<###> "+className);
                try {                          
                    Class<?> c = Class.forName(className);
                    DocumentationWrapper wrapper = new DocumentationWrapper(c);

                    for (Class<?> i : c.getInterfaces()) {
                        if (i == DocumentationProvider.class) {
                            try {
                                result.add((DocumentationWrapper)c.getDeclaredConstructor().newInstance());
                                break;
                            }
                            catch (Exception e) {
                                System.err.println(e.getClass().getSimpleName()+" while loading "+className);
                            }
                        }    
                    }
                    if (wrapper.getDocumentationFile() == null || wrapper.getDocumentationFile().length() == 0)
                        continue;
                    else
                        result.add(wrapper);
                }
                catch (Error e) {
                    System.err.println(e.getClass().getSimpleName()+" while loading "+className);
                }
                catch (Exception e) {
                    System.err.println(e.getClass().getSimpleName()+" while loading "+className);
                }
            }
            else if (f.isDirectory()) {
                if (curPackage.length() == 0)
                    findClasses(f.getName(), f, result);
                else
                    findClasses(curPackage+"."+f.getName(), f, result);                    
            }

        }
    }

    /**
     * Generates the manual index under doc/manual/index.html
     */
    public static void generateIndex() throws Exception {
        Vector<File> sourceFolders = new Vector<File>();
        sourceFolders.add(new File("src"));
        sourceFolders.addAll(Arrays.asList(new File("plugins-dev").listFiles()));
        Vector<DocumentationProvider> result = new Vector<DocumentationProvider>();
        for (File f : sourceFolders) {
            findClasses("", f, result);
        }
        LinkedHashMap<String, LinkedHashMap<String, String>> index = new LinkedHashMap<String, LinkedHashMap<String,String>>();
        
        for (DocumentationProvider s : result) {
            if (!index.containsKey(s.getSectionName()))
                index.put(s.getSectionName(), new LinkedHashMap<String, String>());
            index.get(s.getSectionName()).put(s.getArticleTitle(), s.getDocumentationFile());
        }
        Vector<String> sections = new Vector<String>();
        sections.addAll(index.keySet());
        Collections.sort(sections);
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(new File("doc/manual/index.html")));
            writer.write("<html>\n\t<head>\n\t\t<title>Neptus Help</title>\n\t\t" +
            		"<link rel=\"stylesheet\" href=\"./style.css\" type=\"text/css\">\n\t</head>\n\t<body>\n");
            for (String section : sections) {
                Vector<String> articles = new Vector<String>();
                articles.addAll(index.get(section).keySet());
                Collections.sort(articles);
                writer.write("\t\t<h2>"+section+"</h2>\n\t\t<ul>");
                for (String article : articles) {
                    writer.write("\n\t\t\t<li><a href=\""+index.get(section).get(article)+"\">"+article+"</a></li>");
                }
                writer.write("\n\t\t</ul>\n");
            }
            writer.write("\n\t</body>\n</html>\n");
            writer.close();
        }
        catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }        
        
        System.exit(0);
    }

    /**
     * This program will generate the manual index by scanning all existing Java classes for documentation
     * @param args unused
     */
    public static void main(String[] args) {
        try {
            generateIndex();
        }
        catch (Error e) {
            e.printStackTrace();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

}
