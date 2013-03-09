/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by ZP
 * 2009/09/16
 */
package pt.up.fe.dceg.neptus.plugins.containers;

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

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.console.ContainerSubPanel;
import pt.up.fe.dceg.neptus.console.SubPanel;
import pt.up.fe.dceg.neptus.console.plugins.AlarmProviderOld;
import pt.up.fe.dceg.neptus.plugins.ConfigurationListener;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty.DistributionEnum;
import pt.up.fe.dceg.neptus.plugins.update.IPeriodicUpdates;
import pt.up.fe.dceg.neptus.plugins.update.PeriodicUpdatesService;

/**
 * @author ZP
 *
 */
@PluginDescription(name="Console Layout: Tabbed", description="Allows to add various tabs to the console", icon="pt/up/fe/dceg/neptus/plugins/containers/layout.png")
public class TabContainer extends ContainerSubPanel implements ConfigurationListener, IPeriodicUpdates {

    private static final long serialVersionUID = 1L;

    @NeptusProperty(name="Tab names", description="The name to be displayed in the tabs, separated by commas",
            distribution = DistributionEnum.DEVELOPER)
	public String tabNames = "1,2,3,4,5,6,7,8,9";
	
	private LinkedHashMap<SubPanel, ArrayList<AlarmProviderOld>> subPanelList = new LinkedHashMap<SubPanel, ArrayList<AlarmProviderOld>>();
	
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
	
	public SubPanel getSelectedSubPanel() {
		try {
			return (SubPanel)pivot.getComponent(((JTabbedPane)pivot).getSelectedIndex());
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
	public void addSubPanel(SubPanel panel) {
		super.addSubPanel(panel);
		ArrayList<AlarmProviderOld> l = new ArrayList<AlarmProviderOld>();
		for(Component c : panel.getComponents())
		{
		    if(c instanceof AlarmProviderOld)
		    {
		        //System.out.println(c);
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
