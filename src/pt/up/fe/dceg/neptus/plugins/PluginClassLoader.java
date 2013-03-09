/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by 
 * 20??/??/??
 */
package pt.up.fe.dceg.neptus.plugins;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.swing.JFrame;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.util.ReflectionUtil;
import pt.up.fe.dceg.neptus.util.conf.ConfigFetch;

public class PluginClassLoader extends URLClassLoader {

    private static PluginClassLoader instance = null;

    public static PluginClassLoader getInstance() {
        return instance;
    }

    public static Object getInstance(String classname) {
        try {
            Class<?> o = instance.loadClass(classname);
            return o.newInstance();
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void install(String[] pluginsDirs) {

        ClassLoader currentThreadClassLoader = Thread.currentThread().getContextClassLoader();

        File[] dirs = new File[pluginsDirs.length];
        for (int i = 0; i < pluginsDirs.length; i++) {
            dirs[i] = new File(pluginsDirs[i]);
        }

        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {

            @Override
            public void uncaughtException(Thread t, Throwable e) {
                if (ConfigFetch.INSTANCE == null) {
                    System.err.println("Uncaught exception: " + e.getMessage());
                }
                else
                    NeptusLog.pub().error("Uncaught Exception! " + ReflectionUtil.getCallerStamp(), e);
            }
        });

        instance = new PluginClassLoader(dirs, currentThreadClassLoader);
        Thread.currentThread().setContextClassLoader(instance);

    }

    public static void install() {
        install(new String[] { "source", "plugins", "plugins-dev" });
    }

    private PluginClassLoader(File[] pluginsDirs, ClassLoader parent) {
        super(listPlugins(pluginsDirs).toArray(new URL[] {}), parent);
        Vector<URL> urls = listPlugins(pluginsDirs);

        try {
            for (URL url : urls) {
                PluginClassLoader.addToSysClassLoader(url);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        parsePlugins(urls);

    }

    private static String getFileExtension(File fx) {
        String path = null;
        try {
            path = fx.getCanonicalPath();
        }
        catch (IOException e1) {
            path = fx.getAbsolutePath();
        }
        int lastDotPostion = path.lastIndexOf('.');
        return (lastDotPostion != -1) ? (path.substring(lastDotPostion + 1)) : "";
    }

    private static void parsePlugins(Vector<URL> plugins) {
        for (URL url : plugins) {
            try {
                File f = new File(url.toURI());
                ZipFile zf = null;
                BufferedReader br = null;

                if (f.isDirectory() && new File(f, "plugins.lst").isFile()) {
                    br = new BufferedReader(new FileReader(new File(f, "plugins.lst")));
                }
                else {
                    zf = new ZipFile(f);
                    // look for SPI services...
                    ZipEntry servEntry = zf.getEntry("META-INF/services/");
                    if (servEntry != null && servEntry.isDirectory()) {
                        Enumeration<?> entries = zf.entries();
                        while (entries.hasMoreElements()) {
                            ZipEntry entry = (ZipEntry) entries.nextElement();
                            if (entry.getName().startsWith("META-INF/services/") && !entry.isDirectory()) {

                                String interf = entry.getName().substring(entry.getName().lastIndexOf('/') + 1);
                                Class<?> itf = null;
                                try {
                                    itf = ClassLoader.getSystemClassLoader().loadClass(interf);
                                }
                                catch (Exception e) {
                                    continue;
                                }

                                br = new BufferedReader(new InputStreamReader(zf.getInputStream(entry)));

                                String line = br.readLine();
                                while (line != null) {
                                    line = line.trim();
                                    if (line.length() > 0 && line.charAt(0) != '#') {
                                        try {
                                            // Add a service implementation to
                                            // the plugins repository
                                            PluginsRepository.addOtherPlugin(itf, ClassLoader.getSystemClassLoader()
                                                    .loadClass(line));
                                            // System.out.println("\t"+line);
                                        }
                                        catch (Exception e) {
                                        }
                                    }

                                    line = br.readLine();
                                }
                            }
                        }
                    }

                    // try to open the file "plugins.lst"
                    ZipEntry pluginsEntry = zf.getEntry("plugins.lst");

                    // System.out.println(pluginsEntry);
                    if (pluginsEntry != null && !pluginsEntry.isDirectory()) {
                        br = new BufferedReader(new InputStreamReader(zf.getInputStream(pluginsEntry)));
                    }
                }

                if (br == null)
                    continue;

                String line = br.readLine();
                while (line != null) {
                    line = line.trim();
                    if (line.length() > 0 && line.charAt(0) != '#') {
                        try {
                            PluginsRepository.addPlugin(line);
                        }
                        catch (Exception e) {
                        }
                    }
                    line = br.readLine();
                }
                if (zf != null)
                    zf.close();

            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static Vector<URL> listPlugins(File[] pluginsDirs) {
        Vector<URL> urls = new Vector<URL>();

        for (File subdir : pluginsDirs) {
            File[] subF = subdir.listFiles();
            if (subF == null)
                continue;

            for (File file : subF) {
                if (file.isDirectory()) {
                    File desc = new File(file, "plugins.lst");
                    if (desc.canRead()) {
                        try {
                            urls.add(file.toURI().toURL());
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    urls.addAll(listPlugins(new File[] { file }));
                }

                if (getFileExtension(file).equalsIgnoreCase("jar")) {

                    try {
                        urls.add(file.toURI().toURL());
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        return urls;
    }

    private static final Class<?>[] parameters = new Class[] { URL.class };

    private static void addToSysClassLoader(URL u) throws Exception {

        URLClassLoader sysloader = (URLClassLoader) ClassLoader.getSystemClassLoader();
        Class<?> sysclass = URLClassLoader.class;

        try {
            Method method = sysclass.getDeclaredMethod("addURL", parameters);
            method.setAccessible(true);
            method.invoke(sysloader, new Object[] { u });
        }
        catch (Throwable t) {
            t.printStackTrace();
            throw new Exception("Error, could not add URL to system classloader: " + t.getMessage());
        }// end try catch

    }

    public static void launch(String[] args) throws Exception {

    }

    public static void main(String[] args) throws Exception {
        PluginClassLoader.install();
        Class.forName("pt.up.fe.dceg.neptus.plugins.CounterPlugin");
        JFrame frame = new JFrame("rwst");
        frame.setVisible(true);
        // Class.forName("com.jgoodies.looks.windows.WindowsIconFactory");
    }
}
