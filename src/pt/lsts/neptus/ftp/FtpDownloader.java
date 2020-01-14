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
 * Author: José Correia
 * Dec 11, 2012
 */
package pt.lsts.neptus.ftp;

import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPFile;

import pt.lsts.neptus.NeptusLog;

/**
 * @author jqcorreia
 * 
 */
public class FtpDownloader {
    private static final int DATA_TIMEOUT_MILLIS = 15000;

    private FTPClient client;

    private FTPClientConfig conf;

    private String host;
    private int port;

    private String username = null;
    private String password = null;
    
    public FtpDownloader(String host, int port) throws Exception {
        this.host = host;
        this.port = port;
//        renewClient();
    }

    public void renewClient() throws SocketException, IOException {
        renewClient(username, password);
    }

    /**
     * @throws SocketException
     * @throws IOException
     */
    public void renewClient(String username, String password) throws SocketException, IOException {
        if (client != null) {
            try {
                if (client.isConnected())
                    client.disconnect();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        if (client == null) {
            client = new FTPClient();
            conf = new FTPClientConfig(FTPClientConfig.SYST_UNIX);

//        client.setDataTimeout(30000);
//        client.setSoTimeout(30000);

          client.setDataTimeout(DATA_TIMEOUT_MILLIS);
//          client.setSoTimeout(300000); // N funciona, não liga

        }
        
        NeptusLog.pub().warn(FtpDownloader.class.getSimpleName() + " :: " + "connecting to " + host + ":" + port);
        long t1 = System.currentTimeMillis();
        client.connect(host, port);
        NeptusLog.pub().warn(FtpDownloader.class.getSimpleName() + " :: " + "connected to " + host + ":" + port + " took " + (System.currentTimeMillis() - t1) + "ms");
        client.configure(conf);

        client.enterLocalPassiveMode();
        
        if (username != null)
            this.username = username;
        if (password != null)
            this.password = password;
        
        client.login(username == null ? "anonymous" : username, password == null ? "" : password);

        client.setFileType(FTP.BINARY_FILE_TYPE);
        client.setControlEncoding("UTF-8");
    }

    /**
     * @return the host
     */
    public String getHost() {
        return host;
    }
    
    /**
     * @return the port
     */
    public int getPort() {
        return port;
    }

    /**
     * @param host the host to set
     */
    public void setHostAndPort(String host, int port) {
        if (!this.host.equals(host) || this.port != port) {
            try {
                renewClient();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        this.host = host;
        this.port = port;
    }
    
//    public void downloadDirectory(String path, String destPath) throws Exception {
//        System.out.println(FtpDownloader.class.getSimpleName() + " :: " + "Path :" + path);
//        System.out.println(FtpDownloader.class.getSimpleName() + " :: " + "DestPath: " + destPath);
//        ArrayList<FTPFile> toDoList = new ArrayList<FTPFile>();
//
//        if (!client.isConnected()) {
//            try {
//                renewClient();
//            }
//            catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//
//        FTPFile[] lstFiles = client.listFiles(new String(path.getBytes(), "ISO-8859-1"));
//        if (lstFiles.length == 0) {
//            NeptusLog.pub().warn(FtpDownloader.class.getSimpleName() + " :: No files in downloading folder '" + path + "' from " + host);
//            return;
//        }
//        for (FTPFile f : lstFiles) {
//            if(f.isDirectory()) {
//                toDoList.add(f);
//            }
//            else {
//                String filePath =  path + (path.equals("/") ? "" : "/") + f.getName();
//                System.out.println(FtpDownloader.class.getSimpleName() + " :: " + "Downloading " + filePath);
//                downloadFile(filePath, destPath);
//            }
//        }
//        for(FTPFile f : toDoList) {
////            System.out.println(path + f.getName());
//            downloadDirectory(path + "/" + f.getName(), destPath);
//        }
//    }
    
    public LinkedHashMap<String, FTPFile> listDirectory(String path) throws Exception {
        ArrayList<FTPFile> toDoList = new ArrayList<FTPFile>();
        LinkedHashMap<String, FTPFile> finalList = new LinkedHashMap<String, FTPFile>();
        
        if (!client.isConnected()) {
            try {
                renewClient();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        FTPFile[] lstFiles = client.listFiles(new String(path.getBytes(), "ISO-8859-1"));
        if (lstFiles.length == 0) {
            NeptusLog.pub().warn(FtpDownloader.class.getSimpleName() + " :: No files listing in folder '" + path + "' from " + host);
            return finalList;
        }
        for (FTPFile f : lstFiles) {
            if(f.isDirectory()) {
                toDoList.add(f);
            }
            else {
                String filePath =  path + (path.equals("/") ? "" : "/") + f.getName();
                finalList.put(filePath, f);
            }
        }
        for(FTPFile f : toDoList) {
            finalList.putAll(listDirectory(path + "/" + f.getName()));
        }
        return finalList;
    }
    
    public LinkedHashMap<FTPFile, String> listLogs() throws IOException {
        LinkedHashMap<FTPFile, String> list = new LinkedHashMap<FTPFile, String>();
        
        if (isConnected()) {
            try {
                renewClient();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        FTPFile[] lstFiles = client.listFiles("/");
        if (lstFiles.length == 0) {
            NeptusLog.pub().warn(FtpDownloader.class.getSimpleName() + " :: Empty listing folder '/' from " + host);
            return list;
        }
        for (FTPFile f : lstFiles) {
            if(f.isDirectory()) {
                String workingDirectory = "/" + new String(f.getName().getBytes(), "ISO-8859-1"); 
                FTPFile[] files = client.listFiles(workingDirectory);
                
                if(files.length == 0) {
                    NeptusLog.pub().warn(":: " + workingDirectory + " has 0 files. Deleting folder");
                    boolean deletionOfEmptyLogFolder = client.deleteFile(workingDirectory);
                    if (!deletionOfEmptyLogFolder) {
                        NeptusLog.pub().warn(":: " + workingDirectory + " has 0 files. Fail deletion of folder");
                    }
                }
                for (FTPFile f2 : files) {
                    list.put(f2, f.getName() + "/" + f2.getName());
                }
            }
        }
        return list;
    }

//    private void downloadFile(String filePath, String destPath) {
//        String toks[] = filePath.split("/");
//        String fileName = toks[toks.length - 1];
//
//        if (!isConnected()) {
//            try {
//                renewClient();
//            }
//            catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//
//        try {
//            String dest = destPath + fileName;
//            boolean ret = retrieveFile(client.retrieveFileStream(new String(filePath.getBytes(), "ISO-8859-1")), new FileOutputStream(new File(dest)));
//            if (!ret) {
//                NeptusLog.pub().warn(
//                        FtpDownloader.class.getSimpleName() + " :: Error downloading file '" + filePath + "' from " + host);
//            }
//
//            System.out.println(FtpDownloader.class.getSimpleName() + " :: " + dest);
//        }
//        catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

//    private boolean retrieveFile(InputStream is, OutputStream os) {
//        try {
//            System.out.println(FtpDownloader.class.getSimpleName() + " :: " + is);
//
//            Util.copyStream(is, os, 1024, CopyStreamEvent.UNKNOWN_STREAM_SIZE, new CopyStreamListener() {
//                @Override
//                public void bytesTransferred(long arg0, int arg1, long arg2) {
//                    System.out.println(FtpDownloader.class.getSimpleName() + " :: " + "1 " + " " + arg0 + " " + arg1 + " " + arg2);
//                }
//
//                @Override
//                public void bytesTransferred(CopyStreamEvent arg0) {
//                    System.out.println(FtpDownloader.class.getSimpleName() + " :: " + "2 " + arg0);
//                }
//            }, false);
//        }
//        catch (IOException e) {
//            e.printStackTrace();
//        }
//        return true;
//    }

    public FTPClient getClient() {
        return client;
    }
    
    public boolean isConnected() {
        if (client == null)
            return false;
        
        return client.isConnected();
    }
    
    public void close() throws IOException {
        if (client != null)
            client.disconnect();
    }

    public static void main(String[] args) throws Exception {
        FtpDownloader test = new FtpDownloader("10.0.10.80", 30021);
        @SuppressWarnings("unused")
        LinkedHashMap<String, FTPFile> res = new LinkedHashMap<>();
        
        System.out.println(test.getClient().getControlEncoding());
        
        test.getClient().setControlEncoding("UTF-8");
        test.getClient().changeWorkingDirectory("/20130730/");
        
        String foo = test.getClient().listFiles()[6].getName();
        
        System.out.println(foo + " size: " + foo.length());
        
        for(byte b : foo.getBytes())
            System.out.print(String.format("%02X", b));
        System.out.println();
        
        String foo2 = new String(foo.getBytes(), "ISO-8859-1");
        
        System.out.println(foo2 + " size: " + foo2.length());
        for(byte b : foo2.getBytes())
            System.out.print(String.format("%02X", b));
        System.out.println();
        
        System.out.println(test.getClient().changeWorkingDirectory(foo));

        for(FTPFile s : test.getClient().listFiles()) {
            System.out.println(s.getName());
        }
        
//        res = test.listDirectory("20130730/153626_проверить_кириллицы/");
        
//        InputStream stream = test.getClient().retrieveFileStream("/20130730/153626_проверить_кириллицы/Output.txt");
//        InputStream stream = test.getClient().retrieveFileStream("/20130730/162556_pp/Output.txt");
        
//        System.out.println(stream);
    }
}
