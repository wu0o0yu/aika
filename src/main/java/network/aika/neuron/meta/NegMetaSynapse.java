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
package network.aika.neuron.meta;

import network.aika.Model;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.excitatory.ExcitatoryNeuron;

/**
 *
 * @author Lukas Molzberger
 */
public class NegMetaSynapse extends MetaSynapse {

    public static final String TYPE_STR = Model.register("SNM", NegMetaSynapse.class);


    public NegMetaSynapse() {
        super();
    }

    public NegMetaSynapse(Neuron input, Neuron output, boolean recurrent, int lastCount) {
        super(input, output, recurrent, false, lastCount);
    }


    @Override
    public String getType() {
        return TYPE_STR;
    }


    public static class Builder extends Synapse.Builder {

        public Synapse getSynapse(Neuron outputNeuron) {
            NegMetaSynapse s = (NegMetaSynapse) super.getSynapse(outputNeuron);

            return s;
        }

        protected Synapse.SynapseFactory getSynapseFactory() {
            return (input, output) -> new NegMetaSynapse(input, output, recurrent, output.getModel().charCounter);
        }
    }
}
