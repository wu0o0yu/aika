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
package network.aika.debugger.activations.properties;

import network.aika.debugger.properties.AbstractPropertyPanel;
import network.aika.enums.direction.Direction;
import network.aika.elements.Element;
import network.aika.elements.activations.Activation;
import network.aika.elements.links.Link;
import network.aika.elements.neurons.Neuron;

import java.util.stream.Stream;


/**
 * @author Lukas Molzberger
 */
public class LinksPropertyPanel extends AbstractPropertyPanel {

    public LinksPropertyPanel(Stream<? extends Link> links) {
        links
                .limit(10)
                .forEach(l -> {
                    addEntry(LinkPropertyPanel.create(l));
                    addSeparator();
                }
        );

        addFinal();
    }

    public static LinksPropertyPanel create(Element element, Direction dir) {
        Activation act = (Activation) element;
        Neuron n = act.getNeuron();

        if(dir == Direction.INPUT)
            return new LinksPropertyPanel(act.getInputLinks());
        else
            return new LinksPropertyPanel(act.getOutputLinks());
    }
}