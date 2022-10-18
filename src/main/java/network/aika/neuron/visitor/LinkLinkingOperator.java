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
package network.aika.neuron.visitor;

import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.ConjunctiveLink;
import network.aika.neuron.activation.Link;
import network.aika.neuron.conjunctive.ConjunctiveSynapse;
import network.aika.neuron.visitor.linking.LinkingCallback;
import network.aika.neuron.visitor.linking.LinkingOperator;

/**
 * @author Lukas Molzberger
 */
public class LinkLinkingOperator extends LinkingOperator {


    public LinkLinkingOperator(Activation fromBS, ConjunctiveSynapse syn) {
        super(fromBS, syn);
    }

    @Override
    public void check(LinkingCallback v, Link lastLink, Activation act) {
        if(act.getNeuron() != syn.getOutput())
            return;

        if(act == fromBS)
            return;

        ConjunctiveLink<?, ?, ?> l = (ConjunctiveLink) lastLink;
        if(!v.compatible(syn.getScope(), l.getSynapse().getScope()))
            return;

        link(l.getInput(), l.getSynapse(), l, fromBS, syn);
    }
}
