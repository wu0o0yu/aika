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
package network.aika.lattice;

import network.aika.Document;
import network.aika.neuron.INeuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.relation.Relation;

import java.util.*;

/**
 * Converts the synapse weights of a neuron into a boolean logic representation of this neuron.
 *
 * @author Lukas Molzberger
 */
public class Converter {

    public static int MAX_AND_NODE_SIZE = 10;


    public static Comparator<Synapse> SYNAPSE_COMP = (s1, s2) -> {
        int r = Boolean.compare(
                s2.linksAnyOutput() || s2.isIdentity(),
                s1.linksAnyOutput() || s1.isIdentity()
        );
        if (r != 0) return r;
        r = Double.compare(s2.weight, s1.weight);
        if (r != 0) return r;
        return Integer.compare(s1.getId(), s2.getId());
    };

    private int threadId;
    private INeuron neuron;
    private Document doc;
    private OrNode outputNode;
    private Collection<Synapse> modifiedSynapses;


    public static boolean convert(int threadId, Document doc, INeuron neuron, Collection<Synapse> modifiedSynapses) {
        return new Converter(threadId, doc, neuron, modifiedSynapses).convert();
    }


    private Converter(int threadId, Document doc, INeuron neuron, Collection<Synapse> modifiedSynapses) {
        this.doc = doc;
        this.neuron = neuron;
        this.threadId = threadId;
        this.modifiedSynapses = modifiedSynapses;
    }


    private boolean convert() {
        outputNode = neuron.node.get();

        initSlotFlags();

        INeuron.SynapseSummary ss = neuron.getSynapseSummary();

        if(ss.getBiasSum() + ss.getPosDirSum() + ss.getPosRecSum() <= 0.0) {
            outputNode.removeParents(threadId);
            return false;
        }

        List<Synapse> candidates = prepareCandidates();

        NodeContext nodeContext = null;
        boolean noFurtherRefinement = false;
        TreeSet<Synapse> reqSyns = new TreeSet<>(Synapse.INPUT_SYNAPSE_COMP);
        double sum = 0.0;

        if(ss.getNumDisjunctiveSynapses() == 0) {
            double remainingSum = ss.getPosDirSum();
            int i = 0;
            for (Synapse s : candidates) {
                double v = s.getMaxInputValue();
                final boolean isOptionalInput = sum + remainingSum - v + ss.getPosRecSum() + ss.getPosPassiveSum() + ss.getBiasSum() > 0.0;
                final boolean maxAndNodesReached = i >= MAX_AND_NODE_SIZE;
                if (isOptionalInput || maxAndNodesReached) {
                    break;
                }

                remainingSum -= v;
                reqSyns.add(s);

                NodeContext nlNodeContext = expandNode(nodeContext, s);
                if(nlNodeContext == null) {
                    break;
                }
                nodeContext = nlNodeContext;

                i++;

                sum += v;

                final boolean sumOfSynapseWeightsAboveThreshold = sum + ss.getPosRecSum() + ss.getPosPassiveSum() + ss.getBiasSum() > 0.0;
                if (sumOfSynapseWeightsAboveThreshold) {
                    noFurtherRefinement = true;
                    break;
                }
            }

            outputNode.removeParents(threadId);

            if (noFurtherRefinement || (i > 0 && (i == MAX_AND_NODE_SIZE || i == candidates.size()))) {
                outputNode.addInput(nodeContext.getSynapseIds(), threadId, nodeContext.node, true);
            } else {
                for (Synapse s : candidates) {
                    double v = s.getMaxInputValue();
                    boolean belowThreshold = sum + v + remainingSum + ss.getPosRecSum() + ss.getPosPassiveSum() + ss.getBiasSum() <= 0.0;
                    if (belowThreshold) {
                        break;
                    }

                    if (!reqSyns.contains(s)) {
                        NodeContext nlNodeContext = expandNode(nodeContext, s);
                        if(nlNodeContext != null) {
                            outputNode.addInput(nlNodeContext.getSynapseIds(), threadId, nlNodeContext.node, true);
                            remainingSum -= v;
                        }
                    }
                }
            }
        } else {
            for (Synapse s : modifiedSynapses) {
                if (s.isDisjunction() && !s.isRecurrent()) {
                    NodeContext nlNodeContext = expandNode(nodeContext, s);
                    outputNode.addInput(nlNodeContext.getSynapseIds(), threadId, nlNodeContext.node, false);
                }
            }
        }

        return true;
    }


    private void initSlotFlags() {
        modifiedSynapses.forEach(s -> {
            for(Integer slot: s.linksOutput()) {
                neuron.slotHasInputs.add(slot);
            }
            for(Relation rel: s.getRelations().values()) {
                rel.registerRequiredSlots(s.getInput());
            }
        });
    }


    private List<Synapse> prepareCandidates() {
        Synapse syn = getBestSynapse(neuron.inputSynapses.values());

        TreeSet<Integer> alreadyCollected = new TreeSet<>();
        ArrayList<Synapse> selectedCandidates = new ArrayList<>();
        TreeMap<Integer, Synapse> relatedSyns = new TreeMap<>();
        while(syn != null && selectedCandidates.size() < MAX_AND_NODE_SIZE) {
            relatedSyns.remove(syn.getId());
            selectedCandidates.add(syn);
            alreadyCollected.add(syn.getId());
            for(Map.Entry<Integer, Relation> me: syn.getRelations().entrySet()) {
                Integer relId = me.getKey();
                Relation rel = me.getValue();
                if(rel.isConvertible() && !alreadyCollected.contains(relId)) {
                    Synapse rs = syn.getOutput().getSynapseById(relId);
                    if(rs != null) {
                        relatedSyns.put(relId, rs);
                    }
                }
            }

            syn = getBestSynapse(relatedSyns.values());
        }

        return selectedCandidates;
    }


    private Synapse getBestSynapse(Collection<Synapse> synapses) {
        Synapse maxSyn = null;
        for(Synapse s: synapses) {
            if(!s.isNegative() && !s.isRecurrent() && !s.isInactive() && !s.getInput().get().isPassiveInputNeuron()) {
                if(maxSyn == null || SYNAPSE_COMP.compare(maxSyn, s) > 0) {
                    maxSyn = s;
                }
            }
        }
        return maxSyn;
    }


    private NodeContext expandNode(NodeContext nc, Synapse s) {
        if (nc == null) {
            NodeContext nln = new NodeContext();
            nln.node = s.getInput().get().outputNode.get();
            nln.offsets = new Synapse[] {s};
            return nln;
        } else {
            Relation[] relations = new Relation[nc.offsets.length];
            for(int i = 0; i < nc.offsets.length; i++) {
                Synapse linkedSynapse = nc.offsets[i];
                relations[i] = s.getRelationById(linkedSynapse.getId());
            }

            NodeContext nln = new NodeContext();
            nln.offsets = new Synapse[nc.offsets.length + 1];
            AndNode.Refinement ref = new AndNode.Refinement(new AndNode.RelationsMap(relations), s.getInput().get().outputNode);
            AndNode.RefValue rv = nc.node.extend(threadId, doc, ref);
            if(rv == null) {
                return null;
            }

            nln.node = rv.child.get(doc);

            for (int i = 0; i < nc.offsets.length; i++) {
                nln.offsets[rv.offsets[i]] = nc.offsets[i];
            }
            for (int i = 0; i < nln.offsets.length; i++) {
                if (nln.offsets[i] == null) {
                    nln.offsets[i] = s;
                }
            }
            return nln;
        }
    }


    private class NodeContext {
        Node node;

        Synapse[] offsets;

        int[] getSynapseIds() {
            int[] result = new int[offsets.length];
            for(int i = 0; i < result.length; i++) {
                result[i] = offsets[i].getId();
            }
            return result;
        }
    }
}
