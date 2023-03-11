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
package network.aika.debugger.properties;

import javax.swing.*;
import java.awt.*;

import static java.awt.GridBagConstraints.HORIZONTAL;
import static java.awt.GridBagConstraints.PAGE_START;

/**
 * @author Lukas Molzberger
 */
public class TitleProperty extends AbstractProperty {

    private String title;
    private Font font;

    public TitleProperty(Container parent, String title, Font font) {
        super(parent);
        this.title = title;
        this.font = font;
    }

    @Override
    public void addField(int pos, Insets insets) {
        JLabel label = new JLabel(title);
        label.setFont(font);

        GridBagConstraints c = new GridBagConstraints();
        c.fill = HORIZONTAL;
        c.anchor = PAGE_START;
        // c.weightx = 0.5;
        c.gridx = 0;
        c.gridwidth = 4;
        c.gridy = pos;
        parent.add(label, c);
    }

    @Override
    public void registerListener() {

    }

    @Override
    public void deregisterListener() {

    }
}
