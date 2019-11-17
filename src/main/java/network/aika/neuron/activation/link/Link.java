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
package network.aika.neuron.activation.link;

import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.TSynapse;

import java.util.Comparator;

import static network.aika.neuron.activation.link.Direction.INPUT;
import static network.aika.neuron.activation.link.Direction.OUTPUT;

/**
 *
 * @author Lukas Molzberger
 */
public class Link {
    private final Synapse synapse;
    private TSynapse targetSynapse;

    private final Activation input;
    private final Activation output;

    public static Comparator<Link> INPUT_COMP = (l1, l2) -> {
        int r = Synapse.INPUT_SYNAPSE_COMP.compare(l1.synapse, l2.synapse);
        if (r != 0) return r;
        return Integer.compare(l1.input.getId(), l2.input.getId());
    };

    public static Comparator<Link> OUTPUT_COMP = (l1, l2) -> {
        int r = Synapse.OUTPUT_SYNAPSE_COMP.compare(l1.synapse, l2.synapse);
        if (r != 0) return r;
        return Integer.compare(l1.output.getId(), l2.output.getId());
    };


    public Link(Synapse s, Activation input, Activation output) {
        this.synapse = s;
        this.targetSynapse = null;
        this.input = input;
        this.output = output;
    }


    public Link(Synapse s, TSynapse targetSynapse, Activation input, Activation output) {
        this.synapse = s;
        this.targetSynapse = targetSynapse;
        this.input = input;
        this.output = output;
    }


    public Synapse getSynapse() {
        return synapse;
    }


    public TSynapse getTargetSynapse() {
        return targetSynapse;
    }

    public void setTargetSynapse(TSynapse targetSynapse) {
        this.targetSynapse = targetSynapse;
    }


    public Activation getInput() {
        return input;
    }


    public Activation getOutput() {
        return output;
    }


    public boolean isNegative(Synapse.State s) {
        return synapse.isNegative(s);
    }


    public boolean isInactive() {
        return synapse.isInactive();
    }


    public void link() {
        input.addLink(INPUT, this);
        output.addLink(OUTPUT, this);
    }


    public String toString() {
        return synapse + ": " + input + " --> " + output;
    }
}


