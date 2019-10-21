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
package network.aika.training.meta;


import network.aika.Document;
import network.aika.Model;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.link.Link;
import network.aika.neuron.relation.PositionRelation;
import network.aika.neuron.relation.Relation;
import network.aika.training.MetaModel;
import network.aika.training.TDocument;
import network.aika.training.TNeuron;
import network.aika.training.TSynapse;
import network.aika.training.excitatory.ExcitatoryNeuron;
import network.aika.training.excitatory.ExcitatorySynapse;
import network.aika.training.relation.WeightedRelation;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;


/**
 *
 * @author Lukas Molzberger
 */
public class MetaSynapse extends TSynapse {

    public boolean isMetaVariable;

    Map<ExcitatoryNeuron, MappingLink> targetSynapses = new TreeMap<>();


    public MetaSynapse(Neuron input, Neuron output, Integer id, int lastCount) {
        super(input, output, id, lastCount);
    }


    public Integer getMetaSynapseId() {
        return getId();
    }


    public void updateWeight() {
        double sum = 0.0;
        double norm = 0.0;

        Map<Relation, Double> outputRelations = new TreeMap<>();

        for(MappingLink sml: targetSynapses.values()) {
            MetaNeuron mn = (MetaNeuron) sml.metaSynapse.getOutput().get();
            ExcitatoryNeuron tn = (ExcitatoryNeuron) sml.targetSynapse.getOutput().get();
            MetaNeuron.MappingLink nml = mn.targetNeurons.get(tn);

            double nij = nml.nij;
            double wjl = sml.targetSynapse.getWeight();

            sum += nij * wjl;
            norm += nij;

            countTargetRelations(outputRelations, sml, nij);
        }

        update(null, sum / norm, 1.0);
    }


    public void countTargetRelations(Map<Relation, Double> outputRelations, MappingLink sml, double nij) {
        for(Relation.Key rk : sml.targetSynapse.getOutputRelations()) {
            Relation rel = rk.getRelation();

            if (rel instanceof WeightedRelation) {
                rel = ((WeightedRelation) rel).keyRelation;
            }

            if (rel instanceof PositionRelation) {
                countRelation(outputRelations, rel, nij);
            }
        }
    }


    private void countRelation(Map<Relation, Double> outputRelations, Relation rel, double w) {
        Double ow = outputRelations.get(rel);
        if(ow == null) {
            ow = 0.0;
        }
        outputRelations.put(rel, ow + w);
    }


    public void transferTemplateSynapse(Document doc, TNeuron inputNeuron, ExcitatoryNeuron targetNeuron, Link metaLink) {
        if(metaLink.getTargetSynapse() != null) {
            return;
        }

        ExcitatorySynapse targetSynapse = targetNeuron.createOrLookupSynapse(doc, this, inputNeuron.getProvider());

        targetSynapse.setRecurrent(isRecurrent());
        targetSynapse.setIdentity(isIdentity());

        new MappingLink(this, targetSynapse).link();

        targetSynapse.updateDelta(
                doc,
                getWeight(),
                getLimit()
        );

        System.out.println("  Transfer Template Synapse: IN:" +
                inputNeuron.getLabel() +
                " OUT:" + targetNeuron.getLabel() +
                " M-SynId:" + getId() +
                " T-SynId:" + targetSynapse.getId() +
                " W:" + targetSynapse.getNewWeight() +
                " Rec:" + targetSynapse.isRecurrent() +
                " Ident:"  + targetSynapse.isIdentity()
        );

        metaLink.setTargetSynapse(targetSynapse);
    }


    @Override
    public void write(DataOutput out) throws IOException {
        out.writeBoolean(isMetaVariable);
    }

    @Override
    public void readFields(DataInput in, Model m) throws IOException {
        isMetaVariable = in.readBoolean();
    }


    /**
     *
     * @author Lukas Molzberger
     */
    public static class Builder extends Synapse.Builder {

        public boolean isMetaVariable;


        public Builder setIsMetaVariable(boolean isMetaVariable) {
            this.isMetaVariable = isMetaVariable;
            return this;
        }

        public Synapse getSynapse(Neuron outputNeuron) {
            MetaSynapse s = (MetaSynapse) super.getSynapse(outputNeuron);

            s.isMetaVariable = isMetaVariable;

            return s;
        }

        protected SynapseFactory getSynapseFactory() {
            return (input, output, id) -> new MetaSynapse(input, output, id, ((MetaModel) output.getModel()).charCounter);
        }
    }


    public static class MappingLink {
        public MetaSynapse metaSynapse;
        public ExcitatorySynapse targetSynapse;

        public MappingLink(MetaSynapse metaSynapse, ExcitatorySynapse targetSynapse) {
            this.metaSynapse = metaSynapse;
            this.targetSynapse = targetSynapse;
        }

        public void link() {
            metaSynapse.targetSynapses.put((ExcitatoryNeuron) targetSynapse.getOutput().get(), this);
            targetSynapse.metaSynapses.put(metaSynapse, this);
        }
    }
}
