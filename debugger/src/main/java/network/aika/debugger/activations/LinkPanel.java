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

import network.aika.debugger.AbstractConsoleManager;
import network.aika.debugger.ElementPanel;
import network.aika.debugger.properties.AbstractPropertyPanel;
import network.aika.elements.links.Link;

import javax.swing.*;


/**
 * @author Lukas Molzberger
 */
public class LinkPanel extends ElementPanel {


    public LinkPanel(Link l) {

        //The following line enables to use scrolling tabs.
        setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

        setFocusCycleRoot(true);

        initTabs(l);
    }

    private void initTabs(Link l) {
        if(l == null)
            return;

        AbstractPropertyPanel actPropertyPanel = AbstractPropertyPanel.create(l);
        actPropertyPanel.addFinal();
        addTab(
                "Link",
                "Shows the Link",
                actPropertyPanel
        );

        AbstractPropertyPanel neuronPropertyPanel = AbstractPropertyPanel.createNeuralElement(l);
        neuronPropertyPanel.addFinal();
        addTab(
                "Synapse",
                "Shows the Neuron or Synapse",
                neuronPropertyPanel
        );
    }

    protected void setConsoleManager(AbstractConsoleManager cm) {
        super.setConsoleManager(cm);

        setSelectedIndex(consoleManager.getSelectedLinkPanelTab());
        addChangeListener(e -> {
            JTabbedPane sourceTabbedPane = (JTabbedPane) e.getSource();
            consoleManager.setSelectedLinkPanelTab(
                    sourceTabbedPane.getSelectedIndex()
            );
        });
    }

    public void remove() {
    }
}
