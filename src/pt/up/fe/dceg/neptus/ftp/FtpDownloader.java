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
    FTPClient client;

    FTPClientConfig conf;

    public FtpDownloader(String host, int port) throws Exception {
        client = new FTPClient();
        conf = new FTPClientConfig(FTPClientConfig.SYST_UNIX);

        client.connect(host, port);
        client.configure(conf);

        client.enterLocalPassiveMode();
        client.login("anonymous", "");

        client.setFileType(FTP.BINARY_FILE_TYPE);
        client.setControlEncoding("UTF-8");
    }

    public void downloadDirectory(String path, String destPath) throws Exception {
        
        System.out.println("Path :" + path);
        System.out.println("DestPath: " + destPath);
        ArrayList<FTPFile> toDoList = new ArrayList<FTPFile>();
        
        client.changeWorkingDirectory(path);
        
        for (FTPFile f : client.listFiles()) {
            if(f.isDirectory()) {
                toDoList.add(f);
            }
            else {
                String filePath =  path + (path.equals("/") ? "" : "/") + f.getName();
                System.out.println("Downloading " + filePath);
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
        
        client.changeWorkingDirectory(path);
        
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
        
        client.changeWorkingDirectory("/");
        
        for (FTPFile f : client.listFiles()) {
            if(f.isDirectory()) {
                client.changeWorkingDirectory("/" + f.getName());
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
        
        try {
            String dest = destPath + fileName;
            boolean b = retrieveFile(client.retrieveFileStream(filePath), new FileOutputStream(new File(dest)));
            System.out.println(dest);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    
    private boolean retrieveFile(InputStream is, OutputStream os) {
                try
                {
                    System.out.println(is);
                    
                    Util.copyStream(is, os, 1024,
                                    CopyStreamEvent.UNKNOWN_STREAM_SIZE, new CopyStreamListener() {
                                        
                                        @Override
                                        public void bytesTransferred(long arg0, int arg1, long arg2) {
                                            System.out.println("1 " + " " + arg0 + " " + arg1 + " " + arg2);
                                        }
                                        
                                        @Override
                                        public void bytesTransferred(CopyStreamEvent arg0) {
                                            System.out.println("2 " + arg0);
                                            
                                        }
                                    },
                                    false);
                }
                catch (IOException e)
                {
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
        LinkedHashMap<String, FTPFile> res = new LinkedHashMap<>();

        test.getClient().setControlEncoding("UTF-8");
        res = test.listDirectory("/");
        
        test.getClient().setRestartOffset(10000000);
        InputStream stream = test.getClient().retrieveFileStream("/20130724/142631_cross_hatch_1h_v2/Data.jsf"); 
        
        System.out.println("skipping");
        System.out.println("end skipping");
        
//        for(String s : res.keySet()) {
//            System.out.println(s);
//        }
//        System.out.println(res.keySet().size());
    }
}
