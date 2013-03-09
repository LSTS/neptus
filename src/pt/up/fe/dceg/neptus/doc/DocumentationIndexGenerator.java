/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by zp
 * Aug 29, 2011
 */
package pt.up.fe.dceg.neptus.doc;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Vector;


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
                System.out.println(className);
                try {                          
                    Class<?> c = Class.forName(className);
                    DocumentationWrapper wrapper = new DocumentationWrapper(c);

                    for (Class<?> i : c.getInterfaces()) {
                        if (i == DocumentationProvider.class) {
                            try {
                                result.add((DocumentationWrapper)c.newInstance());
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
