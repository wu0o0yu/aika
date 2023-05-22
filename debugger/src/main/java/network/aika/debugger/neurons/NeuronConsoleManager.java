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
package network.aika.debugger.neurons;

import network.aika.debugger.AbstractConsoleManager;
import network.aika.debugger.ElementPanel;
import network.aika.elements.Element;
import network.aika.elements.neurons.Neuron;
import network.aika.elements.synapses.Synapse;

import javax.swing.*;
import java.awt.*;

/**
 * @author Lukas Molzberger
 */
public class NeuronConsoleManager extends JPanel implements AbstractConsoleManager {

    protected ElementPanel neuronPanel;

    GridBagConstraints c1 = new GridBagConstraints();

    public NeuronConsoleManager() {
        super(new GridBagLayout());

        this.neuronPanel = new NeuronPanel(null);

        c1.fill = GridBagConstraints.BOTH;
        c1.weightx = 1.0;
        c1.weighty = 1.0;
        c1.anchor = GridBagConstraints.FIRST_LINE_START;

        add(neuronPanel, c1);

        setFocusCycleRoot(true);
    }

    public ElementPanel getNeuronPanel() {
        return neuronPanel;
    }

    public void showSelectedElementContext(Neuron n) {
        replaceTab(n);
    }

    public void showSelectedElementContext(Synapse s) {
        replaceTab(s);
    }

    private void replaceTab(Element e) {
        removeAll();
        invalidate();

        neuronPanel = ElementPanel.createElementPanel(this, e);
        add(neuronPanel, c1);

        revalidate();
        repaint();
    }
}
