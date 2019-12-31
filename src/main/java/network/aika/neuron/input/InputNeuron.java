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
package network.aika.neuron.input;

import network.aika.ActivationFunction;
import network.aika.Model;
import network.aika.Config;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.TNeuron;
import network.aika.neuron.activation.Fired;
import network.aika.neuron.excitatory.ExcitatoryNeuron;

import java.util.Collection;


/**
 *
 * @author Lukas Molzberger
 */
public class InputNeuron extends TNeuron<Synapse> {

    public static final String TYPE_STR = Model.register("NIN", InputNeuron.class);


    private InputNeuron() {
        super();
    }


    public InputNeuron(Neuron p) {
        super(p);
    }


    public InputNeuron(Model model, String label) {
        super(model, label);
    }


    @Override
    public Fired incrementFired(Fired f) {
        return f;
    }


    public boolean isWeak(Synapse s, Synapse.State state) {
        return false;
    }


    public String getType() {
        return TYPE_STR;
    }

    @Override
    public ActivationFunction getActivationFunction() {
        return ActivationFunction.NULL_FUNCTION;
    }


    @Override
    public void commit(Collection<? extends Synapse> modifiedSynapses) {

    }

    @Override
    public double getTotalBias(Synapse.State state) {
        return 0;
    }

    @Override
    public void dumpStat() {
        System.out.println(getLabel() + "  Freq:(" + freqToString() + ")  P(" + propToString() + ")  Rel:" + getReliability());
    }


    public boolean isMature(Config c) {
        return posFrequency > c.getMaturityThreshold();
    }


    public String typeToString() {
        return "INPUT";
    }

}
