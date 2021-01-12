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
 * 22/Jan/2005
 */
package pt.lsts.neptus.util.editors;

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
