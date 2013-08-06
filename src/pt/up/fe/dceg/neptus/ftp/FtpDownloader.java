/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
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
 * Author: José Correia
 * Dec 11, 2012
 */
package pt.up.fe.dceg.neptus.ftp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.io.CopyStreamEvent;
import org.apache.commons.net.io.CopyStreamListener;
import org.apache.commons.net.io.Util;

import pt.up.fe.dceg.neptus.NeptusLog;

/**
 * @author jqcorreia
 * 
 */
public class FtpDownloader {
    private FTPClient client;

    private FTPClientConfig conf;

    private String host;
    private int port;
    
    public FtpDownloader(String host, int port) throws Exception {
        this.host = host;
        this.port = port;
        
        renewClient();
    }

    /**
     * @throws SocketException
     * @throws IOException
     */
    public void renewClient() throws SocketException, IOException {
        if (client != null) {
            try {
                if (client.isConnected())
                    client.disconnect();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        client = new FTPClient();
        conf = new FTPClientConfig(FTPClientConfig.SYST_UNIX);

        System.out.println(FtpDownloader.class.getSimpleName() + " :: " + "connecting to " + host + ":" + port);
        client.connect(host, port);
        System.out.println(FtpDownloader.class.getSimpleName() + " :: " + "connected to " + host + ":" + port);
        client.configure(conf);

        client.enterLocalPassiveMode();
        client.login("anonymous", "");

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
    
    public void downloadDirectory(String path, String destPath) throws Exception {
        System.out.println(FtpDownloader.class.getSimpleName() + " :: " + "Path :" + path);
        System.out.println(FtpDownloader.class.getSimpleName() + " :: " + "DestPath: " + destPath);
        ArrayList<FTPFile> toDoList = new ArrayList<FTPFile>();

        if (!client.isConnected()) {
            try {
                renewClient();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        boolean ret = client.changeWorkingDirectory(new String(path.getBytes(), "ISO-8859-1"));
        if (!ret) {
            NeptusLog.pub().warn(
                    FtpDownloader.class.getSimpleName() + " :: Error downloading folder '" + path + "' from " + host);
            return;
        }
        
        for (FTPFile f : client.listFiles()) {
            if(f.isDirectory()) {
                toDoList.add(f);
            }
            else {
                String filePath =  path + (path.equals("/") ? "" : "/") + f.getName();
                System.out.println(FtpDownloader.class.getSimpleName() + " :: " + "Downloading " + filePath);
                downloadFile(filePath, destPath);
            }
        }
        for(FTPFile f : toDoList) {
//            System.out.println(path + f.getName());
            downloadDirectory(path + "/" + f.getName(), destPath);
        }
    }
    
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

        boolean ret = client.changeWorkingDirectory(new String(path.getBytes(), "ISO-8859-1"));
        if (!ret) {
            NeptusLog.pub().warn(
                    FtpDownloader.class.getSimpleName() + " :: Error listing folder '" + path + "' from " + host);
            return finalList;
        }
        
        for (FTPFile f : client.listFiles()) {
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
        
        if (!client.isConnected()) {
            try {
                renewClient();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        boolean ret = client.changeWorkingDirectory("/");
        if (!ret) {
            NeptusLog.pub().warn(
                    FtpDownloader.class.getSimpleName() + " :: Error downloading folder '/' from " + host);
            return list;
        }
        
        for (FTPFile f : client.listFiles()) {
            if(f.isDirectory()) {
                boolean ret1 = client.changeWorkingDirectory("/" + new String(f.getName().getBytes(), "ISO-8859-1"));
                if (!ret1) {
                    NeptusLog.pub().warn(
                            FtpDownloader.class.getSimpleName() + " :: Error listing folder '" + f.getName() + "' from " + host);
                    continue;
                }

                for (FTPFile f2 : client.listFiles()) {
                    list.put(f2, f.getName() + "/" + f2.getName());
                }
            }
        }
        return list;
    }
    /**
     * @param filePath
     */
    private void downloadFile(String filePath, String destPath) {
//        byte buf[] = new byte[8192];
        
        String toks[] = filePath.split("/");
        String fileName = toks[toks.length - 1];

        if (!client.isConnected()) {
            try {
                renewClient();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        try {
            String dest = destPath + fileName;
            boolean ret = retrieveFile(client.retrieveFileStream(new String(filePath.getBytes(), "ISO-8859-1")), new FileOutputStream(new File(dest)));
            if (!ret) {
                NeptusLog.pub().warn(
                        FtpDownloader.class.getSimpleName() + " :: Error downloading file '" + filePath + "' from " + host);
            }

            System.out.println(FtpDownloader.class.getSimpleName() + " :: " + dest);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean retrieveFile(InputStream is, OutputStream os) {
        try {
            System.out.println(FtpDownloader.class.getSimpleName() + " :: " + is);

            Util.copyStream(is, os, 1024, CopyStreamEvent.UNKNOWN_STREAM_SIZE, new CopyStreamListener() {
                @Override
                public void bytesTransferred(long arg0, int arg1, long arg2) {
                    System.out.println(FtpDownloader.class.getSimpleName() + " :: " + "1 " + " " + arg0 + " " + arg1 + " " + arg2);
                }

                @Override
                public void bytesTransferred(CopyStreamEvent arg0) {
                    System.out.println(FtpDownloader.class.getSimpleName() + " :: " + "2 " + arg0);
                }
            }, false);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    public FTPClient getClient() {
        return client;
    }
    
    public void close() throws IOException {
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
