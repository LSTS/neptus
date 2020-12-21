/*
 * Copyright (c) 2004-2016 OceanScan - Marine Systems & Technology Lda.
 * Polo do Mar do UPTEC, Avenida da Liberdade, 4450-718 Matosinhos, Portugal
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
 */

package pt.lsts.neptus.plugins.mjpeg;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import net.miginfocom.swing.MigLayout;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.util.ImageUtils;

/**
 * Simple toolbar.
 *
 * @author Ricardo Martins
 */
public class Toolbar extends JPanel {
    private static final long serialVersionUID = 1L;

    private final JButton saveButton = new JButton();
    private final JButton markerButton = new JButton();
    private final JToggleButton showCaptionButton = new JToggleButton();
    private final JToggleButton showTrackButton = new JToggleButton();

    public Toolbar() {
        initialize();
    }

    public JButton getSaveButton() {
        return saveButton;
    }

    public JButton getMarkButton() {
        return markerButton;
    }

    public JToggleButton getShowCaptionButton() {
        return showCaptionButton;
    }

    public JToggleButton getShowTrackButton() {
        return showTrackButton;
    }

    protected void initialize() {
        showCaptionButton.setSelected(true);
        showCaptionButton.setIcon(new ImageIcon(ImageUtils.getImage("pt/lsts/neptus/plugins/mjpeg/icons/caption.png")));
        showCaptionButton.setToolTipText(I18n.text("Show caption"));

        showTrackButton.setSelected(true);
        showTrackButton.setIcon(new ImageIcon(ImageUtils.getImage("pt/lsts/neptus/plugins/mjpeg/icons/track.png")));
        showTrackButton.setToolTipText(I18n.text("Show track"));

        saveButton.setIcon(new ImageIcon(ImageUtils.getImage("pt/lsts/neptus/plugins/mjpeg/icons/save.png")));
        saveButton.setToolTipText(I18n.text("Save current photo"));

        markerButton.setIcon(new ImageIcon(ImageUtils.getImage("images/buttons/mark.png")));
        markerButton.setToolTipText(I18n.text("Add marker"));

        setLayout(new MigLayout());
        add(showCaptionButton);
        add(showTrackButton);
        add(saveButton);
        add(markerButton);
    }
}
