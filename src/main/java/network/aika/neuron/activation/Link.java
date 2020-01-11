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
package network.aika.neuron.activation;

import network.aika.neuron.Synapse;
import network.aika.neuron.TSynapse;

import static network.aika.neuron.Synapse.State.CURRENT;


/**
 *
 * @author Lukas Molzberger
 */
public class Link {

    final Synapse synapse;

    final Activation input;
    final Activation output;


    public Link(Synapse s, Activation input, Activation output) {
        this.synapse = s;
        this.input = input;
        this.output = output;
    }


    public Synapse getSynapse() {
        return synapse;
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


    public boolean isRecurrent() {
        return synapse.isRecurrent();
    }


    public boolean isConflict() {
        return isRecurrent() && isNegative(CURRENT);
    }


    public void link() {
        input.outputLinks.put(output, this);
        output.inputLinks.put(input.getNeuron(), this);
        output.inputLinksFiredOrder.put(input, this);
    }


    public String toString() {
        return synapse + ": " + input + " --> " + output;
    }

}
