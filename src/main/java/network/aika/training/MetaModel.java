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
package network.aika.training;


import network.aika.Document;
import network.aika.Model;
import network.aika.neuron.INeuron;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.relation.PositionRelation;
import network.aika.neuron.relation.Relation;
import network.aika.training.inhibitory.InhibitoryNeuron;
import network.aika.training.inhibitory.MetaInhibSynapse;
import network.aika.training.meta.MetaNeuron;
import network.aika.training.meta.MetaSynapse;
import network.aika.training.relation.WeightedRelation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static network.aika.neuron.Synapse.OUTPUT;
import static network.aika.neuron.activation.Activation.BEGIN;
import static network.aika.neuron.activation.Activation.END;
import static network.aika.neuron.activation.search.SearchNode.COMPUTE_SOFT_MAX;


/**
 * Meta-neurons and meta-synapses allow to generateNeurons new fully trained neurons based on a single neuron data set.
 * The meta-network employs an inhibitory feedback loop to determine if a certain information is already known (i.e.
 * there is already a neuron representing it) or if it is new knowledge that should be represented by a new neuron. If
 * there is already a neuron that represents this information, then the meta-neuron will be suppressed by the
 * feedback loop. Otherwise the meta-neuron will be activated which means a copy of the meta-neuron will be generated
 * using only the meta information of this neuron. The meta-synapses are only added to this new neuron if the input
 * neuron has beed active in the neuron data set as well. If the input neuron of the meta-synapse is another
 * inhibitory neuron, then the resulting synapse of the new neuron is going to be connected to the activated input
 * neuron of this inhibitory neuron.
 *
 * @author Lukas Molzberger
 */
public class MetaModel extends Model {
    private static final Logger log = LoggerFactory.getLogger(MetaModel.class);

    public static boolean DEBUG = true;

    public int charCounter = 0;

    public MetaModel() {
        COMPUTE_SOFT_MAX = true;

        Document.ROUND_LIMIT = 1;
    }


    public MetaNeuron createMetaNeuron(String label) {
        MetaNeuron metaNeuron = new MetaNeuron(this, "M-" + label);
        InhibitoryNeuron inhibNeuron = new InhibitoryNeuron(this, "I-" + label);
        metaNeuron.setInhibitoryNeuron(inhibNeuron);

        return metaNeuron;
    }


    public void initMetaNeuron(MetaNeuron metaNeuron, double bias, double trainingBias, Relation inhibOutputRelation, Neuron.Builder... inputs) {
        InhibitoryNeuron inhibNeuron = metaNeuron.getInhibitoryNeuron();

        List<Neuron.Builder> inputsList = new ArrayList<>(Arrays.asList(inputs));

        inputsList.forEach(b -> b.registerSynapseIds(metaNeuron.getProvider()));

        Integer inhibSynId = metaNeuron.getNewSynapseId();
        inputsList.add(
                new MetaSynapse.Builder()
                        .setIsMetaVariable(false)
                        .setSynapseId(inhibSynId)
                        .setNeuron(inhibNeuron.getProvider())
                        .setWeight(-100.0)
                        .setRecurrent(true)
        );
        inputsList.add(
                new Relation.Builder()
                        .setFrom(inhibSynId)
                        .setTo(OUTPUT)
                        .setRelation(new Relation[] {
                                new WeightedRelation(new PositionRelation.LessThan(BEGIN, END, false), 1.0),
                                new WeightedRelation(new PositionRelation.GreaterThan(END, BEGIN, false, Integer.MAX_VALUE), 1.0)
                        })
        );

        metaNeuron.trainingBias = trainingBias;

        Neuron.init(metaNeuron.getProvider(), bias + trainingBias, inputsList);

        inhibSynId = inhibNeuron.getNewSynapseId();
        Neuron.init(inhibNeuron.getProvider(),
                new MetaInhibSynapse.Builder()
                        .setSynapseId(inhibSynId)
                        .setNeuron(metaNeuron.getProvider())
                        .setIdentity(true)
                        .setWeight(1.0),
                new Relation.Builder()
                        .setFrom(inhibSynId)
                        .setTo(OUTPUT)
                        .setRelation(inhibOutputRelation)
        );
    }


    public void dumpStat() {
        for(Neuron n: getActiveNeurons()) {
            if(n.getType() == INeuron.Type.INPUT) {
                TNeuron tn = (TNeuron) n.get();
                System.out.println(tn.getLabel() + "  Freq:(" + tn.freqToString() + ")  P(" + tn.propToString() + ")  Rel:" + tn.getReliability());
            }
        }

        for(Neuron n: getActiveNeurons()) {
            if(n.getType() == INeuron.Type.EXCITATORY && "DERIVED-FROM-(d)".equalsIgnoreCase(n.getLabel())) {
                TNeuron tn = (TNeuron) n.get();
                System.out.println("OUT:  " + tn.getLabel() + "  Freq:(" + tn.freqToString() + ")  P(" + tn.propToString() + ")");

                for(Synapse s: n.getActiveInputSynapses()) {
                    TSynapse ts = (TSynapse) s;

                    System.out.println("IN:  " + ts.getInput().getLabel());
                    System.out.println("     Freq:(" + ts.freqToString() + ")");
                    System.out.println("     PXi(" + ts.pXiToString() + ")");
                    System.out.println("     PXout(" + ts.pXoutToString() + ")");
                    System.out.println("     P(" + ts.propToString() + ")");
                    System.out.println("     Rel:" + ts.getReliability());
                }
            }
        }
        System.out.println();
    }


    public void dumpModel() {
        System.out.println();
        System.out.println("Dump Model:");
        for(Neuron n: getActiveNeurons()) {
            TNeuron tn = (TNeuron) n.get();
            System.out.println(tn.toStringWithSynapses());
            System.out.println();
        }
    }
}
