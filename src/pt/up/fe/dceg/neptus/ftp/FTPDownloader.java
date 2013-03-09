/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by jqcorreia
 * Dec 18, 2012
 */
package pt.up.fe.dceg.neptus.ftp;

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.EventObject;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellEditor;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import net.miginfocom.swing.MigLayout;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPFile;

/**
 * @author jqcorreia
 * 
 */
public class FTPDownloader extends JDialog {
    private static final long serialVersionUID = 1L;

    int port = 21;
    FTPClient client;
    String system;
    DefaultMutableTreeNode root = new DefaultMutableTreeNode(new FTPProgressPanel(new FTPFile(), "/", null));
    DefaultTreeModel model = new DefaultTreeModel(root);
    JTree tree = new JTree(model);

    String basePath = "/home/jqcorreia/temp/";
    
    MouseAdapter mouseAdapter = new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
            if(e.getButton()==MouseEvent.BUTTON1) {
                
                if(e.getClickCount()==1) {
                    // Single Click
                    // TODO
                    System.out.println(tree.getSelectionCount());
                }
                if(e.getClickCount()==2){ 
                    // Double Click
                    TreePath path = tree.getPathForLocation(e.getX(), e.getY());

                    // Only proccess inside items area
                    if (path != null) {
                        DefaultMutableTreeNode n = (DefaultMutableTreeNode) path.getLastPathComponent();
                        
                        FTPProgressPanel fpanel = (FTPProgressPanel) n.getUserObject();
                        String s = fpanel.getPath();
                        if(fpanel.getFile().isDirectory()) {
                            System.out.println("walk into " + s + " ");
                            walk(s + "/",n);
                        }
                        else {
                            fpanel.download(basePath, client);
                        }
                    }
                }
            }
            // Let the original event go the UI Thread
            super.mouseClicked(e);
        }
    };

    public FTPDownloader(String host, int port, String system) {
        this.client = new FTPClient();
        this.system = system;
        this.port = port;

        FTPClientConfig config = new FTPClientConfig(FTPClientConfig.SYST_UNIX);

        client.configure(config);
        try {
            client.connect(host, port);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        
        walk("/", root);
        model.nodeStructureChanged(root);
        buildDialog();
        setVisible(true);
        
    }

    private void buildDialog() {
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.out.println("Closing downloader");
                disconnect();
                dispose();
            }
        });
        
        tree.setCellRenderer(new FTPTreeRenderer());
        tree.addMouseListener(mouseAdapter);
        tree.setEditable(true);
        tree.setCellEditor(new MyTreeCellEditor(tree, (DefaultTreeCellRenderer) tree.getCellRenderer()));
            
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        setLayout(new MigLayout());
        setSize(400, 400);
        
        add(new JScrollPane(tree), "w 100%, wrap");
        add(new JButton("Download"));
    }

    public FTPFile[] getFileList(String path) {
        try {
            String oldPath = client.printWorkingDirectory();
            client.changeWorkingDirectory(path);
            FTPFile[] list = client.listFiles();
            client.changeWorkingDirectory(oldPath);
            return list;
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void walk(String path, DefaultMutableTreeNode node) {
        try {
            client.changeWorkingDirectory(path);
            for(FTPFile f : client.listFiles()) {
                DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(new FTPProgressPanel(f, path+"/"+f.getName(), node));
                node.add(newNode);
            }
            model.nodeStructureChanged(node);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        try {
            client.disconnect();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Needed to be able to click on the buttons.
    class MyTreeCellEditor extends DefaultTreeCellEditor
    {
        public MyTreeCellEditor ( JTree tree, DefaultTreeCellRenderer renderer )
        {
            super(tree, renderer);
        }
 
        public Component getTreeCellEditorComponent ( JTree tree, Object value, boolean isSelected, boolean expanded, boolean leaf, int row )
        {
            return renderer.getTreeCellRendererComponent(tree, value, true, expanded, leaf, row, true);
        }
        public boolean isCellEditable ( EventObject anEvent )
        {
            return true;
        }
    }
    
    public static void main(String[] args) {
        new FTPDownloader("localhost", 21, "lauv-seacon-3");
    }
}
