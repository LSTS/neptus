/*
 * Copyright (c) 2004-2018 Universidade do Porto - Faculdade de Engenharia
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
 * Author: zp
 * Aug 8, 2018
 */
package pt.lsts.neptus.plugins.logs;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.TimeZone;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

import pt.lsts.neptus.data.Pair;
import pt.lsts.neptus.util.GuiUtils;

/**
 * @author zp
 *
 */
public class LogBookPanel extends JPanel {

    private static final long serialVersionUID = 6240623007831835681L;
    private JEditorPane logbook = new JEditorPane("text/html", "");
    private JTextField entry = new JTextField();
    private SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
    private JButton btnAdd = new JButton("Add");
    private File file;

    public JsonArray toJson() {
        JsonArray arr = new JsonArray();
        Document doc = Jsoup.parse(logbook.getText());
        Elements elems = doc.getElementsByTag("tr");
        for (Element elem : elems) {
            try {
                Elements inner = elem.getElementsByTag("td");
                JsonObject row = new JsonObject();
                row.add("timestamp", inner.get(0).text());
                row.add("text", inner.get(1).text());
                arr.add(row);
            }
            catch (Exception e) {
                // TODO: handle exception
            }
        }
        return arr;
    }

    public void merge(JsonArray log) {
        JsonArray thisLog = toJson();

        
        ArrayList<Pair<String, String>> entries = new ArrayList<>();
        thisLog.forEach(v -> entries.add(
                new Pair<String, String>(v.asObject().getString("timestamp", ""), v.asObject().getString("text", ""))));

        int sizeBefore = entries.size();
        log.forEach(v -> {
            Pair<String, String> val = new Pair<String, String>(v.asObject().getString("timestamp", ""),
                    v.asObject().getString("text", ""));

            if (!entries.contains(val)) {
                entries.add(val);
            }
        });

        // if no new entries, merge is done.
        if (sizeBefore == entries.size())
            return;
        
        entries.sort(new Comparator<Pair<String, String>>() {
            @Override
            public int compare(Pair<String, String> o1, Pair<String, String> o2) {
                return o1.first().compareTo(o2.first());
            }
        });
        
        logbook.setText("");
        
        for (Pair<String, String> entry : entries)
            addLogEntry(entry.first(), entry.second());
        
    }

    public LogBookPanel() {
        this(new File("logbook.html"));
    }

    public LogBookPanel(File html) {
        this.file = html;
        setLayout(new BorderLayout(5, 5));
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(entry, BorderLayout.CENTER);

        bottom.add(btnAdd, BorderLayout.EAST);

        add(new JScrollPane(logbook), BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        btnAdd.addActionListener(this::addText);
        entry.addActionListener(this::addText);

        if (html != null) {
            try {
                Document soup = Jsoup
                        .parse(Files.readAllLines(html.toPath()).stream().collect(Collectors.joining("\n")));
                logbook.setText(soup.html());
                logbook.setText(cleanHtml());
            }
            catch (IOException e) {
            }
        }
        installUndoRedo();
    }

    private void addText(ActionEvent evt) {
        addLogEntry(entry.getText());
        entry.setText("");
    }

    private String cleanHtml() {
        Document doc = Jsoup.parse(logbook.getText());

        Document newDoc = new Document("");
        Element newTable = newDoc.appendElement("table").attr("id", "table").attr("width", "100%");

        int i = 0;
        // copy all table rows to new doc
        for (Element el : doc.getElementsByTag("tr")) {
            if (i++ % 2 == 0)
                el.attr("bgcolor", "#FFFFFF");
            else
                el.attr("bgcolor", "#EEEEEE");
            newTable.appendChild(el);
        }

        return newDoc.html();
    }
    
    public void addLogEntry(String time, String text) {
        Document doc = Jsoup.parse(cleanHtml());

        // fetch previous html elements
        Element table = doc.getElementById("table");
        if (table == null) {
            doc = new Document("");
            table = doc.appendElement("table").attr("id", "table");
        }

        int row = doc.getElementsByTag("tr").size();

        Element newRow = table.appendElement("tr");

        if (row % 2 == 0)
            newRow.attr("bgcolor", "#FFFFFF");
        else
            newRow.attr("bgcolor", "#EEEEEE");

        newRow.appendElement("td").attr("width", "10px").appendElement("strong").appendText(time);
        newRow.appendElement("td").appendText(text);
        logbook.setText(doc.html());

        saveToFile();
        System.out.println(toJson());
    }

    public void addLogEntry(String text) {
        addLogEntry(sdf.format(new Date()), text);
    }

    private void saveToFile() {
        try {
            Files.write(file.toPath(), cleanHtml().getBytes(), StandardOpenOption.CREATE);
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void installUndoRedo() {
        final UndoManager undo = new UndoManager();
        javax.swing.text.Document doc = logbook.getDocument();

        doc.addUndoableEditListener(new UndoableEditListener() {
            public void undoableEditHappened(UndoableEditEvent evt) {
                undo.addEdit(evt.getEdit());
                saveToFile();
            }
        });

        logbook.getActionMap().put("Undo", new AbstractAction("Undo") {
            private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent evt) {
                try {
                    if (undo.canUndo()) {
                        undo.undo();
                        saveToFile();
                    }
                }
                catch (CannotUndoException e) {
                }
            }
        });

        logbook.getInputMap().put(KeyStroke.getKeyStroke("control Z"), "Undo");

        logbook.getActionMap().put("Redo", new AbstractAction("Redo") {
            private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent evt) {
                try {
                    if (undo.canRedo()) {
                        undo.redo();
                        saveToFile();
                    }
                }
                catch (CannotRedoException e) {
                }
            }
        });

        logbook.getInputMap().put(KeyStroke.getKeyStroke("control Y"), "Redo");
    }

    public static void main(String[] args) {
        GuiUtils.setLookAndFeelNimbus();
        LogBookPanel panel = new LogBookPanel();
        GuiUtils.testFrame(panel);
    }
}
