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
import network.aika.neuron.INeuron.SynapseSummary;
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
        r = Double.compare(s2.getWeight(), s1.getWeight());
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
        outputNode = neuron.getInputNode().get();

        initSlotFlags();

        SynapseSummary ss = neuron.getSynapseSummary();

        if(ss.getBiasSum() + ss.getPosDirSum() + ss.getPosRecSum() <= 0.0) {
            outputNode.removeParents(threadId);
            return false;
        }

        if(ss.getNumDisjunctiveSynapses() == 0) {
            convertConjunction();
        } else {
            convertDisjunction();
        }

        return true;
    }


    private void convertConjunction() {
        SynapseSummary ss = neuron.getSynapseSummary();

        outputNode.removeParents(threadId);

        List<Synapse> candidates = prepareCandidates();
        double sum = 0.0;
        NodeContext nodeContext = null;
        double remainingSum = ss.getPosDirSum();
        int i = 0;
        for (Synapse s : candidates) {
            double v = s.getMaxInputValue();
            final boolean isOptionalInput = sum + remainingSum - v + ss.getPosRecSum() + ss.getPosPassiveSum() + ss.getBiasSum() > 0.0;

            if (!isOptionalInput) {
                remainingSum -= v;

                NodeContext nlNodeContext = expandNode(nodeContext, s);
                if (nlNodeContext == null) {
                    break;
                }
                nodeContext = nlNodeContext;
                sum += v;
                i++;
            } else {
                boolean belowThreshold = sum + v + remainingSum + ss.getPosRecSum() + ss.getPosPassiveSum() + ss.getBiasSum() <= 0.0;
                if (belowThreshold) {
                    break;
                }

                NodeContext nlNodeContext = expandNode(nodeContext, s);
                if (nlNodeContext != null) {
                    outputNode.addInput(nlNodeContext.getSynapseIds(), threadId, nlNodeContext.node, true);
                    remainingSum -= v;
                }
            }

            final boolean sumOfSynapseWeightsAboveThreshold = sum + ss.getPosRecSum() + ss.getPosPassiveSum() + ss.getBiasSum() > 0.0;
            final boolean maxAndNodesReached = i >= MAX_AND_NODE_SIZE;
            if (sumOfSynapseWeightsAboveThreshold || maxAndNodesReached || i == candidates.size()) {
                outputNode.addInput(nodeContext.getSynapseIds(), threadId, nodeContext.node, true);
                return;
            }
        }
    }


    private void convertDisjunction() {
        for (Synapse s : modifiedSynapses) {
            if (s.isDisjunction() && !s.isRecurrent()) {
                NodeContext nlNodeContext = expandNode(null, s);
                outputNode.addInput(nlNodeContext.getSynapseIds(), threadId, nlNodeContext.node, false);
            }
        }
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
        Synapse syn = getStrongestSynapse(neuron.getInputSynapses());

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

            syn = getStrongestSynapse(relatedSyns.values());
        }

        return selectedCandidates;
    }


    private Synapse getStrongestSynapse(Collection<Synapse> synapses) {
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
            nln.node = s.getInput().get().getOutputNode().get();
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
            AndNode.Refinement ref = new AndNode.Refinement(new AndNode.RelationsMap(relations), s.getInput().get().getOutputNode());
            AndNode.RefValue rv = nc.node.expand(threadId, doc, ref);
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
