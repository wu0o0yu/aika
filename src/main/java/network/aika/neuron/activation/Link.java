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

/**
 *
 * @author Lukas Molzberger
 */
public class Link {

    private final Synapse synapse;

    private Activation input;
    private Activation output;

    public Link(Synapse s, Activation input, Activation output) {
        this.synapse = s;
        this.input = input;
        this.output = output;
    }

    public static void link(Synapse s, Activation input, Activation output) {
        Link l = new Link(s, input, output);
        input.getThought().add(l);
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

    public Activation getActivation(Direction dir) {
        switch(dir) {
            case INPUT:
                return getInput();
            case OUTPUT:
                return getOutput();
        }

        return null;
    }

    public boolean isNegative() {
        return synapse.isNegative();
    }

    public boolean isRecurrent() {
        return synapse != null && synapse.isRecurrent();
    }

    public boolean isConflict() {
        return isRecurrent() && isNegative();
    }

    public boolean isSelfRef() {
        return output == input.inputLinksFiredOrder.firstEntry().getValue().input;
    }

    public void link() {
        if(input != null) {
            input.outputLinks.put(output, this);
            output.inputLinksFiredOrder.put(this, this);
        }
        Link ol = output.inputLinks.put(synapse, this);
        if(ol != null && ol != this) {
            output.inputLinksFiredOrder.remove(ol);
            ol.input.outputLinks.remove(ol.output);
        }
    }

    public void unlink() {
        input.outputLinks.remove(input);
    }

    public String toString() {
        return synapse + ": " + input + " --> " + output;
    }

    public void process() {
        if (output.isFinal() && !isSelfRef()) {
            output = isConflict() ?
                    output.createBranch() :
                    output.createUpdate();
        }

        output.addLink(this);
    }
}
