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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: zp
 * 24/05/2016
 */
package pt.lsts.neptus.wizard;

import java.awt.BorderLayout;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

import pt.lsts.neptus.gui.PropertiesProvider;
import pt.lsts.neptus.gui.PropertiesTable;
import pt.lsts.neptus.plugins.PluginUtils;

/**
 * @author zp
 *
 */
public class  PojoPropertiesPage<T> extends WizardPage<T> {

    private static final long serialVersionUID = 8722983211665068873L;

    T pojo;
    PropertiesTable table = new PropertiesTable();
    
    public PojoPropertiesPage(final T pojo) {
        this.pojo = pojo;
        setLayout(new BorderLayout());
        
        PropertiesProvider provider = new PropertiesProvider() {
            
            @Override
            public void setProperties(Property[] properties) {
                PluginUtils.setPluginProperties(pojo, properties);                
            }
            
            @Override
            public String[] getPropertiesErrors(Property[] properties) {
                return null;
            }
            
            @Override
            public String getPropertiesDialogTitle() {
                return PluginUtils.getPluginName(pojo.getClass())+" properties";
            }
            
            @Override
            public DefaultProperty[] getProperties() {
                return PluginUtils.getPluginProperties(pojo);
            }
        };
        
        table.editProperties(provider);
        add(table, BorderLayout.CENTER);        
    }
    
    @Override
    public T getSelection() throws InvalidUserInputException {
        return pojo;
    }

    @Override
    public String getTitle() {
        return PluginUtils.getPluginName(pojo.getClass()) + " properties";
    }

}
