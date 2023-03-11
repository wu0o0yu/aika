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

import java.awt.*;

import static java.awt.GridBagConstraints.HORIZONTAL;
import static java.awt.GridBagConstraints.WEST;

/**
 * @author Lukas Molzberger
 */
public abstract class AbstractProperty {

    protected Container parent;

    public AbstractProperty(Container parent) {
        this.parent = parent;
    }

    public abstract void addField(int pos, Insets insets);

    public abstract void registerListener();

    public abstract void deregisterListener();

    protected void addGridEntry(Component comp, int x, int y, int width, Insets insets) {
        GridBagConstraints c = new GridBagConstraints();
        c.fill = HORIZONTAL;
        c.anchor = WEST;
        c.weightx = 1.0;
        c.gridx = x;
        c.gridy = y;
        c.gridwidth = width;
        c.insets = insets;
        parent.add(comp, c);
    }
}
