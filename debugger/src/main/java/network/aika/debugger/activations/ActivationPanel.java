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
package network.aika.debugger.activations;

import network.aika.debugger.ElementPanel;
import network.aika.debugger.activations.properties.LinksPropertyPanel;
import network.aika.debugger.activations.properties.SynapsesPropertyPanel;
import network.aika.debugger.properties.AbstractPropertyPanel;
import network.aika.direction.Direction;
import network.aika.elements.activations.Activation;

import javax.swing.*;


/**
 * @author Lukas Molzberger
 */
public class ActivationPanel extends ElementPanel {


    public ActivationPanel(Activation act) {

        //The following line enables to use scrolling tabs.
        setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

        setFocusCycleRoot(true);

        initTabs(act);
    }

    private void initTabs(Activation act) {
        if(act == null)
            return;

        AbstractPropertyPanel actPropertyPanel = AbstractPropertyPanel.create(act);
        actPropertyPanel.addFinal();
        addTab(
                "Activation",
                "Shows the Activation",
                actPropertyPanel
        );

        AbstractPropertyPanel neuronPropertyPanel = AbstractPropertyPanel.createNeuralElement(act);
        neuronPropertyPanel.addFinal();
        addTab(
                "Neuron",
                "Shows the Neuron",
                neuronPropertyPanel
        );

        {
            AbstractPropertyPanel linksPropertyPanel = LinksPropertyPanel.create(act, Direction.INPUT);
            if (linksPropertyPanel != null)
                addTab(
                        "Input-Links",
                        "Shows the Input-Links",
                        linksPropertyPanel
                );
        }

        {
            AbstractPropertyPanel linksPropertyPanel = LinksPropertyPanel.create(act, Direction.OUTPUT);
            if (linksPropertyPanel != null)
                addTab(
                        "Output-Links",
                        "Shows the Output-Links",
                        linksPropertyPanel
                );
        }

        {
            AbstractPropertyPanel synapsesPropertyPanel = SynapsesPropertyPanel.create(act, Direction.INPUT, false);
            if (synapsesPropertyPanel != null)
                addTab(
                        "Input-Synapses",
                        "Shows the Input-Synapses",
                        synapsesPropertyPanel
                );
        }
        {
            AbstractPropertyPanel synapsesPropertyPanel = SynapsesPropertyPanel.create(act, Direction.OUTPUT, false);
            if (synapsesPropertyPanel != null)
                addTab(
                        "Output-Synapses",
                        "Shows the Output-Synapses",
                        synapsesPropertyPanel
                );
        }
        {
            AbstractPropertyPanel synapsesPropertyPanel = SynapsesPropertyPanel.create(act, Direction.OUTPUT, true);
            if (synapsesPropertyPanel != null)
                addTab(
                        "PreAct Output-Synapses",
                        "Shows the PreAct Output-Synapses",
                        synapsesPropertyPanel
                );
        }
    }
}
