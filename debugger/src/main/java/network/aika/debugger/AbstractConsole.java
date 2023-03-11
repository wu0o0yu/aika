/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package network.aika.debugger;


import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.util.function.Consumer;

import static java.awt.Color.lightGray;

/**
 * @author Lukas Molzberger
 */
public abstract class AbstractConsole extends JTextPane {
    public static String NOT_SET_STR = "--";

    public AbstractConsole() {
        addStylesToDocument(getStyledDocument());
        setEditable(false);
        setFocusable(false);
    }

    public void render(Consumer<StyledDocument> content) {
        setOpaque(false);
        DefaultStyledDocument sDoc = new DefaultStyledDocument();
        addStylesToDocument(sDoc);

        content.accept(sDoc);

        SwingUtilities.invokeLater(() -> setStyledDocument(sDoc));
    }

    public void addStylesToDocument(StyledDocument doc) {
        Color green = new Color(0, 130, 0);

        Style def = StyleContext.getDefaultStyleContext().
                getStyle(StyleContext.DEFAULT_STYLE);
        StyleConstants.setFontFamily(def, "SansSerif");

        Style regular = doc.addStyle("regular", def);
        StyleConstants.setFontSize(regular, 11);

        Style s = doc.addStyle("regularGreen", regular);
        StyleConstants.setForeground(s, green);

        s = doc.addStyle("regularGray", regular);
        StyleConstants.setForeground(s, lightGray);

        s = doc.addStyle("italic", regular);
        StyleConstants.setItalic(s, true);

        Style bold = doc.addStyle("bold", regular);
        StyleConstants.setBold(bold, true);

        s = doc.addStyle("boldGreen", bold);
        StyleConstants.setForeground(s, green);

        s = doc.addStyle("boldGray", bold);
        StyleConstants.setForeground(s, lightGray);

        s = doc.addStyle("small", regular);

        s = doc.addStyle("headline", regular);
        StyleConstants.setFontSize(s, 14);
    }

    public void clear() {
        StyledDocument sDoc = getStyledDocument();
        try {
            sDoc.remove(0, sDoc.getLength());
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }
}
