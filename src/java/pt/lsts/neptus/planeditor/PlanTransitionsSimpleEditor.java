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
 * Author: Paulo Dias
 * 5/06/2011
 */
package pt.lsts.neptus.planeditor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.PlainDocument;
import javax.swing.text.PlainView;
import javax.swing.text.Segment;
import javax.swing.text.StyledEditorKit;
import javax.swing.text.Utilities;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;

import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.Maneuver;
import pt.lsts.neptus.types.mission.ActionType;
import pt.lsts.neptus.types.mission.ConditionType;
import pt.lsts.neptus.types.mission.TransitionType;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.util.ConsoleParse;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.NameNormalizer;

/**
 * @author pdias
 *
 */
@SuppressWarnings("serial")
public class PlanTransitionsSimpleEditor extends JPanel {

    private PlanType plan = null;
    private String[] nodeStrList = null;
    private Vector<PlanTransitionGuiPanel> toRemoveFromPlanGraph = null;
    
    // GUI
    private JPanel holder, buttonBarPanel;
    private JScrollPane scrollHolder;
    private JButton okButton, cancelButton, addButton, removeButton, clearSelectionButton;
    private AbstractAction okAction, cancelAction, addAction, removeAction, clearSelectionAction;
    
    /**
     * @param plan
     * @param mapPlanEditor 
     */
    public PlanTransitionsSimpleEditor(PlanType plan) {
        this.plan = plan;
        initializeActions();
        initialize();
    }
    
    /**
     * 
     */
    private void initialize() {
        toRemoveFromPlanGraph = new Vector<PlanTransitionsSimpleEditor.PlanTransitionGuiPanel>();
        
        Vector<String> nodes = new Vector<String>();
        for (Maneuver man : plan.getGraph().getAllManeuvers()) {
            nodes.add(man.getId());
        }
        nodeStrList = nodes.toArray(new String[nodes.size()]);
        
//        String str = "", strNodes = "Nodes:";
//        for (TransitionType nee : plan.getGraph().getAllEdges()) {
//            str += "\n" + nee.getSourceManeuver() + " -> " + nee.getTargetManeuver() + "  [" +
//            		nee.getCondition() + " / " + nee.getAction() + "]";
//        }
//        for (Maneuver man : plan.getGraph().getAllManeuvers()) {
//            strNodes += " \"" + man.getId() + "\"";
//        }
//        NeptusLog.pub().info("<###> "+str);
        setLayout(new BorderLayout(5, 10));
//        add(new JLabel(strNodes), BorderLayout.NORTH);
        
        holder = new JPanel();
        holder.setLayout(new BoxLayout(holder, BoxLayout.PAGE_AXIS));
        holder.setSize(100, 60);
        
        for (TransitionType nee : plan.getGraph().getAllEdges()) {
            PlanTransitionGuiPanel tEd = new PlanTransitionGuiPanel(nee.getSourceManeuver(),
                    nee.getTargetManeuver(), nee.getCondition().getStringRepresentation(), 
                    nee.getAction().getStringRepresentation(), nodeStrList);
            tEd.transition = nee;
            holder.add(tEd);
        }
        
//        PlanTransitionGuiPanel tEd = new PlanTransitionGuiPanel("A",
//                "B", "ManeuverIsDone", 
//                "End");
//        holder.add(tEd);
//        tEd = new PlanTransitionGuiPanel("B", "A", "ManeuverIsDone & ~df[t=\"30\"]", "End");
//        holder.add(tEd);

        scrollHolder = new JScrollPane(holder);
        add(scrollHolder);
        
        Dimension buttonDimension = new Dimension(80, 30);
        okButton = new JButton(okAction);
        okButton.setSize(buttonDimension);
        cancelButton = new JButton(cancelAction);
        cancelButton.setSize(buttonDimension);
        addButton = new JButton(addAction);
        addButton.setSize(buttonDimension);
        removeButton = new JButton(removeAction);
        removeButton.setSize(buttonDimension);
        removeButton = new JButton(removeAction);
        removeButton.setSize(buttonDimension);
        clearSelectionButton = new JButton(clearSelectionAction);
        clearSelectionButton.setSize(buttonDimension);
        
        buttonBarPanel = new JPanel();
        buttonBarPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
        buttonBarPanel.add(addButton);
        buttonBarPanel.add(Box.createHorizontalStrut(10));
        buttonBarPanel.add(removeButton);
        buttonBarPanel.add(clearSelectionButton);
        buttonBarPanel.add(Box.createHorizontalStrut(10));
        buttonBarPanel.add(okButton);
        buttonBarPanel.add(cancelButton);
        
        GuiUtils.reactEscapeKeyPress(cancelButton);
        
        add(buttonBarPanel, BorderLayout.SOUTH);
    }

    /**
     * 
     */
    private void initializeActions() {
        okAction = new AbstractAction(I18n.text("Save")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                // validate();
                updatePlanWithModifiedTransitions();
                Window window = SwingUtilities.getWindowAncestor(PlanTransitionsSimpleEditor.this);
                if (window != null) {
                    window.setVisible(false);
                    window.dispose();
                }
            }
        };

        cancelAction = new AbstractAction(I18n.text("Cancel")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                Window window = SwingUtilities.getWindowAncestor(PlanTransitionsSimpleEditor.this);
                if (window != null) {
                    window.setVisible(false);
                    window.dispose();
                }
            }
        };

        addAction = new AbstractAction(I18n.text("Add")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                int index = -1;
                for (int i = 0 ; i < holder.getComponentCount(); i++ ) {
                    try {
                        PlanTransitionGuiPanel pt = (PlanTransitionGuiPanel)holder.getComponent(i);
                        if (pt.selCheckBox.isSelected()) {
                            index = i;
                            break;
                        }
                    }
                    catch (ClassCastException e2) {
                        // This is ok to happen
                    }
                }
                PlanTransitionGuiPanel tEd = new PlanTransitionGuiPanel("", "", "ManeuverIsDone", "", nodeStrList);
                if (index < 0)
                    holder.add(tEd);
                else
                    holder.add(tEd, index);
                holder.invalidate();
                holder.validate();
                scrollHolder.invalidate();
                scrollHolder.validate();
            }
        };
        
        removeAction = new AbstractAction(I18n.text("Remove Selected")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                for (Component comp : holder.getComponents()) {
                    try {
                        PlanTransitionGuiPanel pt = (PlanTransitionGuiPanel)comp;
                        if (pt.selCheckBox.isSelected()) {
                            holder.remove(pt);
                            if (pt.transition != null)
                                toRemoveFromPlanGraph.add(pt);
                        }
                    }
                    catch (ClassCastException e2) {
                        // This is ok to happen
                    }
                }
                holder.invalidate();
                holder.validate();
                scrollHolder.invalidate();
                scrollHolder.validate();
                holder.repaint();
            }
        };

        clearSelectionAction = new AbstractAction(I18n.text("Clear Selection")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                for (Component comp : holder.getComponents()) {
                    try {
                        PlanTransitionGuiPanel pt = (PlanTransitionGuiPanel)comp;
                        pt.selCheckBox.setSelected(false);
                    }
                    catch (ClassCastException e2) {
                        // This is ok to happen
                    }
                }
            }
        };
    }

    /**
     * 
     */
    protected void updatePlanWithModifiedTransitions() {
        LinkedHashMap<String, TransitionType> transitions = plan.getGraph().getTransitions();
        for (Component comp : holder.getComponents()) {
            try {
                PlanTransitionGuiPanel pt = (PlanTransitionGuiPanel)comp;
                if (pt.transition == null) {
                    pt.transition = new TransitionType((String)pt.sourceComboBox.getSelectedItem(), 
                            (String)pt.targetComboBox.getSelectedItem());
                    pt.transition.setId(NameNormalizer.getRandomID());
                    ConditionType condT = new ConditionType();
                    condT.setCondition(pt.conditionPane.getText());
                    pt.transition.setCondition(condT);
                    ActionType actT = new ActionType();
                    actT.setAction(pt.actionPane.getText());
                    pt.transition.setAction(actT);
                    
                    if (pt.transition.getSourceManeuver() != null && pt.transition.getTargetManeuver() != null)
                        transitions.put(pt.transition.getId(), pt.transition);
                }
                else {
                    pt.transition.setSourceManeuver((String)pt.sourceComboBox.getSelectedItem());
                    pt.transition.setTargetManeuver((String)pt.targetComboBox.getSelectedItem());
                    pt.transition.getCondition().setCondition(pt.conditionPane.getText());
                    pt.transition.getAction().setAction(pt.actionPane.getText());
                    
                    if (pt.transition.getSourceManeuver() == null || pt.transition.getTargetManeuver() == null)
                        toRemoveFromPlanGraph.add(pt);
                }
            }
            catch (ClassCastException e2) {
                // This is ok to happen
            }
            
            for (PlanTransitionGuiPanel pttr : toRemoveFromPlanGraph) {
                if (pttr.transition != null) {
                    transitions.remove(pttr.transition.getId());
                    //NeptusLog.pub().info("<###>Transition: "+pttr.transition.getId());
                }
            }
        }
    }

    /**
     * 
     */
    public void clean() {
        plan = null;
        toRemoveFromPlanGraph.clear();
        holder.removeAll();
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#finalize()
     */
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        clean();
    }
    
    static class PlanTransitionGuiPanel extends JPanel {
        TransitionType transition = null;
        
//        String source = "";
//        String target = "";
//        
//        String condition = "ManeuverIsDone";
//        String action = "";
        
        String[] nodesStrList = null;
        
        // GUI
        JCheckBox selCheckBox = null;
        //JTextField sourcePane = null;
        JComboBox<?> sourceComboBox = null; 
        //JTextField targetPane = null;
        JComboBox<?> targetComboBox = null;
        JTextPane conditionPane = null;
        JTextPane actionPane = null;
        
//        String[] petStrings = { "Bird", "Cat", "Dog", "Rabbit", "Pig" };
//        //Create the combo box, select item at index 4.
//        //Indices start at 0, so 4 specifies the pig.
//        JComboBox petList = new JComboBox(petStrings);
//        petList.setSelectedIndex(4);
//        petList.addActionListener(this);
      
        /**
         * 
         */
        public PlanTransitionGuiPanel(String source, String target, 
                String condition, String action, String[] nodesStrList) {

            sourceComboBox = new JComboBox<Object>(nodesStrList);
            targetComboBox = new JComboBox<Object>(nodesStrList);
            conditionPane = new JTextPane();
            conditionPane.setEditorKitForContentType("text/imc-cond", new ConditionStyleEdtorKit());
            conditionPane.setContentType("text/imc-cond");
            conditionPane.setText(condition);
            conditionPane.setEditable(false);

            actionPane = new JTextPane();
            actionPane.setEditorKitForContentType("text/imc-act", new ActionStyleEdtorKit());
            actionPane.setContentType("text/imc-act");
            actionPane.setText(action);
            actionPane.setEditable(false);
            
            sourceComboBox.setSelectedItem(source);
            targetComboBox.setSelectedItem(target);

            initialize();
        }
        
        /**
         * 
         */
        private void initialize() {
            selCheckBox = new JCheckBox();
            JScrollPane conditionPaneScroll = new JScrollPane(conditionPane,
                    JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, 
                    JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            JScrollPane actionPaneScroll = new JScrollPane(actionPane,
                    JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, 
                    JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

            JLabel aL = new JLabel("<html><b>>");
            JLabel gL = new JLabel("<html><b>/");
            GroupLayout groupLayout = new GroupLayout(this);
            this.setLayout(groupLayout);
            groupLayout.setAutoCreateContainerGaps(true);
            groupLayout.setHorizontalGroup(groupLayout.createSequentialGroup()
                    .addComponent(selCheckBox)
                    .addComponent(sourceComboBox, 40, 80, Short.MAX_VALUE)
                    .addGap(10, 10, 10)
                    .addComponent(aL)
                    .addGap(10, 10, 10)
                    .addComponent(targetComboBox)
                    .addGap(20, 20, 20)
                    .addComponent(conditionPaneScroll, 90, 180, Short.MAX_VALUE)
                    .addGap(10, 10, 10)
                    .addComponent(gL)
                    .addGap(10, 10, 10)
                    .addComponent(actionPaneScroll, 90, 180, Short.MAX_VALUE));
            
            groupLayout.setVerticalGroup(groupLayout.createParallelGroup()
                    .addComponent(selCheckBox)
                    .addComponent(sourceComboBox, 10, 10, 30)
                    .addComponent(aL)
                    .addComponent(targetComboBox)
                    .addComponent(conditionPaneScroll, 40, 60, Short.MAX_VALUE)
                    .addComponent(gL)
                    .addComponent(actionPaneScroll, 40, 60, Short.MAX_VALUE));

            groupLayout.linkSize(SwingConstants.HORIZONTAL, sourceComboBox, targetComboBox);
            groupLayout.linkSize(SwingConstants.HORIZONTAL, conditionPaneScroll, actionPaneScroll);
            groupLayout.linkSize(SwingConstants.HORIZONTAL, aL, gL);

            groupLayout.linkSize(SwingConstants.VERTICAL, sourceComboBox, targetComboBox, aL, gL);

            groupLayout.linkSize(SwingConstants.VERTICAL, conditionPaneScroll, actionPaneScroll);
        }
    }
    
    
    private static class ConditionStyleEdtorKit extends StyledEditorKit {
        private ViewFactory viewFactory;
//      {
//          viewFactory = new ViewFactory() {
//              @Override
//              public View create(Element elem) {
//                  return new ConditionView(elem);
//              }
//          };
//      }
        public ConditionStyleEdtorKit() {
            viewFactory = new ViewFactory() {
                @Override
                public View create(Element elem) {
                    return new ConditionView(elem);
                }
            };
        }
      
        @Override
        public ViewFactory getViewFactory() {
            return viewFactory;
        }

        @Override
        public String getContentType() {
            return "text/imc-cond";
        }
    }

    private static class ActionStyleEdtorKit extends StyledEditorKit {
        private ViewFactory viewFactory;
//      {
//          viewFactory = new ViewFactory() {
//              @Override
//              public View create(Element elem) {
//                  return new ConditionView(elem);
//              }
//          };
//      }
        public ActionStyleEdtorKit() {
            viewFactory = new ViewFactory() {
                @Override
                public View create(Element elem) {
                    return new ActionView(elem);
                }
            };
        }
      
        @Override
        public ViewFactory getViewFactory() {
            return viewFactory;
        }

        @Override
        public String getContentType() {
            return "text/imc-act";
        }
    }

    private static class ConditionView extends ConditionActionView {
        private static String GENERIC_TXT = "[A-Za-z]+[A-Za-z0-9\\-_]*(:[A-Za-z]+[A-Za-z0-9\\-_]+)?";

        private static String PAT_ATR = "\\w*\\=\\w*(\"[^\"]*\")";
        private static String PAT_COND = "((~?" + GENERIC_TXT + ")(\\[("+PAT_ATR+"(;?|(;"+PAT_ATR+"))*)\\])?)";
        private static String PAT_1 = "(" + PAT_COND + "( [&|] " + PAT_COND + ")?)";
        private static String PAT_2 = "(" + PAT_1 + "( [&|] " + PAT_1 + ")*)";
        private static String PAT_3 = "(" + PAT_2 + "|(~?\\([ ]*" + PAT_2 + "[ ]*\\)))";
        private static String PAT1 = PAT_3 + "( [&|] " + PAT_3 + ")*";

        /**
         * @param element
         */
        public ConditionView(Element element) {
            super(element);
            
            //patternColors.put(Pattern.compile(PAT_COD1), Color.MAGENTA);
            //patternColors.put(Pattern.compile(PAT_COD), Color.GREEN);
//            patternColors.put(Pattern.compile(PAT_PAR1), new Color(127, 127, 63));
//            patternColors.put(Pattern.compile(PAT_PAR), new Color(63, 127, 127));
            //patternColors.put(Pattern.compile(PAT_COD), Color.BLUE);
            patternColors.put(Pattern.compile(PAT1), Color.GREEN.darker());
            //patternColors.put(Pattern.compile(PAT_COND), Color.BLUE);
        }
    }

    private static class ActionView extends ConditionActionView {
        private static String GENERIC_TXT = "[A-Za-z]+[A-Za-z0-9\\-_]*(:[A-Za-z]+[A-Za-z0-9\\-_]+)?";

        private static String PAT_1 = "(" + GENERIC_TXT + ")(;[ ]?(" + GENERIC_TXT + "))*";
        private static String PAT1 = PAT_1;

        /**
         * @param element
         */
        public ActionView(Element element) {
            super(element);
            patternColors.put(Pattern.compile(PAT1), Color.GREEN.darker());
        }
    }

    private static class ConditionActionView extends PlainView {

        HashMap<Pattern, Color> patternColors;
//        private String GENERIC_XML_NAME = "[A-Za-z]+[A-Za-z0-9\\-_]*(:[A-Za-z]+[A-Za-z0-9\\-_]+)?";
//        private String TAG_PATTERN = "(</?" + GENERIC_XML_NAME + ")\\s?>?";
//        private String TAG_END_PATTERN = "(/>)";
//        private String TAG_ATTRIBUTE_PATTERN = "(" + GENERIC_XML_NAME + ")\\w*\\=";
//        private String TAG_ATTRIBUTE_VALUE = "\\w*\\=\\w*(\"[^\"]*\")";
//        private String TAG_COMMENT = "(<\\!--[\\w ]*-->)";
//        private String TAG_CDATA = "(<\\!\\[CDATA\\[.*\\]\\]>)";

        {
            // NOTE: the order is important!
            patternColors = new LinkedHashMap<Pattern, Color>();
//            patternColors
//                    .put(Pattern.compile(TAG_PATTERN), new Color(63, 127, 127));
//            patternColors.put(Pattern.compile(TAG_CDATA), Color.GRAY);
//            patternColors.put(Pattern.compile(TAG_ATTRIBUTE_PATTERN), new Color(
//                    127, 0, 127));
//            patternColors.put(Pattern.compile(TAG_END_PATTERN), new Color(63, 127,
//                    127));
//            patternColors.put(Pattern.compile(TAG_ATTRIBUTE_VALUE), new Color(42,
//                    0, 255));
//            patternColors.put(Pattern.compile(TAG_COMMENT), Color.BLUE);
        }

        public ConditionActionView(Element element) {
            super(element);

            // Set tabsize to 4 (instead of the default 8)
            getDocument().putProperty(PlainDocument.tabSizeAttribute, 4);
        }

        @Override
        protected int drawUnselectedText(Graphics graphics, int x, int y, int p0,
                int p1) throws BadLocationException {

            Document doc = getDocument();
            String text = doc.getText(p0, p1 - p0);

            Segment segment = getLineBuffer();

            SortedMap<Integer, Integer> startMap = new TreeMap<Integer, Integer>();
            SortedMap<Integer, Color> colorMap = new TreeMap<Integer, Color>();

            // Match all regexes on this snippet, store positions
            for (Map.Entry<Pattern, Color> entry : patternColors.entrySet()) {

                Matcher matcher = entry.getKey().matcher(text);

                while (matcher.find()) {
                    startMap.put(matcher.start(1), matcher.end());
                    colorMap.put(matcher.start(1), entry.getValue());
                }
            }

            // TODO: check the map for overlapping parts

            int i = 0;

            // Colour the parts
            for (Map.Entry<Integer, Integer> entry : startMap.entrySet()) {
                int start = entry.getKey();
                int end = entry.getValue();

                if (i < start) {
                    graphics.setColor(Color.black);
                    doc.getText(p0 + i, start - i, segment);
                    x = Utilities.drawTabbedText(segment, x, y, graphics, this, i);
                }

                graphics.setColor(colorMap.get(start));
                i = end;
                doc.getText(p0 + start, i - start, segment);
                x = Utilities.drawTabbedText(segment, x, y, graphics, this, start);
            }

            // Paint possible remaining text black
            if (i < text.length()) {
                graphics.setColor(Color.black);
                doc.getText(p0 + i, text.length() - i, segment);
                x = Utilities.drawTabbedText(segment, x, y, graphics, this, i);
            }

            return x;
        }
    }
    
    
    public static void main(String[] args) {
//        JPanel holder = new JPanel();
//        holder.setLayout(new BoxLayout(holder, BoxLayout.PAGE_AXIS));
//        //holder.setLayout(new GridLayout(0,1));
//        
//        holder.add(new PlanTransitionGuiPanel("A", "B", "ManeuverIsDone & ~df[t=\"30\"]", "End"));
//        holder.add(new PlanTransitionGuiPanel("B", "A", "ManeuverIsDone | ~df[t=\"30\"] | (df | w | w)", "End"));
//        
//        GuiUtils.testFrame(holder);
//        
//        MissionType mission = new MissionType("missions/APDL/missao-apdl.nmisz");
//        PlanType plan = mission.getIndividualPlansList().values().toArray(new PlanType[0])[0];
//        GuiUtils.testFrame(new PlanTransitionsSimpleEditor(plan, new MapPlanEditor(plan)));
        
        ConsoleParse.consoleLayoutLoader("conf/consoles/seacon-basic.ncon");
    }
}
