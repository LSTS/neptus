/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
 * Author: pdias
 * 17/12/2015
 */
package pt.lsts.neptus.mra;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.util.Date;

import javax.swing.JOptionPane;

import org.apache.commons.io.FileUtils;

import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.bathymetry.TidePredictionFactory;
import pt.lsts.neptus.util.bathymetry.TidePredictionFinder;
import pt.lsts.neptus.util.conf.GeneralPreferences;

/**
 * @author pdias
 *
 */
public class TidesMraLoader {

    /** To avoid instantiation */
    private TidesMraLoader() {
    }

    /**
     * Presents the user with a change tide source option. If the local files are not enough,
     * a Web search will be presented.
     * 
     * @param source
     * @param parent
     */
    public static  void chooseTideSource(IMraLogGroup source, Component parent) {
        String tideInfoPath = TidePredictionFactory.MRA_TIDE_INDICATION_FILE_PATH;
        String noTideStr = TidePredictionFactory.NO_TIDE_STR;
        String usedTideStr = noTideStr;
        
        File tideInfoFx = new File(source.getDir(), TidePredictionFactory.MRA_TIDE_INDICATION_FILE_PATH);
        if (tideInfoFx.exists() && tideInfoFx.canRead()) {
            String hF = FileUtil.getFileAsString(tideInfoFx);
            if (hF != null && !hF.isEmpty()) {
                File fx = new File(TidePredictionFactory.BASE_TIDE_FOLDER_PATH, hF);
                if (fx != null && fx.exists() && fx.canRead())
                    usedTideStr = hF;
            }
        }

        String ret = usedTideStr;
        Date startDate = new Date((long) (source.getLsfIndex().getStartTime() * 1E3));
        Date endDate = new Date((long) (source.getLsfIndex().getEndTime() * 1E3));

        // Choosing tide sources options
        ret = TidePredictionFactory.showTidesSourceChooserGuiPopup(parent, ret, startDate, endDate);

        if (ret == null) {
            FileUtil.saveToFile(new File(source.getDir(), tideInfoPath).getAbsolutePath(), "");
        }
        else {
            String tName = ret;
            String msg = I18n.text("Trying to load tide data");
            // Needed for the TidePredictionFactory.create(..)
            FileUtil.saveToFile(new File(source.getDir(), tideInfoPath).getAbsolutePath(), tName);
            TidePredictionFinder tFinder = TidePredictionFactory.create(source);
            if (tFinder == null) {
                FileUtil.saveToFile(new File(source.getDir(), tideInfoPath).getAbsolutePath(), "");
                msg = I18n.text("Not possible to load tide file");
            }

            if (tFinder == null || !tFinder.contains(startDate) || !tFinder.contains(endDate)) {
                msg = I18n.text("Some tide data missing. Want to update tide predictions?");
                if (tFinder == null)
                    msg = I18n.text("No tide data found. Want to update tide predictions?");

                int updatePredictionsQuestion = GuiUtils.confirmDialog(parent, I18n.text("Tides"), msg);
                switch (updatePredictionsQuestion) {
                    case JOptionPane.YES_OPTION:
                        String harborFetch = TidePredictionFactory.fetchData(parent,
                                tFinder == null ? null : tFinder.getName(), startDate, endDate, true);
                        if (harborFetch != null)
                            FileUtil.saveToFile(new File(source.getDir(), tideInfoPath).getAbsolutePath(),
                                    harborFetch + "." + TidePredictionFactory.defaultTideFormat);
                        break;
                    default:
                        FileUtil.saveToFile(new File(source.getDir(), tideInfoPath).getAbsolutePath(), "");
                        break;
                }
            }
        }
    }

    /**
     * If the file {@link TidePredictionFactory#MRA_TIDE_INDICATION_FILE_PATH} 
     * is not found in the log one will be created with the default from 
     * {@link GeneralPreferences#tidesFile}.getName().
     * 
     * @param source
     */
    public static void setDefaultTideIfNotExisted(IMraLogGroup source) {
        File tideInfoFx = new File(source.getDir(), TidePredictionFactory.MRA_TIDE_INDICATION_FILE_PATH);
        if (!tideInfoFx.exists() || !tideInfoFx.canRead()) {
            // Account for old way of saving tide info to mra folder of the log
            File tideInfoOldFx = new File(source.getDir(), TidePredictionFactory.MRA_TIDE_INDICATION_FILE_PATH_OLD);
            boolean writeDefault = false;
            if (tideInfoOldFx.exists() && tideInfoOldFx.canRead()) {
                try {
                    FileUtils.moveFile(tideInfoOldFx, tideInfoFx);
                }
                catch (IOException e) {
                    e.printStackTrace();
                    writeDefault = true;
                }
            }
            
            if (writeDefault) {
                File defaultTidesSource = GeneralPreferences.tidesFile;
                if (defaultTidesSource != null && defaultTidesSource.exists() && defaultTidesSource.canRead()) {
                    FileUtil.saveToFile(new File(source.getDir(), TidePredictionFactory.MRA_TIDE_INDICATION_FILE_PATH)
                            .getAbsolutePath(), defaultTidesSource.getName());
                }
            }
        }
    }
}
