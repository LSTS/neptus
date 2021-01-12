/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
 * Author: 
 * 20??/??/??
 */
package pt.lsts.neptus.plugins;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.reflections.Reflections;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.util.conf.ConfigFetch;
import pt.lsts.neptus.util.conf.ConfigFetch.Environment;

/**
 * This class has static methods to load Neptus plugins
 * 
 * @author Hugo
 * 
 */
public class PluginsLoader {

//    private static final Class<?>[] parameters = new Class[] { URL.class };

    /**
     * Loads Plugins according to the ENVIROMENT<br>
     * ConfigFetch.initilize() MUST be ran before loading plugins to set the ENVIROMENT
     */
    public static void load() {
        loadCorePlugins();

        if (ConfigFetch.getRunEnvironment() == Environment.PRODUCTION) {
            List<Path> pluginsJars = findJars();

            try {
                for (Path jar : pluginsJars) {
                    try {
//                        addToSysClassLoader(jar.toUri().toURL());
                        FindPlugins plugins = new FindPlugins();
                        FileSystem zipFileSystem = createZipFileSystem(jar.toAbsolutePath().toString(), false);
                        final Path root = zipFileSystem.getPath("/");
                        Files.walkFileTree(root, plugins);
                        List<Path> pluginsLST = plugins.getPlugins();
                        for (Path lst : pluginsLST) {
                            loadPluginFromLST(lst);
                        }
                    }
                    catch (Exception e) {
                        NeptusLog.pub().error("Error loading plugin from jars", e);
                    }
                }
            }
            catch (Exception e) {
                NeptusLog.pub().error("Error getting plugins from jars", e);
            }
        }

        if (ConfigFetch.getRunEnvironment() == Environment.DEVELOPMENT) {
//            List<Path> externalJars = findExternalPluginsJars();
//
//            try {
//                for (Path jar : externalJars) {
//                    try {
//                        addToSysClassLoader(jar.toUri().toURL());
//                    }
//                    catch (Exception e) {
//                        NeptusLog.pub().error("Error loading plugin jar from dev", e);
//                    }
//                }
//            }
//            catch (Exception e) {
//                NeptusLog.pub().error("Error getting plugins from dev", e);
//            }

            FindPlugins plugins = new FindPlugins();
            Path start = Paths.get("plugins-dev");
            try {
                Files.walkFileTree(start, plugins);
                List<Path> pluginsLST = plugins.getPlugins();
                for (Path lst : pluginsLST) {
                    loadPluginFromLST(lst);
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Loads core plugins inside the src folder in the "pt.lsts.neptus.console.plugins" package
     */
    private static void loadCorePlugins() {
        for (String pkg : new String[] { "pt.lsts.neptus.console.plugins", "pt.lsts.neptus.mra",
                "pt.lsts.neptus.controllers", "pt.lsts.neptus.mp.element" }) {
            Reflections reflections = new Reflections(pkg);
            for (Class<?> c : reflections.getTypesAnnotatedWith(PluginDescription.class)) {
                PluginsRepository.addPlugin(c.getCanonicalName());
            }
        }
    }

    private static List<Path> findJars() {
        Path start = Paths.get("plugins");
        List<Path> jars;
        FindJars findJars = new FindJars();
        try {
            Files.walkFileTree(start, findJars);
            jars = findJars.getJars();
            return jars;
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static List<Path> findExternalPluginsJars() {
        Path start = Paths.get("plugins-dev");
        List<Path> jars;
        FindJars findJars = new FindJars();
        try {
            Files.walkFileTree(start, findJars);
            jars = findJars.getJars();
            return jars;

        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

//    private static void addToSysClassLoader(URL u) throws Exception {
//
////        URLClassLoader sysloader = (URLClassLoader) ClassLoader.getSystemClassLoader();
////        Class<?> sysclass = URLClassLoader.class;
//
//        try {
////            Method method = sysclass.getDeclaredMethod("addURL", parameters);
////            method.setAccessible(true);
////            method.invoke(sysloader, new Object[] { u });
//            PluginsClassLoader.classLoader.addURL(u);
//        }
//        catch (Throwable t) {
//            t.printStackTrace();
//            throw new Exception("Error, could not add URL to system classloader: " + t.getMessage());
//        }
//    }

    private static void loadPluginFromLST(Path file) {
        Charset charset = Charset.forName("UTF-8");
        try (BufferedReader reader = Files.newBufferedReader(file, charset)) {
            String line = null;
            
            while ((line = reader.readLine()) != null) {
                if (line.length() > 0 && line.charAt(0) != '#') {
                    PluginsRepository.addPlugin(line);
                }
            }
        }
        catch (IOException x) {
            System.err.format("IOException: %s%n", x);
        }
    }

    protected static class FindJars extends SimpleFileVisitor<Path> {
        List<Path> jars = new ArrayList<>();

        /**
         * @return the jars
         */
        public List<Path> getJars() {
            return jars;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            if (FilenameUtils.getExtension(file.getFileName().toString()).equalsIgnoreCase("jar")) {
                jars.add(file);
                // System.out.println(file.toAbsolutePath());
            }
            return FileVisitResult.CONTINUE;
        }
    }

    protected static class FindPlugins extends SimpleFileVisitor<Path> {
        List<Path> plugins = new ArrayList<>();

        public List<Path> getPlugins() {
            return plugins;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            if (file.getFileName().toString().equalsIgnoreCase("plugins.lst")) {
                plugins.add(file);
                // System.out.println(file.toAbsolutePath());
                return FileVisitResult.SKIP_SIBLINGS;
            }
            return FileVisitResult.CONTINUE;
        }
    }

    private static FileSystem createZipFileSystem(String zipFilename, boolean create) throws IOException {
        // convert the filename to a URI
        final Path path = Paths.get(zipFilename);
        final URI uri = URI.create("jar:file:" + path.toUri().getRawPath());

        final Map<String, String> env = new HashMap<>();
        if (create) {
            env.put("create", "true");
        }
        return FileSystems.newFileSystem(uri, env);
    }
}
