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
 * 22/Jun/2005
 */
package pt.lsts.neptus.gui;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dialog.ModalityType;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;

import com.l2fprod.common.beans.editor.AbstractPropertyEditor;
import com.l2fprod.common.beans.editor.ComboBoxPropertyEditor;
import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;
import com.l2fprod.common.propertysheet.PropertyEditorRegistry;
import com.l2fprod.common.propertysheet.PropertyRendererRegistry;
import com.l2fprod.common.propertysheet.PropertySheet;
import com.l2fprod.common.propertysheet.PropertySheetDialog;
import com.l2fprod.common.propertysheet.PropertySheetPanel;
import com.l2fprod.common.swing.BannerPanel;
import com.l2fprod.common.swing.renderer.DefaultCellRenderer;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.colormap.ColorMap;
import pt.lsts.neptus.comm.manager.imc.ImcId16;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.gui.editor.BitmaskPropertyEditor;
import pt.lsts.neptus.gui.editor.ColorMapPropertyEditor;
import pt.lsts.neptus.gui.editor.ComboEditor;
import pt.lsts.neptus.gui.editor.EnumeratedPropertyEditor;
import pt.lsts.neptus.gui.editor.FileOnlyPropertyEditor;
import pt.lsts.neptus.gui.editor.ImcId16Editor;
import pt.lsts.neptus.gui.editor.LocationTypePropertyEditor;
import pt.lsts.neptus.gui.editor.NeptusDoubleEditor;
import pt.lsts.neptus.gui.editor.PlanActionsEditor;
import pt.lsts.neptus.gui.editor.RenderSelectionEditor;
import pt.lsts.neptus.gui.editor.RenderType;
import pt.lsts.neptus.gui.editor.Script;
import pt.lsts.neptus.gui.editor.ScriptSelectionEditor;
import pt.lsts.neptus.gui.editor.SpeedEditor;
import pt.lsts.neptus.gui.editor.VehicleSelectionEditor;
import pt.lsts.neptus.gui.editor.ZUnitsEditor;
import pt.lsts.neptus.gui.editor.renderer.ArrayAsStringRenderer;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.messages.Bitmask;
import pt.lsts.neptus.messages.Enumerated;
import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.mp.ManeuverLocationEditor;
import pt.lsts.neptus.mp.SpeedType;
import pt.lsts.neptus.mp.actions.PlanActions;
import pt.lsts.neptus.types.coord.CoordinateUtil;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.conf.ConfigFetch;
import pt.lsts.neptus.util.credentials.Credentials;
import pt.lsts.neptus.util.credentials.CredentialsEditor;

/**
 * This provides a simple way of editing any properties that are provide
 *  by PropertiesProvider object
 * @author ZP
 */
public class PropertiesEditor {
	
	static PropertyEditorRegistry per = null;
	static PropertyRendererRegistry prr = null;
	
	/**
	 * This method shows a dialog with all the properties available in the properties provider
	 * If the user presses the OK button, the new properties are sent back to the properties provider
	 * @param provider The PropertiesProvider that is to be configured
     * @return <b>true</b> if cancelled or <b>false</b> otherwise.
     */
	public static boolean editProperties(PropertiesProvider provider, boolean editable) {
        return editPropertiesWorker(provider, null, editable);
	}
	
    public static <P extends Window> boolean editProperties(PropertiesProvider provider, P parent, boolean editable) {
    	 return editPropertiesWorker(provider, parent, editable);
    }
    
    public static boolean createAggregatedPropertiesDialog(ConsoleLayout console, boolean editable) {

        final PropertySheetPanel psp = new PropertySheetPanel();
        psp.setEditorFactory(getPropertyEditorRegistry());
        psp.setRendererFactory(getPropertyRendererRegistry());
        psp.setMode(PropertySheet.VIEW_AS_CATEGORIES);
        psp.setSortingCategories(true);
        psp.setDescriptionVisible(true);
        psp.setToolBarVisible(false);

        return true;

    }
    
    protected static String normalize(String text) {
        text = text.toUpperCase();
        text = text.replaceAll("[^\\w]", "_");
        return text;
    }
    
    public static void localizeProperties(Collection<DefaultProperty> original, Vector<DefaultProperty> result) {
        result.clear();
        for (DefaultProperty dp : original) {
//            DefaultProperty newProp = new DefaultProperty();
            dp.setName(dp.getName());
            dp.setDisplayName(I18n.text(dp.getDisplayName()));
            if (dp.getCategory() != null)
                dp.setCategory(I18n.text(dp.getCategory()));
            if (dp.getShortDescription() != null)
                dp.setShortDescription(I18n.text(dp.getShortDescription()));
//            newProp.setEditable(dp.isEditable());
//            newProp.setType(dp.getType());
//            newProp.setValue(dp.getValue());
//            newProp.setParentProperty(dp.getParentProperty());
//            
//            try {
//                PropertyEditor editorOgnl;
//                try {
//                    editorOgnl = PropertiesEditor.getPropertyEditorRegistry().getEditor(dp);
//                    if (editorOgnl != null)
//                        PropertiesEditor.getPropertyEditorRegistry().registerEditor(newProp, editorOgnl);
//                }
//                catch (Exception e) {
//                    // TODO Auto-generated catch block
//                    e.printStackTrace();
//                }
//            }
//            catch (Exception e) {
//                e.printStackTrace();
//            }
            
            result.add(dp);
        }        
    }
    
    /**
     * @return <b>true</b> if cancelled or <b>false</b> otherwise.
     */
    private static boolean editPropertiesWorker(PropertiesProvider provider, Window parent, boolean editable) {
        
    	boolean canceled = false;
    	
    	final PropertySheetPanel psp = new PropertySheetPanel();
        psp.setEditorFactory(getPropertyEditorRegistry());    
        psp.setRendererFactory(getPropertyRendererRegistry());    
        psp.setMode(PropertySheet.VIEW_AS_CATEGORIES);
        psp.setToolBarVisible(false);
        
        psp.setSortingCategories(true);
        psp.setCategorySortingComparator(new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                String advancedI18nText = I18n.textc("Advance",
                        "Properties category dubbed Advance, normally category is "
                                + "\"Advance\" or STARTS WITH \"Advance - \"");

                if (o1.startsWith("Advance") || o2.startsWith("Advance")) {
                    if (o1.startsWith("Advance") && o2.startsWith("Advance"))
                        return o1.compareTo(o2);
                    if (!o1.equalsIgnoreCase(o2))
                        return o1.startsWith("Advance") ? 1 : -1;
                    else
                        return o1.compareTo(o2);
                }
                else {
//                    return o1.compareTo(o2);
                    if (o1.startsWith(advancedI18nText) || o2.startsWith(advancedI18nText)) {
                        if (o1.startsWith(advancedI18nText) && o2.startsWith(advancedI18nText))
                            return o1.compareTo(o2);
                        if (!o1.equalsIgnoreCase(o2))
                            return o1.startsWith(advancedI18nText) ? 1 : -1;
                        else
                            return o1.compareTo(o2);
                    }
                    else
                        return o1.compareTo(o2);
                }
            }
        });
        
        psp.setDescriptionVisible(true);
        DefaultProperty[] properties = provider.getProperties();
        Vector<DefaultProperty> result = new Vector<DefaultProperty>();
        localizeProperties(Arrays.asList(properties), result);
        DefaultProperty[] propertiesLocalized = result.toArray(new DefaultProperty[0]);
//        DefaultProperty[] propertiesLocalized = properties;
        
        if (!editable) {
        	for (DefaultProperty p : propertiesLocalized) {
        		p.setEditable(false);
        	}
        }
        if (propertiesLocalized != null) {
        	
            for (int i = 0; i < propertiesLocalized.length; i++) 
                psp.addProperty(propertiesLocalized[i]);
        }
        final PropertySheetDialog propertySheetDialog = createWindow(parent, editable, psp,
                provider.getPropertiesDialogTitle());

        LinkedHashMap<String, DefaultProperty> original = createHashMap(properties);
        
        boolean end = false;
        while (!end) {
            if (!propertySheetDialog.ask()) {
                end = true;
                canceled = true;
                break;
            }
            Property[] newProps = psp.getProperties();
            DefaultProperty[] propsUnlocalized = unlocalizeProps(original, newProps);
            String[] errors = provider.getPropertiesErrors(propsUnlocalized);
            if (errors != null && errors.length > 0) {
                printErrors(parent, errors);
            }
            else {
                end = true;
                try {
                    provider.setProperties(propsUnlocalized);
                }
                catch (Exception e) {
                    NeptusLog.pub().error(e, e);
                }
            }
        }
        return canceled;
    }

    public static PropertySheetDialog createWindow(Window parent, boolean editable, final PropertySheetPanel psp,
            String title) {
        return createWindow(parent, editable, psp, title, title);
    }

    public static PropertySheetDialog createWindow(Window parent, boolean editable, final PropertySheetPanel psp,
            String title, String bannerTitle) {
        final PropertySheetDialog propertySheetDialog;
        if (parent instanceof Dialog) {
            Dialog pDialog = (Dialog) parent;
            propertySheetDialog = new PropertySheetDialog(pDialog);
        }
        else if (parent instanceof Frame) {
            Frame pFrame = (Frame) parent;
            propertySheetDialog = new PropertySheetDialog(pFrame);
        }
        else {
            propertySheetDialog = new PropertySheetDialog() {
                private static final long serialVersionUID = 1L;

                @Override
                public void ok() {
                    if (psp.getTable().getEditorComponent() != null)
                        psp.getTable().commitEditing();
                    super.ok();
                };
            };
        }
        if (editable) {
            propertySheetDialog.setDialogMode(PropertySheetDialog.OK_CANCEL_DIALOG);
        }
        else {
            propertySheetDialog.setDialogMode(PropertySheetDialog.CLOSE_DIALOG);
        }

        // To I18n PropertySheetDialog our way
        int sb = 1;
        // NeptusLog.pub().info("<###> "+propertySheetDialog.getRootPane().getComponents().length);
        for (Component compL0 : propertySheetDialog.getRootPane().getComponents()) {
            // NeptusLog.pub().info("<###> "+compL0);
            if (compL0 instanceof JLayeredPane) {
                for (Component compL01 : ((JLayeredPane) compL0).getComponents()) {
                    if (!(compL01 instanceof JPanel))
                        continue;
                    for (Component compL1 : ((JPanel) compL01).getComponents()) {
                        if (compL1 instanceof BannerPanel)
                            continue;
                        if (compL1 instanceof JPanel) {
                            for (Component compL2 : ((JPanel) compL1).getComponents()) {
                                for (Component compL3 : ((JPanel) compL2).getComponents()) {
                                    if (compL3 instanceof JButton) {
                                        if (propertySheetDialog.getDialogMode() == PropertySheetDialog.OK_CANCEL_DIALOG && sb == 1) {
                                            ((JButton) compL3).setText(I18n.text("OK"));
                                            sb--;
                                        }
                                        else if (propertySheetDialog.getDialogMode() == PropertySheetDialog.CLOSE_DIALOG || sb == 0) {
                                            ((JButton) compL3)
                                            .setText(propertySheetDialog.getDialogMode() == PropertySheetDialog.CLOSE_DIALOG ? I18n
                                                    .text("Close") : I18n.text("Cancel"));
                                            sb--;
                                            break;
                                        }
                                        if (sb < 0)
                                            break;
                                    }
                                }
                                if (sb < 0)
                                    break;
                            }
                        }
                    }
                }
                
            }
        }

        
        propertySheetDialog.addWindowListener(new WindowAdapter() {

            @Override
            public void windowActivated(WindowEvent e) {
                Window ownerWindow = propertySheetDialog.getOwner();
                if (ownerWindow != null)
                    ConfigFetch.setSuperParentFrameForced(ownerWindow);
            }
        });

        // propertySheetDialog.getBanner().setTitle(provider.getPropertiesDialogTitle());
        // propertySheetDialog.setTitle(provider.getPropertiesDialogTitle());
        if (title != null) {
            propertySheetDialog.setTitle(I18n.text(title));
            propertySheetDialog.getBanner().setTitle(
                    I18n.text(bannerTitle == null || bannerTitle.length() == 0 ? title : bannerTitle));
        }
        propertySheetDialog.setIconImage(ImageUtils.getImage("images/menus/settings.png"));
        propertySheetDialog.getBanner().setIcon(ImageUtils.getIcon("images/settings.png"));
        propertySheetDialog.getContentPane().add(psp);
        propertySheetDialog.pack();

        GuiUtils.centerOnScreen(propertySheetDialog);
        propertySheetDialog.setModalityType(ModalityType.DOCUMENT_MODAL);
        return propertySheetDialog;
    }

    public static LinkedHashMap<String, DefaultProperty> createHashMap(DefaultProperty[] properties) {
        LinkedHashMap<String, DefaultProperty> original = new LinkedHashMap<String, DefaultProperty>();
        for (DefaultProperty p : properties)
            original.put(p.getName(), p);
        return original;
    }

    private static void printErrors(Window parent, String[] errors) {
        boolean hasParentWindow;
        if (parent instanceof Dialog || parent instanceof Frame) {
            hasParentWindow = true;
        }
        else {
            hasParentWindow = false;
        }
        
        
        String errorsString = "<html>" + I18n.text("The following errors were found") + ":<br>";

        int i = 1;

        for (String error : errors) {
            errorsString = errorsString + "<br> &nbsp;" + (i++) + ") " + error;
        }
        errorsString = errorsString + "</html>";
        GuiUtils.errorMessage(hasParentWindow ? parent : ConfigFetch.getSuperParentFrame(),
                I18n.text("Invalid properties"), errorsString);
    }

    public static DefaultProperty[] unlocalizeProps(LinkedHashMap<String, DefaultProperty> original,
            Property[] newProps) {
        DefaultProperty[] propsUnlocalized = new DefaultProperty[newProps.length];
        
        for (int i = 0; i < newProps.length;i++) {
            propsUnlocalized[i] = new DefaultProperty();
            String name = newProps[i].getName();
            
            
            DefaultProperty orig = original.get(name);
            propsUnlocalized[i].setName(name);
            propsUnlocalized[i].setDisplayName(orig.getDisplayName());
            propsUnlocalized[i].setShortDescription(orig.getShortDescription());
            propsUnlocalized[i].setCategory(orig.getCategory());
            propsUnlocalized[i].setType(orig.getType());
            propsUnlocalized[i].setValue(newProps[i].getValue());
            propsUnlocalized[i].setParentProperty(orig.getParentProperty());        	    
        }
        return propsUnlocalized;
    }

    /**
     * Convinience method to create a default property object from various parameters
     * @param propertyName The name of the property
     * @param propertyClass The class of the values of this property
     * @param value The current value for this property
     * @param isEditable Sets whether the property can be edited by the user
     * @return A DefaultProperty object @see DefaultProperty
     */
    public static DefaultProperty getPropertyInstance(String propertyName, Class<?> propertyClass, Object value, boolean isEditable) {
        
    	DefaultProperty property = new DefaultProperty();
        
    	property.setName(propertyName);
        property.setDisplayName(I18n.text(propertyName));
        property.setType(propertyClass);
        property.setValue(value);
        property.setEditable(isEditable);
        
        return property;
    }

	/**
	 * Convinience method to create a default property object from various parameters
	 * @param propertyName The name of the property
     * @param categoryName The category of the property to be created
	 * @param propertyClass The class of the values of this property
	 * @param value The current value for this property
	 * @param isEditable Sets whether the property can be edited by the user
     * @param shortDescription Sets a short description text.
	 * @return A DefaultProperty object @see DefaultProperty
	 */
	public static DefaultProperty getPropertyInstance(String propertyName,
            String categoryName, Class<?> propertyClass, Object value,
            boolean isEditable, String shortDescription)
    {
		DefaultProperty property = getPropertyInstance(propertyName,
                categoryName, propertyClass, value, isEditable);
		property.setShortDescription(shortDescription);
		if (propertyClass.isEnum()) {
		    PropertiesEditor.getPropertyEditorRegistry().registerEditor(property, new ComboEditor<Object>(propertyClass.getEnumConstants()));
		}
		return property;
	}
	
	/**
	 * Convinience method to create a default property object from various parameters
	 * @param propertyName The name of the property
	 * @param propertyClass The class of the values of this property
	 * @param value The current value for this property
	 * @param isEditable Sets whether the property can be edited by the user
	 * @return A DefaultProperty object @see DefaultProperty
	 */
	public static Property[] mergeProperties(PropertiesProvider[] childProviders, Property[] properties) {
		Vector<Property> vec = new Vector<Property>();
		for (Property p : properties) {
			vec.add(p);
		}
		
		for (PropertiesProvider provider: childProviders) {
			for (Property property : provider.getProperties()) {				
				DefaultProperty prop = new DefaultProperty();
				prop.setCategory(provider.getPropertiesDialogTitle());
				prop.setDisplayName(property.getDisplayName());
				prop.setName(property.getName());
				prop.setEditable(property.isEditable());
				prop.setType(property.getType());
				prop.setValue(property.getValue());
				vec.add(prop);
			}
		}
		
		return vec.toArray(new Property[vec.size()]);
		
	}
	
	/**
	 * Convenience method to create a default property object from various parameters
	 * @param propertyName The name of the property
	 * @param categoryName The category of the property to be created
	 * @param propertyClass The class of the values of this property
	 * @param value The current value for this property
	 * @param isEditable Sets whether the property can be edited by the user
	 * @return A DefaultProperty object @see DefaultProperty
	 */
    public static DefaultProperty getPropertyInstance(String propertyName, String categoryName,
            Class<?> propertyClass, Object value, boolean isEditable) {
		DefaultProperty dp = getPropertyInstance(propertyName, propertyClass, value, isEditable);
		dp.setCategory(categoryName);
		return dp;
	}
	
	public static PropertyEditorRegistry getPropertyEditorRegistry() {
		if (per == null) {
			per = new PropertyEditorRegistry();
			per.registerDefaults();
			per.registerEditor(LocationType.class, LocationTypePropertyEditor.class);
			per.registerEditor(VehicleType.class, VehicleSelectionEditor.class);
			per.registerEditor(Script.class, ScriptSelectionEditor.class);
			per.registerEditor(RenderType.class, RenderSelectionEditor.class);
			per.registerEditor(Enumerated.class, EnumeratedPropertyEditor.class);
			per.registerEditor(Bitmask.class, BitmaskPropertyEditor.class);
			per.registerEditor(ColorMap.class, ColorMapPropertyEditor.class);
			per.registerEditor(ImcId16.class, ImcId16Editor.class);
			per.registerEditor(PlanActions.class, PlanActionsEditor.class);
			per.registerEditor(Double.class, NeptusDoubleEditor.class);
			per.registerEditor(Float.class, NeptusDoubleEditor.class);
			per.registerEditor(ManeuverLocation.class, ManeuverLocationEditor.class);
			per.registerEditor(Credentials.class, CredentialsEditor.class);            
            per.registerEditor(SpeedType.class, SpeedEditor.class);
            per.registerEditor(File.class, FileOnlyPropertyEditor.class);
            per.registerEditor(ManeuverLocation.Z_UNITS.class, ZUnitsEditor.class);
		}
		return per;
	}
	
	@SuppressWarnings("serial")
    public static PropertyRendererRegistry getPropertyRendererRegistry() {
	    if (prr == null) {
	        prr = new PropertyRendererRegistry();
	        prr.registerDefaults();
	        prr.registerRenderer(ImcId16.class, new DefaultCellRenderer() {
                {
                    setShowOddAndEvenRows(false);
                }
	            @Override
	            protected String convertToString(Object value) {
	                try {
	                    ImcId16 id = (ImcId16) value;
	                    return id.toPrettyString();
                    }
                    catch (Exception e) {
                        return super.convertToString(value);
                    }
	            }
	        });
            prr.registerRenderer(LocationType.class, new DefaultCellRenderer() {
                {
                    setShowOddAndEvenRows(false);
                }
                private String toolTip = "";
                @Override
                protected String convertToString(Object value) {
                    try {
                        LocationType loc = (LocationType) value;
                        toolTip = loc.toString();
                        setToolTipText(toolTip);
                        // return "<html>" + loc.toString().replaceAll(",\\ ", ",<br>");
                        LocationType sLoc = loc.getNewAbsoluteLatLonDepth();
                        return CoordinateUtil.latitudeAsPrettyString(sLoc.getLatitudeDegs()) + ", "
                                + CoordinateUtil.longitudeAsPrettyString(sLoc.getLongitudeDegs());
                    }
                    catch (Exception e) {
                        return super.convertToString(value);
                    }
                }
            });
            prr.registerRenderer(Double.class, new DefaultCellRenderer() {
                {  setOpaque(false);  }
                protected DecimalFormat format = new DecimalFormat("0.0######");
                @Override
                protected String convertToString(Object value) {
                    return format.format(value);
                }
            });
            prr.registerRenderer(Enum.class, new DefaultCellRenderer() {
                {
                    setOpaque(false);
                }

                @Override
                protected String convertToString(Object value) {
                    return I18n.text(value.toString());
                }
            });

            prr.registerRenderer(String[].class, new ArrayAsStringRenderer());
            prr.registerRenderer(Long[].class, new ArrayAsStringRenderer());
            prr.registerRenderer(long[].class, new ArrayAsStringRenderer());
            prr.registerRenderer(Integer[].class, new ArrayAsStringRenderer());
            prr.registerRenderer(int[].class, new ArrayAsStringRenderer());
            prr.registerRenderer(Short[].class, new ArrayAsStringRenderer());
            prr.registerRenderer(short[].class, new ArrayAsStringRenderer());
            prr.registerRenderer(Double[].class, new ArrayAsStringRenderer());
            prr.registerRenderer(double[].class, new ArrayAsStringRenderer());
            prr.registerRenderer(Float[].class, new ArrayAsStringRenderer());
            prr.registerRenderer(float[].class, new ArrayAsStringRenderer());
            prr.registerRenderer(Boolean[].class, new ArrayAsStringRenderer());
            prr.registerRenderer(boolean[].class, new ArrayAsStringRenderer());
            prr.registerRenderer(Byte[].class, new ArrayAsStringRenderer());
            prr.registerRenderer(byte[].class, new ArrayAsStringRenderer());
            prr.registerRenderer(Character[].class, new ArrayAsStringRenderer());
            prr.registerRenderer(char[].class, new ArrayAsStringRenderer());
        }
	    return prr;
	}
	
	public static DefaultProperty getPropertyInstance(Property p) {
		return getPropertyInstance(p.getName(), p.getClass(), p.getValue(), p.isEditable());
	}
	
	public static AbstractPropertyEditor getComboBoxPropertyEditor(String[] options, String value) {
		ComboBoxPropertyEditor editor = new ComboBoxPropertyEditor();
		editor.setAvailableValues(options);
		editor.setValue(value);
		return editor;
	}	
}
