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
 * Author: José Correia
 * Jan 5, 2012
 */
package pt.lsts.neptus.plugins.messages;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import com.google.common.eventbus.Subscribe;

import net.miginfocom.swing.MigLayout;
import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.comm.manager.imc.EntitiesResolver;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.update.IPeriodicUpdates;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;
import pt.lsts.neptus.util.gui.Java2sAutoTextField;

/**
 * 
 * @author jqcorreia
 */
@SuppressWarnings("serial")
@PluginDescription(name = "IMC Inspector", icon = "images/imc.png", documentation = "imc-inspector/imc-inspector.html")
public class IMCInspector extends ConsolePanel implements KeyListener, IPeriodicUpdates {


    private final LinkedHashMap<Integer, InspectorMessagePanel> entities = new LinkedHashMap<Integer, InspectorMessagePanel>();
    // private String messageName;
    private JDialog inspector;
    private final JLabel systemLabel = new JLabel("System: ");
    private final JTextField systemField = new JTextField();

    // private JTextField data = new JTextField();

    private final JLabel msgLabel = new JLabel("Message: ");
    private Java2sAutoTextField msgChoice;
    // private JTextField msgChoice2 = new JTextField();

    private JScrollPane jsp;
    private final JPanel pane = new JPanel(new MigLayout());

    // JSuggestField msgChoice;
    private final Vector<String> possibleValues = new Vector<String>();

    private String messageToListen = "EstimatedState";
    private int systemIdToListen;

    private final ActionListener action = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            changeListenedSystem(getConsole().getMainSystem());
            inspector.setVisible(true);
            msgChoice.requestFocus();
        }
    };

    /**
     * @param console
     */
    public IMCInspector(ConsoleLayout console) {
        super(console);
    }

    private JDialog getInspectorDialog() {
        final JDialog dialog = new JDialog(getConsole()) {

            @Override
            public void setVisible(boolean b) {
                super.setVisible(b);
            }
        };

        JButton btnClose = new JButton(new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                dialog.dispose();
            }
        });
        JButton btnPlot = new JButton(new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                ArrayList<String> msgs = new ArrayList<String>();
                ArrayList<String> vars = new ArrayList<String>();

                for (JTextArea jta : new PlotConfigDialog().getConfig()) {
                    String s[] = jta.getText().split("\\.");

                    if (s == null || s.length != 2) // Dummy fix/safeguard //FIXME jqcorreia
                        continue;

                    msgs.add(s[0]);
                    vars.add(s[1]);
                }

                final ImcChart chart = new ImcChart(systemField.getText(), msgs, vars,
                        EntitiesResolver.getEntities(systemField.getText()));

                JDialog plotDialog = new JDialog(getConsole());
                plotDialog.setSize(640, 480);
                plotDialog.add(chart);
                plotDialog.setVisible(true);
                // dialog.setTitle(msgChoice.getText() + "." + field + " (" + systemField.getText() + ")");
                plotDialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

                plotDialog.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        chart.stop();
                    }
                });

            }
        });

        btnPlot.setText("Plot Data");
        JButton btnPlotXY = new JButton(new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                final ImcChartXY chart = new ImcChartXY(systemField.getText());

                JDialog dialog = new JDialog(getConsole());
                dialog.setTitle(systemField.getText());
                dialog.setSize(640, 480);
                dialog.add(chart);
                dialog.setVisible(true);
                dialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

                dialog.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        chart.stop();
                    }
                });

            }
        });

        btnPlot.setText("Plot Data");
        btnPlotXY.setText("Plot XY Data");
        btnClose.setText("Close");

        dialog.setLayout(new MigLayout());
        dialog.setSize(640, 500);
        dialog.setTitle("IMC Inspector");
        jsp = new JScrollPane(pane);
        jsp.getVerticalScrollBar().setUnitIncrement(10);
        systemField.setText(getConsole().getMainSystem());
        dialog.add(systemLabel);
        dialog.add(systemField, "grow, wrap");
        dialog.add(msgLabel);
        dialog.add(msgChoice, "grow, wrap");

        dialog.add(btnPlot);
        dialog.add(btnPlotXY, "wrap");
        dialog.add(jsp, "w 100%, h 100%, grow, span 2, wrap");
        dialog.add(btnClose);

        msgChoice.addKeyListener(this);
        systemField.addKeyListener(this);
        dialog.addKeyListener(this);
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                
            }
        });

        return dialog;
    }

    @Override
    public void initSubPanel() {

        JMenuItem item = addMenuItem(I18n.text("Advanced")+">"+I18n.text("IMC Inspector"), null, action);
        item.setAccelerator(KeyStroke.getKeyStroke("control B"));
        possibleValues.add("");
        for (String s : IMCDefinition.getInstance().getMessageNames()) {
            possibleValues.add(s);
        }

        msgChoice = new Java2sAutoTextField(possibleValues);

        // Initialize Inspector dialog
        inspector = getInspectorDialog();

        msgChoice.setText("EstimatedState");
    }

//    @Override
//    public void cleanSubPanel() {
//        listener.clean();
//        listener = null;
//    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_B) {
            inspector.setVisible(false);
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getSource() == msgChoice) {
            if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_TAB) {
                messageToListen = msgChoice.getText();
                reset();
            }
        }
        if (e.getSource() == systemField) {
            if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_TAB) {
                changeListenedSystem(systemField.getText());
                reset();
            }
        }

    }

    private void changeListenedSystem(String name) {
        systemField.setText(name);
        systemIdToListen = VehiclesHolder.getVehicleById(name).getImcId().intValue();
    }

    private void reset() {
        entities.clear();
        pane.removeAll();

    }

    class PlotConfigDialog extends JDialog {
        ArrayList<JTextArea> vars = new ArrayList<JTextArea>();
        {
            vars.add(new JTextArea());
            vars.add(new JTextArea());
            vars.add(new JTextArea());
            vars.add(new JTextArea());
            vars.add(new JTextArea());
        }
        JButton btnOk = new JButton(new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        public PlotConfigDialog() {
            super(getConsole(), true);
            setLayout(new MigLayout());
            int i = 1;
            for (JTextArea jta : vars) {

                if (i == 1)
                    jta.setText(msgChoice.getText() + ".");

                add(new JLabel("Var " + i++));
                add(jta, "w 100%, wrap");
            }
            btnOk.setText("Ok");
            add(btnOk);
            setSize(300, 200);
        }

        public ArrayList<JTextArea> getConfig() {
            setVisible(true);
            return vars;
        }

        public ArrayList<JTextArea> getVars() {
            return vars;
        }
    }

    @Override
    public long millisBetweenUpdates() {
        return 100;
    }

    @Override
    public boolean update() {
        if (inspector != null) {
            if (inspector.isVisible()) {
                for (Integer i : entities.keySet()) {
                    entities.get(i).update();
                }
            }
        }
        return true;
    }

    @Subscribe
    public void handleEstimatedState(IMCMessage msg) {
        if (msg.getSrc() == systemIdToListen && msg.getAbbrev().equals(messageToListen)) {
            int srcEnt = msg.getHeader().getInteger("src_ent");

            if (entities.containsKey(srcEnt)) {
                entities.get(srcEnt).setMessage(msg);
            }
            else {
                String title = "";
                if (srcEnt != 0xff) {
                    title = EntitiesResolver.resolveName(systemField.getText(), srcEnt);
                }
                else {
                    title = "N/A";
                }

                // Ignore messages relative to an Entity that is still unknown
                if (title == null)
                    return;

                InspectorMessagePanel imp = new InspectorMessagePanel();
                imp.setTitle("Source Entity: " + title);
                // pane.add(new JLabel("\u2206t " + (msg.getTimestamp() - lastMessageTimeByEntity.get(srcEnt))));

                if (!title.equals("N/A"))
                    pane.add(imp, "wrap, w 600, h 200!");
                else
                    pane.add(imp, "wrap");

                entities.put(srcEnt, imp);
                imp.setMessage(msg);

                try {
                    SwingUtilities.invokeAndWait(new Runnable() {

                        @Override
                        public void run() {
                            jsp.invalidate();
                            jsp.validate();
                            jsp.repaint();
                        }
                    });
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.SimpleSubPanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
        // TODO Auto-generated method stub
        
    }
}
