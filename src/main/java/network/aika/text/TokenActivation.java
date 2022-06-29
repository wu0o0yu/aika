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
package network.aika.text;

import network.aika.fields.Field;
import network.aika.fields.ValueSortedQueueField;
import network.aika.neuron.Range;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.*;
import network.aika.neuron.bindingsignal.BindingSignal;
import network.aika.neuron.conjunctive.PatternNeuron;
import network.aika.neuron.conjunctive.PrimaryInputSynapse;
import network.aika.neuron.conjunctive.text.TokenNeuron;

import static network.aika.neuron.bindingsignal.BSKey.createKey;
import static network.aika.neuron.bindingsignal.State.SAME;

/**
 *
 * @author Lukas Molzberger
 */
public class TokenActivation extends PatternActivation {

    private Range range;
    private Integer position;


    public TokenActivation(int id, Integer pos, int begin, int end, Document doc, TokenNeuron tokenNeuron) {
        super(id, doc, tokenNeuron);
        position = pos;
        range = new Range(begin, end);
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    protected Field initNet() {
        return new ValueSortedQueueField(this, "net", 0.0);
    }

    public boolean isInput() {
        return true;
    }

    @Override
    public Range getRange() {
        return range;
    }
}
