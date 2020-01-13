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

import network.aika.Document;
import network.aika.neuron.INeuron;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.inhibitory.InhibitoryNeuron;

import java.util.*;
import java.util.stream.Collectors;

import static network.aika.neuron.activation.Activation.INPUT_COMP;


/**
 *
 * @author Lukas Molzberger
 */
public class Linker {

    private static class Entry {
        Activation act;
        NavigableSet<Link> candidates;


        public Entry(Activation act, NavigableSet<Link> candidates) {
            this.act = act;
            this.candidates = candidates;
        }
    }


    public void linkForward(Activation act) {
        Document doc = act.getDocument();

        TreeSet<Neuron> propagationTargets = new TreeSet(act.getINeuron().getPropagationTargets());

        if(act.lastRound != null) {
            act.lastRound.outputLinks
                    .values()
                    .stream()
                    .forEach(l -> {
                        addAndProcess(l.synapse, act, l.output);
                        propagationTargets.remove(l.output.getINeuron());
                    });
        }

        act.followDown(doc.getNewVisitedId(), cAct -> {
            if(cAct.getINeuron() instanceof InhibitoryNeuron) return false;

            Synapse s = act.getNeuron().getOutputSynapse(cAct.getNeuron());
            if(s == null || !act.outputLinks.containsKey(cAct.getNeuron())) {
                return false;
            }

            addAndProcess(s, act, cAct);
            propagationTargets.remove(cAct.getNeuron());

            return false;
        });

        propagationTargets
                .stream()
                .map(n -> n.get().getProvider())
                .map(n -> act.getNeuron().getOutputSynapse(n))
                .forEach(s -> addAndProcess(s, act, lookupNewActivation(doc, s.getOutput(), null)));
    }


    private Activation lookupNewActivation(Document doc, INeuron n, Activation oldAct) {
        if(oldAct == null) {
            return new Activation(doc, n, null, 0);
        } else if(oldAct.nextRound != null) {
            return oldAct.nextRound;
        } else {
            return oldAct.cloneAct(false);
        }
    }


    private void addAndProcess(Synapse s, Activation input, Activation output) {
        Link l = new Link(s, input, output);

        ArrayDeque<Entry> queue = new ArrayDeque<>();

        NavigableSet<Link> candidates = new TreeSet<>(INPUT_COMP);
        candidates.add(l);
        queue.add(new Entry(l.output, candidates));

        while (!queue.isEmpty()) {
            Entry entry = queue.pollFirst();

            Activation targetAct = entry.act;

            Link cand = entry.candidates.pollFirst();
            targetAct = targetAct.addLink(cand);

            NavigableSet<Link> newCandidates = findLinkingCandidates(targetAct, cand, candidates);

            if (!newCandidates.isEmpty()) {
                Entry newEntry = new Entry(targetAct, newCandidates);
                queue.addLast(newEntry);
            }
        }
    }


    public NavigableSet<Link> findLinkingCandidates(Activation targetAct, Link l, NavigableSet<Link> candidates) {
        if(((targetAct.getINeuron() instanceof InhibitoryNeuron) || l.isConflict())) return candidates;

        NavigableSet<Link> newCandidates = new TreeSet<>(candidates);
        l.input.followDown(l.input.getDocument().getNewVisitedId(), act -> {
            Synapse is = targetAct.getNeuron().getInputSynapse(act.getNeuron());
            if (is == null) {
                return false;
            }

            List<Link> ols = act
                    .getOutputLinks(is)
                    .filter(la -> la.output == targetAct)
                    .collect(Collectors.toList());
            if (ols.isEmpty()) {
                ols.add(new Link(is, act, targetAct));
            }
            newCandidates.addAll(ols);

            return false;
        });

        return newCandidates;
    }

}
