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

import network.aika.debugger.activations.ActivationPanel;
import network.aika.debugger.activations.LinkPanel;
import network.aika.debugger.neurons.NeuronPanel;
import network.aika.debugger.neurons.SynapsePanel;
import network.aika.debugger.properties.AbstractPropertyPanel;
import network.aika.elements.Element;
import network.aika.elements.activations.Activation;
import network.aika.elements.links.Link;
import network.aika.elements.neurons.Neuron;
import network.aika.elements.synapses.Synapse;

import javax.swing.*;

import static network.aika.debugger.activations.ActivationConsoleManager.getScrollPane;

/**
 * @author Lukas Molzberger
 */
public class ElementPanel extends JTabbedPane {

    public static ElementPanel createElementPanel(Element e) {
        if(e instanceof Activation<?>)
            return new ActivationPanel((Activation) e);
        else if(e instanceof Link)
            return new LinkPanel((Link) e);
        if(e instanceof Neuron)
            return new NeuronPanel((Neuron) e);
        else if(e instanceof Synapse)
            return new SynapsePanel((Synapse) e);

        return null;
    }

    protected void addTab(String title, String tip, AbstractPropertyPanel panel) {
        addTab(
                title,
                null,
                getScrollPane(panel),
                tip
        );
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    }

    public void remove() {
    }
}
