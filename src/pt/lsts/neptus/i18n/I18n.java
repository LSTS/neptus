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
 * Author: José Pinto
 * Aug 8, 2012
 */
package pt.lsts.neptus.i18n;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.util.conf.GeneralPreferences;


/**
 * <p>This class can be used to localize Strings in the application. Localized Strings are stored
 * inside .properties files (sets of KEY=Localized phrase) inside a localization folder.
 * Localization folders have several .properties files with filenames corresponding to classes that
 * have localization needs. The file 'global.properties' is a special properties file that will always be looked at
 * before going into the class-specific properties file.</p>
 * <p>
 * To start using localization, you simply need to start calling the static methods {@link #I18n.text(String, String)} and
 * {@link #I18n.textf(String, String, String...)}. The corresponding .properties files together with english-default phrases 
 * will automatically be created.
 * </p>
 * @author zp
 * @author pdias
 */
public class I18n {
    public static final String I18N_BASE_LOCALIZATION = "conf/i18n/";

    protected String defaultLanguage = "en_US";
    protected String language = defaultLanguage;

    protected static Pattern substitutionStringPattern = Pattern.compile("(%[\\w]+)");
    protected static I18n instance = null;
    protected static String appendedText = "";
    
    public static Pattern localeStringPattern = Pattern.compile("([a-z]{2})(_[A-Z]{2})?");
    
    protected PoFile po = null;
    
    private I18n(String language) {
        this.language = language;
        init();
    }

    protected void init() {
        NeptusLog.pub().info("I18n (trying): " + language);
        File localizationDir = new File(I18N_BASE_LOCALIZATION + language);
        
        Matcher m = localeStringPattern.matcher(language);
        
        boolean found = false;
        if (new File(localizationDir, "neptus.po").canRead()) {
            found = true;
        }
        else if (new File("../" + I18N_BASE_LOCALIZATION + language, "neptus.po").canRead()) {
            found = true;
            localizationDir = new File("../" + I18N_BASE_LOCALIZATION + language);
        }
        else if (m.find() && (new File(I18N_BASE_LOCALIZATION + m.group(1), "neptus.po").canRead() ||
                new File("../" + I18N_BASE_LOCALIZATION + m.group(1), "neptus.po").canRead())) {
            found = true;
            language = m.group(1);
            localizationDir = new File(I18N_BASE_LOCALIZATION + language);
            if (!localizationDir.exists())
                localizationDir = new File("../" + I18N_BASE_LOCALIZATION + language);
            GeneralPreferences.language = language;
        }
        else {
            language = defaultLanguage;
            localizationDir = new File(I18N_BASE_LOCALIZATION + language);
            GeneralPreferences.language = language;
        }

        NeptusLog.pub().info("I18n (" + (found ? "found" : "using") + "):  " + language);
        if (new File(localizationDir, "neptus.po").canRead()) {
            po = new PoFile();
            try {
                po.load(new File(I18N_BASE_LOCALIZATION + language+"/neptus.po"));
            }
            catch (Exception e) {
                e.printStackTrace();
                po = null;
            }
        }
    }
    
    protected static I18n getInstance() {
        if (instance == null) {
            String language = GeneralPreferences.language; // GeneralPreferences.getProperty("language");
            if (language == null)
                language = "en";
            instance = new I18n(language);
            instance.applyI18nJavaStdComponentsOverrides();
        }
        return instance;
    }
    
    protected String localize(String key, String englishDefault) {
        String localized = englishDefault;
        
        if (po != null && po.getTranslation(key) != null)
            localized = po.getTranslation(key);
        
        return appendedText + localized;
    }
    
    protected String localizeAdvanced(String key, String englishDefault, Class<?> requester) {
        String localized = englishDefault;
        
        String requestingClass = requester.getName();
        
        // fix for anonymous inner classes
        if (requestingClass.contains("$")) {
            requestingClass = requestingClass.substring(0, requestingClass.indexOf("$"));
        }

        //FIXME
        
        return appendedText + localized;
    }

    /**
     * Overwrite some of the JDK Swing Components to the language of choice 
     */
    private void applyI18nJavaStdComponentsOverrides() {
        UIDefaults defaults = UIManager.getDefaults();
        defaults.put("Label.foreground", Color.black);
        defaults.put("Label.disabledForeground", new Color(51, 51, 51));
        defaults.put("TextArea.foreground", Color.black);
        defaults.put("TextArea.inactiveForeground", new Color(51, 51, 51));
    
        // Please keep the call to I18n.text() like this so the gettext can properly extract them
        UIManager.put("OptionPane.yesButtonText", I18n.text("Yes"));
        UIManager.put("OptionPane.noButtonText", I18n.text("No"));
        UIManager.put("OptionPane.okButtonText", I18n.text("OK"));
        UIManager.put("OptionPane.cancelButtonText", I18n.text("Cancel"));
        UIManager.put("OptionPane.titleText", I18n.text("Select an Option"));
        UIManager.put("OptionPane.inputDialogTitle", I18n.text("Input"));
        UIManager.put("ProgressMonitor.progressText", I18n.text("Progress..."));
        
        UIManager.put("FileChooser.acceptAllFileFilterText", I18n.text("All Files"));
        UIManager.put("FileChooser.lookInLabelText", I18n.text("Look In:"));
        UIManager.put("FileChooser.cancelButtonText", I18n.text("Cancel"));
        UIManager.put("FileChooser.cancelButtonToolTipText", I18n.text("Cancel"));
        UIManager.put("FileChooser.openButtonText", I18n.text("Open"));
        UIManager.put("FileChooser.openButtonToolTipText", I18n.text("Open"));
        UIManager.put("FileChooser.saveButtonText", I18n.text("Save"));
        UIManager.put("FileChooser.saveButtonToolTipText", I18n.text("Save"));
        UIManager.put("FileChooser.updateButtonText", I18n.text("Update"));
        UIManager.put("FileChooser.updateButtonToolTipText", I18n.text("Update"));
        UIManager.put("FileChooser.helpButtonText", I18n.text("Help"));
        UIManager.put("FileChooser.helpButtonToolTipText", I18n.text("Help"));
        UIManager.put("FileChooser.newFolderToolTipText", I18n.text("New Folder"));
        UIManager.put("FileChooser.newFileToolTipText", I18n.text("New File")); //???
        UIManager.put("FileChooser.filesOfTypeLabelText", I18n.text("Files of Type:"));
        UIManager.put("FileChooser.fileNameLabelText", I18n.text("File Name:"));
        UIManager.put("FileChooser.folderNameLabelText", I18n.text("Folder Name:"));
        UIManager.put("FileChooser.listViewButtonToolTipText", I18n.text("List")); 
        UIManager.put("FileChooser.listViewButtonAccessibleName", I18n.text("List")); 
        UIManager.put("FileChooser.detailsViewButtonToolTipText", I18n.text("Details"));
        UIManager.put("FileChooser.detailsViewButtonAccessibleName", I18n.text("Details"));
        UIManager.put("FileChooser.upFolderToolTipText", I18n.text("Up One Level")); 
        UIManager.put("FileChooser.upFolderAccessibleName", I18n.text("Up One Level")); 
        UIManager.put("FileChooser.homeFolderToolTipText", I18n.textc("Home", "File system Home folder"));
        UIManager.put("FileChooser.homeFolderAccessibleName", I18n.textc("Home", "File system Home folder"));
        UIManager.put("FileChooser.desktopFolderToolTipText", I18n.text("Desktop")); //???
        UIManager.put("FileChooser.desktopFolderAccessibleName", I18n.text("Desktop")); //???
        UIManager.put("FileChooser.fileNameHeaderText", I18n.text("Name")); 
        UIManager.put("FileChooser.fileSizeHeaderText", I18n.text("Size")); 
        UIManager.put("FileChooser.fileTypeHeaderText", I18n.text("Type")); 
        UIManager.put("FileChooser.fileDateHeaderText", I18n.text("Modified")); 
        UIManager.put("FileChooser.fileAttrHeaderText", I18n.text("Attributes")); 
        UIManager.put("FileChooser.openDialogTitleText", I18n.text("Open"));
        //UIManager.put("FileChooser.readOnly", Boolean.TRUE);
        
        UIManager.put("PropertySheetPanel.okButtonText", I18n.text("OK"));
        UIManager.put("PropertySheetPanel.cancelButtonText", I18n.text("Cancel"));
        
        UIManager.put("ColorChooser.titleText", I18n.text("Pick a Color"));
        UIManager.put("ColorChooser.okText", I18n.text("OK"));
        UIManager.put("ColorChooser.cancelText", I18n.text("Cancel"));
        UIManager.put("ColorChooser.resetText", I18n.text("Reset"));
        UIManager.put("ColorChooser.sampleText", I18n.text("Sample Text"));
        UIManager.put("ColorChooser.previewText", I18n.text("Preview"));
        
        UIManager.put("ColorChooser.background", ColorUIResource.darkGray);

        UIManager.put("ColorChooser.swatchesNameText", I18n.text("Swatches"));
        UIManager.put("ColorChooser.hsvNameText", I18n.textc("HSV", "Color scheme"));
        UIManager.put("ColorChooser.hslNameText", I18n.textc("HSL", "Color scheme"));
        UIManager.put("ColorChooser.rgbNameText", I18n.textc("RGB", "Color scheme"));
        UIManager.put("ColorChooser.cmykNameText", I18n.textc("CMYK", "Color scheme"));
        
        UIManager.put("ColorChooser.swatchesRecentText", I18n.text("Recent:"));

        UIManager.put("ColorChooser.hsvHueText", I18n.text("Hue"));
        UIManager.put("ColorChooser.hsvSaturationText", I18n.text("Saturation"));
        UIManager.put("ColorChooser.hsvValueText", I18n.text("Value"));
        UIManager.put("ColorChooser.hsvTransparencyText", I18n.text("Transparency"));
        
        UIManager.put("ColorChooser.hslHueText", I18n.text("Hue"));
        UIManager.put("ColorChooser.hslSaturationText", I18n.text("Saturation"));
        UIManager.put("ColorChooser.hslLightnessText", I18n.text("Lightness"));
        UIManager.put("ColorChooser.hslTransparencyText", I18n.text("Transparency"));
        
        UIManager.put("ColorChooser.rgbRedText", I18n.text("Red"));
        UIManager.put("ColorChooser.rgbGreenText", I18n.text("Green"));
        UIManager.put("ColorChooser.rgbBlueText", I18n.text("Blue"));
        UIManager.put("ColorChooser.rgbAlphaText", I18n.text("Alpha"));
            // Not working
        UIManager.put("ColorChooser.colorCodeText", I18n.text("Color Code"));
        
        UIManager.put("ColorChooser.cmykCyanText", I18n.text("Cyan"));
        UIManager.put("ColorChooser.cmykMagentaText", I18n.text("Magenta"));
        UIManager.put("ColorChooser.cmykYellowText", I18n.text("Yellow"));
        UIManager.put("ColorChooser.cmykBlackText", I18n.text("Black"));
        UIManager.put("ColorChooser.cmykAlphaText", I18n.text("Alpha"));
        
        UIManager.put("AbstractUndoableEdit.redoText", I18n.text("Redo"));
        UIManager.put("AbstractUndoableEdit.undoText", I18n.text("Undo"));
    }

    public static String normalize(String text) {
//        text = text.toUpperCase();
//        text = text.replaceAll("[^\\w]", "_");
//        if (text.length() > 60)
//            text = text.substring(0, 60);
        
        return text;
    }
    
    /**
     * This method allows localization of a (simple) phrase whose key can be automatically generated.<br>
     * Automatic generation will replace any now-word characters by '_'. Example: 
     * <pre>"This is a just a (very) simple test!" -> "THIS_IS_JUST_A__VERY__SIMPLE_TEST_" 
     * @param englishDefault The (simple) text to be localized. 
     * @return The localized text
     */
    public static String text(String englishDefault) {
        return getInstance().localize(/* normalize( */englishDefault/* ) */, englishDefault);
    }

    /**
     * Same as {@link #text(String)} but with an extra id containing a context for the translation. E.g. "verb", "referring to a system"
     * @param englishDefault The (simple) text to be localized. 
     * @param context The context for the translation
     * @return
     */
    public static String textc(String englishDefault, String context) {
        return getInstance().localize(normalize(englishDefault) + normalize(context), englishDefault);
    }

    /**
     * You should have at least one parameter... are you forgetting them?
     * @see #textf(String, Object...)
     */
    @Deprecated
    public static String textf(String englishDefault) {
        return null;
    }

    /**
     * You should have at least one parameter... are you forgetting them?
     * @see #textfc(String, Object...)
     */
    @Deprecated
    public static String textfc(String englishDefault, String context) {
        return null;
    }

    /**
     * This method provides parametrized localization. Examples:<br>
     * <pre>String text1 = I18n.textf("FILE_NOT_READABLE", "The file '%filename' is not readable", "/home/zp/file.txt"); //results in "The file '/home/zp/file.txt' is not readable" for english default
     * String text2 = I18n.textf("Loading is %percent% done", 10.4); //results in "Loading is 10.4% done" for english default
     * String text3 = I18n.textf("%s1 plus %s2 equals %s3", s1, s2, s1+s2);
     * </pre>
     * @param englishDefault The default text in american english
     * @param parameters A list of Objects that will be replaced in the localized text (method to toString() is called on each one)
     * @return The localized text with parameters replaced by values
     */
    public static String textf(String englishDefault, Object... parameters) {
        return textfWorker(englishDefault, "", parameters);
    }

    /**
     * Same as {@link #textf(String)} but with an extra id containing a context. E.g. "verb", "referring to a system"
     * @param englishDefault The default text in american english
     * @param context The context for the translation
     * @param parameters A list of Objects that will be replaced in the localized text (method to toString() is called on each one)
     * @return
     */
    public static String textfc(String englishDefault, String context, Object... parameters) {
        return textfWorker(englishDefault, context, parameters);
    }
    
    /**
     * This method marks a string to be translated
     * @param englishDefault
     * @return
     */
    public static String textmark(String englishDefault) {
        return englishDefault;
    }
    
    /**
     * This method marks a string to be translated with context
     * @param englishDefault
     * @param context
     * @return
     */
    public static String textmarkc(String englishDefault, String context) {
        return englishDefault;
    }

    private static String textfWorker(String englishDefault, String context, Object... parameters) {
      
        String text = getInstance().localize(
                normalize(englishDefault) + (context != null && context.length() > 0 ? normalize(context) : ""),
                englishDefault);
        
        Matcher matcher = substitutionStringPattern.matcher(englishDefault);
        
        int i = 0;
        LinkedHashMap<String, String> replaces = new LinkedHashMap<String, String>();
        while (matcher.find() && i < parameters.length ) {
            replaces.put(matcher.group(1), parameters[i++].toString());
        }
        for (String k : replaces.keySet()) {
            text = text.replaceAll(k, Matcher.quoteReplacement(replaces.get(k)));
        }

        return text;
    }

    /**
     * Localize the String by strictly setting the name of the class that is requesting the localization. <br>
     * Usage of this method is not recommended unless you know exactly what you're doing...
     * @param key The key that will be common to all languages. Usage of non-ascii characters is disencouraged
     * @param englishDefault Default english text
     * @param requester The class that will be used for looking up the text 
     * @return The localized phrase. In case the phrase was not found the default english text will be returned and a 
     * new entry will be added to the corresponding localization file.
     */
    public static String textAdvanced(String key, String englishDefault, Class<?> requester) {
        return getInstance().localizeAdvanced(key, englishDefault, requester);        
    }

    /**
     * Change the text that gets appended to all localized Strings (used only in debug)
     * @param prefix The prefix String to be appended to all Strings
     */
    public static void setDebugPrefix(String prefix) {
        appendedText = prefix;
    }
    
    
    public static void main(String[] args) {
        UIDefaults uiDefaults = UIManager.getDefaults();
        List<String> lst = new ArrayList<>();
        Enumeration<Object> enum1 = uiDefaults.keys();
        while (enum1.hasMoreElements()) {
            lst.add(enum1.nextElement().toString());
        } 

        Collections.sort(lst);
        for (Object key : lst) {
          Object val = uiDefaults.get(key);
          System.out.println("[" + key.toString() + "]:[" +
             (null != val ? val.toString() : "(null)") +
             "]");
        }
        
        System.out.println(I18n.textf("File written to %file %t.", new File(".").getAbsolutePath(), "\u00B5"));
    }
}
