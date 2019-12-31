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
package network.aika.neuron.inhibitory;


import network.aika.Model;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.TNeuron;
import network.aika.neuron.TSynapse;
import network.aika.neuron.excitatory.ExcitatoryNeuron;


/**
 *
 * @author Lukas Molzberger
 */
public class InhibitorySynapse extends TSynapse<TNeuron, InhibitoryNeuron> {

    public static final String TYPE_STR = Model.register("SI", InhibitorySynapse.class);

    public InhibitorySynapse() {
        super();
    }

    public InhibitorySynapse(Neuron input, Neuron output, Integer id) {
        super(input, output, id, false, true);
    }

    @Override
    public String getType() {
        return TYPE_STR;
    }


    @Override
    public boolean storeOnInputSide() {
        return false;
    }

    @Override
    public boolean storeOnOutputSide() {
        return true;
    }


    public static class Builder extends Synapse.Builder {
        protected SynapseFactory getSynapseFactory() {
            return (input, output, id) -> new InhibitorySynapse(input, output, id);
        }
    }
}
