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

import network.aika.Thought;
import network.aika.debugger.neurons.properties.SynapsePropertyPanel;
import network.aika.debugger.properties.AbstractPropertyPanel;
import network.aika.direction.Direction;
import network.aika.elements.Element;
import network.aika.elements.activations.Activation;
import network.aika.elements.neurons.Neuron;
import network.aika.elements.neurons.PreActivation;
import network.aika.elements.synapses.Synapse;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * @author Lukas Molzberger
 */
public class SynapsesPropertyPanel extends AbstractPropertyPanel {

    public SynapsesPropertyPanel(Stream<? extends Synapse> synapses) {
        List<? extends Synapse> sortedSynapses = synapses.toList();

        Collections.sort(sortedSynapses, Comparator.comparingDouble(s -> -s.getSortingWeight()));
        sortedSynapses.stream()
                .limit(10)
                .forEach(s -> {
                    addEntry(SynapsePropertyPanel.create(s));
                    addSeparator();
                }
        );

        addFinal();
    }

    public static SynapsesPropertyPanel create(Element element, Direction dir, boolean preActSyns) {
        Thought t = null;
        Neuron n = null;
        if(element instanceof Activation<?>) {
            Activation act = (Activation) element;
            t = act.getThought();
            n = act.getNeuron();
        } else if(element instanceof Neuron<?>) {
            n = (Neuron) element;
        }

        if(n == null)
            return null;

        if(dir == Direction.INPUT)
            return new SynapsesPropertyPanel(n.getInputSynapsesAsStream());
        else if(!preActSyns)
            return new SynapsesPropertyPanel(n.getOutputSynapsesAsStream());
        else {
            PreActivation<?> preAct = n.getPreActivation(t);
            if(preAct == null)
                return null;

            return new SynapsesPropertyPanel(preAct.getOutputSynapses());
        }
    }
}