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
 * Author: Rui Gonçalves
 * 200?/??/??
 */
package pt.lsts.neptus.gui.checklist;

import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import pt.lsts.neptus.types.checklist.CheckAutoSubItem;
import pt.lsts.neptus.types.checklist.CheckAutoVarIntervalItem;
import pt.lsts.neptus.util.GuiUtils;

@SuppressWarnings("serial")
public class VariableIntervalItem extends JPanel implements CheckSubItem {

    public static final String TYPE_ID = "variableTestRange";

    private JTextField variableName = null;
    private JTextField variablePath = null;
    private JFormattedTextField lastValue = null;
    private JFormattedTextField startInterval = null;
    private JFormattedTextField endInterval = null;
    private JCheckBox outInterval = null;
    private JCheckBox check = null;
    private JButton remove = null;
    private JLabel startInc = null;
    private JLabel endInc = null;

    private AutoItemsList parent = null;

    public VariableIntervalItem(AutoItemsList p, CheckAutoVarIntervalItem cavii) {
        this(p);
        fillFromCheckAutoVarIntervalItem(cavii);
    }

    public VariableIntervalItem(AutoItemsList p) {
        super();
        parent = p;
        initialize();
    }

    private void initialize() {

        this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        this.setOpaque(false);
        variableName = new JTextField();
        variableName.setColumns(10);
        variableName.setSize(100, 20);
        variableName.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                parent.fireChangeEvent(VariableIntervalItem.this);
            }
        });

        variablePath = new JTextField();
        variablePath.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                parent.fireChangeEvent(VariableIntervalItem.this);
            }
        });

        lastValue = new JFormattedTextField(GuiUtils.getNeptusDecimalFormat() /* NumberFormat.getInstance() */);
        lastValue.setColumns(3);
        lastValue.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                parent.fireChangeEvent(VariableIntervalItem.this);
            }
        });

        startInterval = new JFormattedTextField(GuiUtils.getNeptusDecimalFormat() /* NumberFormat.getInstance() */);
        startInterval.setColumns(3);
        startInterval.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                parent.fireChangeEvent(VariableIntervalItem.this);
            }
        });

        endInterval = new JFormattedTextField(GuiUtils.getNeptusDecimalFormat() /* NumberFormat.getInstance() */);
        endInterval.setColumns(3);
        endInterval.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                parent.fireChangeEvent(VariableIntervalItem.this);
            }
        });

        outInterval = new JCheckBox();
        outInterval.setOpaque(false);
        outInterval.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                parent.fireChangeEvent(VariableIntervalItem.this);
            }
        });

        remove = new JButton(ICON_CLOSE);
        remove.setMargin(new Insets(0, 0, 0, 0));
        remove.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                parent.removeVarIntervalItem(VariableIntervalItem.this);
            }
        });

        this.add(new JLabel("Variable Description:"));
        this.add(variableName);
        this.add(new JLabel(" Path:"));
        this.add(variablePath);

        this.add(new JLabel("  Interval:"));
        this.add(getStartInc());
        this.add(startInterval);
        this.add(new JLabel(";"));
        this.add(endInterval);
        this.add(getEndInc());
        this.add(new JLabel("  (Not in"));
        this.add(outInterval);
        this.add(new JLabel(") Registered Value"));
        this.add(lastValue);
        this.add(Box.createHorizontalGlue());
        this.add(new JLabel(" Checked:"));
        this.add(getCheck());
        this.add(remove);
        // this.setBorder(javax.swing.BorderFactory.createLineBorder(java.awt.Color.gray,1));

        /*
         * this.add(getJPanel(), null); this.add(getNotesPanel(), null); this.add(getActionsPanel(), null);
         */
    }

    private JLabel getStartInc() {
        if (startInc == null) {
            startInc = new JLabel("[");
            startInc.addMouseListener(new MouseListener() {
                @Override
                public void mouseClicked(MouseEvent e) {

                    VariableIntervalItem.this.setStartInclusion(!isStartInclusion());
                    parent.fireChangeEvent(VariableIntervalItem.this);
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    // TODO Auto-generated method stub

                }

                @Override
                public void mouseExited(MouseEvent e) {
                    // TODO Auto-generated method stub

                }

                @Override
                public void mousePressed(MouseEvent e) {
                    // TODO Auto-generated method stub

                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    // TODO Auto-generated method stub

                }
            });
        }
        return startInc;
    }

    private JLabel getEndInc() {
        if (endInc == null) {
            endInc = new JLabel("]");
            endInc.addMouseListener(new MouseListener() {
                @Override
                public void mouseClicked(MouseEvent e) {

                    VariableIntervalItem.this.setEndInclusion(!isEndInclusion());
                    parent.fireChangeEvent(VariableIntervalItem.this);
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    // TODO Auto-generated method stub

                }

                @Override
                public void mouseExited(MouseEvent e) {
                    // TODO Auto-generated method stub

                }

                @Override
                public void mousePressed(MouseEvent e) {
                    // TODO Auto-generated method stub

                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    // TODO Auto-generated method stub

                }
            });
        }
        return endInc;
    }

    public boolean isStartInclusion() {
        return startInc.getText().equals("[");
    }

    public void setStartInclusion(boolean startInclusion) {
        if (startInclusion)
            startInc.setText("[");
        else
            startInc.setText("]");

    }

    public boolean isEndInclusion() {
        return endInc.getText().equals("]");
    }

    public void setEndInclusion(boolean endInclusion) {
        if (endInclusion)
            endInc.setText("]");
        else
            endInc.setText("[");
    }

    private JCheckBox getCheck() {
        if (check == null) {
            check = new JCheckBox("check");
            check.setOpaque(false);
            check.setText(" ");
            check.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    parent.fireChangeEvent(VariableIntervalItem.this);
                }
            });
        }
        return check;
    }

    public double getRegisteredValue() {
        if (lastValue.getValue() == null || lastValue.getValue().toString().equals(""))
            return Double.NaN;
        return ((Number) lastValue.getValue()).doubleValue();
    }

    public double getStartInterval() {
        if (endInterval.getValue() == null || startInterval.getValue().toString().equals(""))
            return Double.NEGATIVE_INFINITY;
        return ((Number) startInterval.getValue()).doubleValue();
    }

    public double getEndInterval() {
        if (endInterval.getValue() == null || endInterval.getValue().toString().equals(""))
            return Double.POSITIVE_INFINITY;
        return ((Number) endInterval.getValue()).doubleValue();
    }

    public String getVariableName() {
        return variableName.getText();
    }

    public String getVariablePath() {
        return variablePath.getText();
    }

    public boolean isOutInterval() {
        return outInterval.isSelected();
    }

    public boolean isChecked() {
        return check.isSelected();
    }

    private void fillFromCheckAutoVarIntervalItem(CheckAutoVarIntervalItem cavii) {
        variableName.setText(cavii.getVarName());
        variablePath.setText(cavii.getVarPath());
        startInterval.setValue(cavii.getStartInterval());
        endInterval.setValue(cavii.getEndInterval());
        outInterval.setSelected(cavii.isOutInterval());
        check.setSelected(cavii.isChecked());
        lastValue.setValue(cavii.getVarValue());
        setStartInclusion(cavii.isStartInclusion());
        setEndInclusion(cavii.isEndInclusion());
    }

    @Override
    public CheckAutoSubItem getCheckAutoSubItem() {
        CheckAutoVarIntervalItem ret = new CheckAutoVarIntervalItem();
        ret.setEndInterval(getEndInterval());
        ret.setStartInterval(getStartInterval());
        ret.setVarName(getVariableName());
        ret.setVarPath(getVariablePath());
        ret.setOutInterval(isOutInterval());
        ret.setChecked(isChecked());
        ret.setVarValue(getRegisteredValue());
        ret.setStartInclusion(isStartInclusion());
        ret.setEndInclusion(isEndInclusion());
        return ret;
    }
}
