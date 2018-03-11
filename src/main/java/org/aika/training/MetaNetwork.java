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
package org.aika.training;


import org.aika.corpus.SearchNode;
import org.aika.neuron.Synapse;
import org.aika.Model;
import org.aika.neuron.Neuron;
import org.aika.corpus.Document;
import org.aika.neuron.Activation;
import org.aika.neuron.Activation.SynapseActivation;
import org.aika.neuron.INeuron;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static org.aika.corpus.SearchNode.Decision.EXCLUDED;
import static org.aika.corpus.SearchNode.Decision.UNKNOWN;


/**
 * Meta-neurons and meta-synapses allow to generate new fully trained neurons based on a single training data set.
 * The meta-network employs an inhibitory feedback loop to determine if a certain information is already known (i.e.
 * there is already a neuron representing it) or if it is new knowledge that should be represented by a new neuron. If
 * there is already a neuron that represents this information, then the meta-neuron will be suppressed by the
 * feedback loop. Otherwise the meta-neuron will be activated which means a copy of the meta-neuron will be generated
 * using only the meta information of this neuron. The meta-synapses are only added to this new neuron if the input
 * neuron has beed active in the training data set as well. If the input neuron of the meta-synapse is another
 * inhibitory neuron, then the resulting synapse of the new neuron is going to be connected to the activated input
 * neuron of this inhibitory neuron.
 *
 * @author Lukas Molzberger
 */
public class MetaNetwork {

    private static final Logger log = LoggerFactory.getLogger(MetaNetwork.class);

    public static void train(Document doc) {
        long v = doc.visitedCounter++;

        Map<Activation, Target> metaActivations = new TreeMap<>();

        List<INeuron> inhibitoryNeurons = doc.finallyActivatedNeurons
                .stream()
                .filter(n -> n.type == INeuron.Type.INHIBITORY)
                .collect(Collectors.toList());

        for (INeuron n : inhibitoryNeurons) {
            for (Activation inhibAct : n.getFinalActivations(doc)) {
                for (SynapseActivation sa : inhibAct.getFinalInputActivations()) {
                    Activation act = sa.input;
                    Neuron targetNeuron = act.getNeuron();

                    doc.createV = doc.visitedCounter++;

                    boolean newNeuron = false;
                    if (targetNeuron.get().type == INeuron.Type.META) {
                        newNeuron = true;
                        targetNeuron = doc.model.createNeuron(n.label.substring(2) + "-" + doc.getText(act.key.range));
                        INeuron.update(doc.model, doc.threadId, doc, targetNeuron, n.bias, Collections.emptySet());
                    }

                    Activation metaNeuronAct = getMetaNeuronAct(inhibAct);
                    if (metaNeuronAct != null && metaNeuronAct.visited != v) {
                        metaNeuronAct.visited = v;
                        metaActivations.put(metaNeuronAct, new Target(targetNeuron, newNeuron, n.provider));
                    }
                }
            }
        }

        for(Map.Entry<Activation, Target> me: metaActivations.entrySet()) {
            transferMetaSynapses(doc, metaActivations, me.getKey(), me.getValue(), v);
        }
    }


    private static class Target {
        Neuron targetNeuron;
        boolean isNewNeuron;
        Neuron inhibNeuron;

        public Target(Neuron targetNeuron, boolean isNewNeuron, Neuron inhibNeuron) {
            this.targetNeuron = targetNeuron;
            this.isNewNeuron = isNewNeuron;
            this.inhibNeuron = inhibNeuron;
        }
    }


    private static Activation getMetaNeuronAct(Activation inhibAct) {
        for(SynapseActivation sa: inhibAct.neuronInputs) {
            if(sa.input.getINeuron().type == INeuron.Type.META) {
                return sa.input;
            }
        }
        return null;
    }


    private static boolean checkForNewlyCreatedActivations(Activation sAct) {
        for(SynapseActivation sa: sAct.neuronInputs) {
            if(sa.input.finalDecision == UNKNOWN) {
                return false;
            }
        }

        return true;
    }


    private static void transferMetaSynapses(Document doc, Map<Activation, Target> metaActivations, Activation metaAct, Target t, long v) {
        TreeSet<Synapse> inputSynapses = new TreeSet<>(Synapse.INPUT_SYNAPSE_COMP);

        Integer ridOffset = computeRidOffset(metaAct);
        for (SynapseActivation sa : metaAct.getFinalInputActivations()) {
            MetaSynapse ss = sa.synapse.meta;
            if (ss != null && (ss.metaWeight != 0.0 || ss.metaBias != 0.0)) {
                Neuron ina = sa.input.key.node.neuron;

                List<SynapseActivation> inputs = ina.get().type == INeuron.Type.INHIBITORY && ss.metaWeight >= 0.0 ?
                        sa.input.getFinalInputActivations() :
                        Collections.singletonList(sa);

                for(SynapseActivation isa: inputs) {
                    Neuron in = isa.input.getNeuron();
                    if(in.get(doc).type == INeuron.Type.META) {
                        Target it = metaActivations.get(isa.input);
                        in = it != null ? it.targetNeuron : null;
                    }

                    if(in != null) {
                        Integer rid = isa.input.key.rid;
                        Synapse.Key osk = sa.synapse.key;
                        Synapse.Key nsk = new Synapse.Key(
                                osk.isRecurrent,
                                osk.relativeRid != null ?
                                        osk.relativeRid :
                                        (ss.metaRelativeRid && ridOffset != null && rid != null ? rid - ridOffset : null),
                                osk.absoluteRid,
                                osk.rangeMatch,
                                osk.rangeOutput
                        );

                        Synapse ns = new Synapse(in, t.targetNeuron, nsk);
                        if (!ns.exists()) {
                            ns.updateDelta(doc, ss.metaWeight, ss.metaBias);

                            inputSynapses.add(ns);
                        }
                    }
                }
            }
        }

        if(log.isDebugEnabled()) {
            log.debug(showDelta(t.targetNeuron.get(), inputSynapses));
        }

        INeuron.update(doc.model, doc.threadId, doc, t.targetNeuron, t.isNewNeuron ? metaAct.getINeuron().metaBias : 0.0, inputSynapses);

        if (t.isNewNeuron) {
            SynapseActivation inhibMetaLink = metaAct.getFinalOutputActivations().get(0);
            Synapse.Key inhibSynKey = inhibMetaLink.synapse.key;
            MetaSynapse inhibSS = inhibMetaLink.synapse.meta;
            t.inhibNeuron.addSynapse(
                    new Synapse.Builder()
                            .setNeuron(t.targetNeuron)
                            .setWeight(inhibSS.metaWeight)
                            .setBias(inhibSS.metaBias)
                            .setRelativeRid(inhibSynKey.relativeRid)
                            .setAbsoluteRid(inhibSynKey.absoluteRid)
                            .setRangeMatch(inhibSynKey.rangeMatch)
                            .setRangeOutput(inhibSynKey.rangeOutput)
            );
        }

        doc.propagate();
    }


    public static String showDelta(INeuron n, Set<Synapse> synapses) {
        StringBuilder sb = new StringBuilder();

        sb.append("N: " + n.label + " ob:" + n.biasSum + " nb:" + (n.biasSum + n.biasSumDelta) + "\n");
        for (Synapse s : synapses) {
            if (s.weightDelta != 0.0) {
                Integer f = null;
                if (s.input.get().statistic != null) {
                    f = ((NeuronStatistic) s.input.get().statistic).frequency;
                }

                sb.append("    S:" + s.input.getLabel() + " ow:" + s.weight + " nw:" + s.getNewWeight() + (f != null ? " f:" + f : "") + " " + (s.isConjunction(false, false) ? "CONJ" : "DISJ") + "\n");
            }
        }
        return sb.toString();
    }



    private static Integer computeRidOffset(Activation mAct) {
        for (Activation.SynapseActivation sa : mAct.getFinalInputActivations()) {
            if(sa.synapse.key.relativeRid != null && sa.input.key.rid != null) {
                return sa.input.key.rid - sa.synapse.key.relativeRid;
            }
        }
        return null;
    }


    public static Neuron initMetaNeuron(Model m, Neuron n, double bias, double metaBias, Synapse.Builder... inputs) {
        n.get().metaBias = metaBias;
        return Neuron.init(n, bias, INeuron.Type.META, inputs);
    }
}
