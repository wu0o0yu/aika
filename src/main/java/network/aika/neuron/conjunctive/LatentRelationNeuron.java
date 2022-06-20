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
package network.aika.neuron.conjunctive;

import network.aika.Model;
import network.aika.direction.Direction;
import network.aika.neuron.activation.BindingActivation;
import network.aika.neuron.bindingsignal.BindingSignal;
import network.aika.neuron.bindingsignal.State;


import java.util.stream.Stream;

/**
 *
 * @author Lukas Molzberger
 */
public class LatentRelationNeuron extends BindingNeuron {

    private int rangeBegin = -1;
    private int rangeEnd = -1;

    public LatentRelationNeuron() {
    }

    public LatentRelationNeuron(Model model, boolean addProvider) {
        super(model, addProvider);
    }


    public int getRangeBegin() {
        return rangeBegin;
    }

    public void setRangeBegin(int rangeBegin) {
        this.rangeBegin = rangeBegin;
    }

    public int getRangeEnd() {
        return rangeEnd;
    }

    public void setRangeEnd(int rangeEnd) {
        this.rangeEnd = rangeEnd;
    }

    @Override
    public BindingNeuron instantiateTemplate(boolean addProvider) {
        LatentRelationNeuron n = new LatentRelationNeuron(getModel(), addProvider);
        initFromTemplate(n);

        return n;
    }

    public Stream<BindingSignal> getRelatedBindingSignals(BindingSignal fromBS) {
        State fromState = fromBS.getLink() == null ?
                State.RELATED_SAME :
                State.RELATED_INPUT;
        State toState = fromBS.getLink() == null ?
                State.RELATED_INPUT :
                State.RELATED_SAME;

        Stream<BindingSignal> bindingSignals = super.getRelatedBindingSignals(fromBS);
        Stream<BindingSignal> relBindingSignals = fromBS.getThought().getRelatedBindingSignals(fromBS, rangeBegin, this);
        relBindingSignals = relBindingSignals.map(bs -> createLatentInstance(fromBS, fromState, bs, toState));

        bindingSignals = Stream.concat(
                bindingSignals,
                relBindingSignals
        );

        return bindingSignals;
    }

    private BindingSignal createLatentInstance(BindingSignal fromBS, State fromState, BindingSignal toBS, State toState) {
 /*       BindingActivation relAct = createActivation(fromBS.getThought());

        BindingSignal(fromBS, PrimaryInputSynapse*/
        return null;
    }
}