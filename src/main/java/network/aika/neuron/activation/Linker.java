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

    private ArrayDeque<Entry> queue = new ArrayDeque<>();

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
                        Activation nAct = lookupNewActivation(doc, null, l.output);
                        addAndProcess(l.synapse, act, nAct);
                        propagationTargets.remove(nAct.getNeuron());
                    });
        }

        act.followDown(doc.getNewVisitedId(), cAct -> {
            if(cAct.getINeuron() instanceof InhibitoryNeuron) return false;

            Synapse s = act.getNeuron().getOutputSynapse(cAct.getNeuron());
            if(s == null || cAct.inputLinks.containsKey(act.getNeuron())) {
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


    public void process() {
        while(!queue.isEmpty()) {
            Entry entry = queue.pollFirst();

            Activation targetAct = entry.act;

            Link cand = entry.candidates.pollFirst();
            Document doc = cand.input.getDocument();

            NavigableSet<Link> newCandidates = new TreeSet<>(entry.candidates);
            cand.input.followDown(doc.getNewVisitedId(), act -> {
                if (act == cand.input) {
                    return false;
                }

                Synapse is = entry.act.getNeuron().getInputSynapse(act.getNeuron());
                if (is == null) {
                    return false;
                }

                List<Link> ols = act
                        .getOutputLinks(is)
                        .filter(l -> l.output == entry.act)
                        .collect(Collectors.toList());
                if (ols.isEmpty()) {
                    ols.add(new Link(is, act, entry.act));
                }
                newCandidates.addAll(ols);

                return false;
            });

            targetAct.addLink(cand);

            if(!newCandidates.isEmpty()) {
                Entry newEntry = new Entry(targetAct, newCandidates);
                queue.addLast(newEntry);
            }
        }
    }


    private void addAndProcess(Synapse s, Activation input, Activation output) {
        Link l = new Link(s, input, output);
        if(!(l.output.getINeuron() instanceof InhibitoryNeuron) && !l.isConflict()) {
            add(l);
            process();
        } else {
            l.output.addLink(l);
        }
    }

    private void add(Link l) {
        NavigableSet<Link> candidates = new TreeSet<>(INPUT_COMP);

        candidates.add(l);

        queue.addLast(new Entry(l.output, candidates));
    }
}
