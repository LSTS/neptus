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
 * Author: José Pinto
 * 2007/09/25
 */
package pt.lsts.neptus.mra;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JPopupMenu;
import javax.swing.ToolTipManager;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.gui.BlockingGlassPane;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.loader.NeptusMain;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.conf.ConfigFetch;
import pt.lsts.neptus.util.llf.LsfReportProperties;

/**
 * Neptus MRA main class
 * - sets up MRA frame
 * 
 * @author ZP
 * @author pdias (LSF)
 * @author jqcorreia
 * @author hfq
 */
@SuppressWarnings("serial")
public class NeptusMRA extends JFrame {
    private static final String MRA_TITLE = I18n.text("Neptus Mission Review And Analysis");

    private MRAProperties mraProperties = new MRAProperties();
    private LsfReportProperties reportProperties = new LsfReportProperties();
    private MRAPanel mraPanel = null;
    private BlockingGlassPane bgp = new BlockingGlassPane(400, true);
    private MRAMenuBar mraMenuBar;
    private MRAFilesHandler mraFilesHandler;

    public static boolean vtkEnabled = true;

    /**
     * Constructor
     */
    public NeptusMRA() {
        super(MRA_TITLE);
        try {
            PluginUtils.loadProperties(ConfigFetch.getConfFolder() + "/mra.properties", getMraProperties());
        }
        catch (Exception e) {
            NeptusLog.pub().error(I18n.text("Not possible to open")
                    + " \"" + ConfigFetch.getConfFolder() + "/mra.properties\"");
        }
        JPopupMenu.setDefaultLightWeightPopupEnabled(false);
        ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);

        setSize(1200, 700);

        setIconImages(ConfigFetch.getIconImagesForFrames());

        GuiUtils.centerOnScreen(this);

        mraMenuBar = new MRAMenuBar(this);
        setMraFilesHandler(new MRAFilesHandler(this));
        mraMenuBar.createMRAMenuBar();
        setJMenuBar(mraMenuBar.getMenuBar());
        setVisible(true);

        JLabel lbl = new JLabel(MRA_TITLE, JLabel.CENTER);
        lbl.setOpaque(false);
        lbl.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        lbl.setFont(new Font("Helvetica", Font.ITALIC, 32));
        lbl.setVerticalTextPosition(JLabel.BOTTOM);
        lbl.setHorizontalTextPosition(JLabel.CENTER);
        lbl.setForeground(new Color(80, 120, 175));
        lbl.revalidate();

        getContentPane().add(lbl);

        addWindowListener(new WindowAdapter() {
            boolean closed = false;

            @Override
            public void windowActivated(WindowEvent e) {
                super.windowActivated(e);
                ConfigFetch.setSuperParentFrameForced(NeptusMRA.this);
            }

            @Override
            public void windowClosing(WindowEvent e) {
                if (closed)
                    return;
                closed = true;
                NeptusMRA.this.setVisible(false);

                if (getMraPanel() != null)
                    getMraPanel().cleanup();
                setMraPanel(null);

                getMraFilesHandler().abortPendingOpenLogActions();
                NeptusMRA.this.getContentPane().removeAll();
                NeptusMRA.this.dispose();

                ConfigFetch.setSuperParentFrameForced(null);                
            }
        });

        setGlassPane(getBgp());
    }

    /**
     * @return a new NeptusMRA instance
     */
    public static NeptusMRA showApplication() {
        ConfigFetch.initialize();
        GuiUtils.setLookAndFeel();

        return new NeptusMRA();
    }

    public void openLog(String logPath) {
        File logFile = new File(logPath);
        if (logFile.exists())
            getMraFilesHandler().openLog(logFile);
    }
    
    /**
     * @return the JMenu bar from mraMenuBar
     */
    @Override
    public JMenuBar getJMenuBar() {
        return mraMenuBar.getMenuBar();
    }

    /**
     * @return the MRAMenuBar instance
     */
    public MRAMenuBar getMRAMenuBar() {
        return mraMenuBar;
    }

    /**
     * @return the MRAPanel
     */
    protected MRAPanel getMraPanel() {
        return mraPanel;
    }

    /**
     * @param mraPanel
     */
    protected void setMraPanel(MRAPanel mraPanel) {
        this.mraPanel = mraPanel;
    }

    /**
     * @return the bgp
     */
    protected BlockingGlassPane getBgp() {
        return bgp;
    }

    /**
     * @return the mraProperties
     */
    public MRAProperties getMraProperties() {
        return mraProperties;
    }

    /**
     * Set MRA properties
     * @param mraProperties
     */
    protected void setMraProperties(MRAProperties mraProperties) {
        this.mraProperties = mraProperties;
    }

    protected LsfReportProperties getReportProperties() { return reportProperties; }

    protected void setReportProperties(LsfReportProperties reportProperties) { this.reportProperties = reportProperties; }

    /**
     * @return the mraFilesHandler
     */
    public MRAFilesHandler getMraFilesHandler() {
        return mraFilesHandler;
    }

    /**
     * @param mraFilesHandler the mraFilesHandler to set
     */
    public void setMraFilesHandler(MRAFilesHandler mraFilesHandler) {
        this.mraFilesHandler = mraFilesHandler;
    }

    /**
     * Launch MRA standalone app
     * @param args
     */
    public static void main(String[] args) {
        NeptusMain.main(new String[] {"mra"});
    }
}
