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
 * 22/Jan/2005
 */
package pt.up.fe.dceg.neptus.util.editors;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTree;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.dom4j.swing.DocumentTreeModel;
import org.dom4j.swing.XMLTableDefinition;
import org.dom4j.swing.XMLTableModel;

/**
 * @author Paulo
 *
 */
public class XMLEditor
{
    DocumentTreeModel docTM;
    
    /**
     * 
     */
    public XMLEditor()
    {
        super();
    }

    public static void main(String[] args) throws DocumentException
    {
        SAXReader reader = new SAXReader();
        Document document = reader.read("neptus-config.xml");
        DocumentTreeModel treeModel = new DocumentTreeModel(document);
        JTree tree = new JTree( treeModel );
        tree.setEditable(true);
        
        JFrame frame = new JFrame( "JTreeDemo: " + document.getName() );
        frame.setSize(300, 300);
        frame.setLocation(100, 100);
        frame.getContentPane().add( new JScrollPane( tree ) );
        frame.validate();
        frame.setVisible(true);
        //this.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
        frame.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
        
        
        reader = new SAXReader();
        XMLTableDefinition definition = new XMLTableDefinition();
        definition.setRowExpression( "/config" );
        definition.addStringColumn( "Name", "name(log-conf-file)" );
        definition.addStringColumn( "Class", "log-conf-file" );
        definition.addStringColumn( "Mapping", "../servlet-mapping[servlet-name=$Name]" );
        document = reader.read( "neptus-config.xml" );
    
        // build table model
        XMLTableModel model = new XMLTableModel( definition, document );
        
        // make the widgets
        JTable table = new JTable( model );
        table.setEditingColumn(1);
        
        JFrame frame1 = new JFrame( "JTableTool: " + document.getName() );
        frame1.setSize(300, 300);
        frame1.setLocation(400, 100);
        frame1.getContentPane().add( new JScrollPane( table ) );
        frame1.validate();
        frame1.setVisible(true);

    }
}
