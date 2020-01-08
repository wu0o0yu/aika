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
package network.aika.neuron.excitatory;

import network.aika.Model;
import network.aika.neuron.*;
import network.aika.neuron.meta.MetaNeuron;
import network.aika.neuron.meta.MetaSynapse;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;


/**
 *
 * @author Lukas Molzberger
 */
public class ExcitatorySynapse extends ConjunctiveSynapse<TNeuron, ConjunctiveNeuron> {

    public static final String TYPE_STR = Model.register("SE", ExcitatorySynapse.class);


    public static final Comparator<Synapse> META_SYNAPSE_COMP = Comparator
            .<Synapse, Neuron>comparing(Synapse::getPInput)
            .thenComparing(Synapse::getPOutput);


    public Map<MetaSynapse, MetaSynapse.MappingLink> metaSynapses = new TreeMap<>(META_SYNAPSE_COMP);

    public ExcitatorySynapse() {
        super();
    }

    public ExcitatorySynapse(Neuron input, Neuron output, boolean recurrent, boolean propagate) {
        super(input, output, recurrent, propagate);
    }



    public ExcitatorySynapse(Neuron input, Neuron output, boolean recurrent, boolean propagate, int lastCount) {
        super(input, output, recurrent, propagate, lastCount);
    }


    @Override
    public String getType() {
        return TYPE_STR;
    }

/*
    public MetaSynapse.MappingLink getMetaSynapse(Neuron in, Neuron out) {
        return metaSynapses.get(new MetaSynapse(in, out, -1, 0));
    }


    public double getUncovered() {
        double max = 0.0;
        for(Map.Entry<MetaSynapse, MetaSynapse.MappingLink> me: metaSynapses.entrySet()) {
            MetaSynapse.MappingLink sml = me.getValue();
            MetaSynapse ms = sml.metaSynapse;
            MetaNeuron mn = ms.getOutput();
            ExcitatoryNeuron tn = (ExcitatoryNeuron) getOutput();
            MetaNeuron.MappingLink nml = mn.targetNeurons.get(tn);

            max = Math.max(max, nml.nij * ms.getCoverage());
        }

        return 1.0 - max;
    }



    public boolean isMappedToMetaSynapse(MetaSynapse metaSyn) {
        MetaSynapse.MappingLink ml = metaSynapses.get(metaSyn.getOutput());
        return ml.metaSynapse == metaSyn;
    }
*/

    @Override
    public void write(DataOutput out) throws IOException {
        super.write(out);
    }


    @Override
    public void readFields(DataInput in, Model m) throws IOException {
        super.readFields(in, m);
    }


    public static class Builder extends Synapse.Builder {

        public Synapse getSynapse(Neuron outputNeuron) {
            ExcitatorySynapse s = (ExcitatorySynapse) super.getSynapse(outputNeuron);

            return s;
        }

        protected SynapseFactory getSynapseFactory() {
            return (input, output) -> new ExcitatorySynapse(input, output, recurrent, propagate, output.getModel().charCounter);
        }
    }

}
