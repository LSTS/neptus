/*
 * Copyright (c) 2004-2015 Universidade do Porto - Faculdade de Engenharia
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
 * Author: José Pinto
 * 2009/09/16
 */
package pt.lsts.neptus.console.plugins.containers;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.dom4j.Element;

import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ContainerSubPanel;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.plugins.AlarmProviderOld;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.NeptusProperty.DistributionEnum;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.update.IPeriodicUpdates;
import pt.lsts.neptus.plugins.update.PeriodicUpdatesService;

/**
 * @author ZP
 *
 */
@PluginDescription(name="Console Layout: Tabbed", description="Allows to add various tabs to the console", icon="pt/lsts/neptus/plugins/containers/layout.png")
public class TabContainer extends ContainerSubPanel implements ConfigurationListener, IPeriodicUpdates {

    private static final long serialVersionUID = 1L;

    @NeptusProperty(name="Tab names", description="The name to be displayed in the tabs, separated by commas",
            distribution = DistributionEnum.DEVELOPER)
	public String tabNames = "1,2,3,4,5,6,7,8,9";
	
	private LinkedHashMap<ConsolePanel, ArrayList<AlarmProviderOld>> subPanelList = new LinkedHashMap<ConsolePanel, ArrayList<AlarmProviderOld>>();
	
	public enum PlacementEnum {
		Left, Right, Top, Bottom
	}
		
	@NeptusProperty(name="Tab placement", description="Where to place the tabs")
	public PlacementEnum tabPlacement = PlacementEnum.Top;
	
	@NeptusProperty
	public int selectedIndex = 0;
		
	@NeptusProperty
	public String name = "";
	
	
	private JComponent pivot = null;
	
	public ConsolePanel getSelectedSubPanel() {
		try {
			return (ConsolePanel)pivot.getComponent(((JTabbedPane)pivot).getSelectedIndex());
		}
		catch (Exception e) {
		    e.printStackTrace();
			return null;
		}
	}
	
	public TabContainer(ConsoleLayout console) {
		super(console);
		setLayout(new BorderLayout());
		pivot = new JTabbedPane();
		((JTabbedPane)pivot).addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent arg0) {
                JTabbedPane tp = (JTabbedPane)arg0.getSource();
                
                tp.setForegroundAt(tp.getSelectedIndex(), Color.BLACK);
            }
		    
		});
		
		add(pivot, BorderLayout.CENTER);
		
		switch (tabPlacement) {
		case Top:
			((JTabbedPane)pivot).setTabPlacement(JTabbedPane.TOP);	
			break;
		case Bottom:
			((JTabbedPane)pivot).setTabPlacement(JTabbedPane.BOTTOM);	
			break;
		case Left:
			((JTabbedPane)pivot).setTabPlacement(JTabbedPane.LEFT);	
			break;
		case Right:
			((JTabbedPane)pivot).setTabPlacement(JTabbedPane.RIGHT);	
			break;
		default:
			break;
		}
		PeriodicUpdatesService.register(this);
		
		
	}
	
	public void setTabNames() {
		String[] names = tabNames.split(",");
		if (names.length >= ((JTabbedPane)pivot).getTabCount()) {
			for (int i = 0; i < ((JTabbedPane)pivot).getTabCount(); i++ ) {
				((JTabbedPane)pivot).setTitleAt(i, names[i]);					
			}
		}	
	}
	
	@Override
	public void addSubPanel(ConsolePanel panel) {
		super.addSubPanel(panel);
		ArrayList<AlarmProviderOld> l = new ArrayList<AlarmProviderOld>();
		for(Component c : panel.getComponents())
		{
		    if(c instanceof AlarmProviderOld)
		    {
		        //NeptusLog.pub().info("<###> "+c);
		        l.add((AlarmProviderOld)c);
		    }
		}
		subPanelList.put(panel, l);

		setTabNames();
	}
	
	@Override
	public void XML_ChildsRead(Element el) {
		super.XML_ChildsRead(el);
		((JTabbedPane)pivot).addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent arg0) {
				selectedIndex = ((JTabbedPane)pivot).getSelectedIndex();
				//getConsole().setConsoleSaved(false);
			}
		});
		((JTabbedPane)pivot).setSelectedIndex(selectedIndex);
	}
	
	@Override
	public void propertiesChanged() {
		setTabNames();
		
		switch (tabPlacement) {
		case Top:
			((JTabbedPane)pivot).setTabPlacement(JTabbedPane.TOP);	
			break;
		case Bottom:
			((JTabbedPane)pivot).setTabPlacement(JTabbedPane.BOTTOM);	
			break;
		case Left:
			((JTabbedPane)pivot).setTabPlacement(JTabbedPane.LEFT);	
			break;
		case Right:
			((JTabbedPane)pivot).setTabPlacement(JTabbedPane.RIGHT);	
			break;
		default:
			break;
		}		
	}

    @Override
    public long millisBetweenUpdates() {
        return 1000;
    }

    @Override
    public boolean update() {
        JTabbedPane tp = ((JTabbedPane)pivot);
        
        int size=tp.getTabCount();
        for(int i = 0; i < size; i++) {
            Component jc = tp.getComponentAt(i);
            
            if(subPanelList.get(jc).size()!=0) {
                for (AlarmProviderOld ap : subPanelList.get(jc)) {
                    if (ap.getAlarmState() > AlarmProviderOld.LEVEL_0 && i != tp.getSelectedIndex()) {
                        tp.setForegroundAt(i, Color.RED);
                    }
                }
            }
        }
        return !destroyed;
    }
    
    protected boolean destroyed = false;
    
    @Override
    public void clean() {
        super.clean();
        PeriodicUpdatesService.unregister((IPeriodicUpdates)this);
        destroyed = true;
    }
}
