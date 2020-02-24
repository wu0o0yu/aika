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
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.inhibitory.InhibitoryNeuron;

import java.util.*;


/**
 *
 * @author Lukas Molzberger
 */
public class Linker {

    public void linkForward(Activation act, boolean processMode) {
        ArrayDeque<Entry> queue = new ArrayDeque<>();
        Document doc = act.getDocument();
        TreeSet<Neuron> propagationTargets = new TreeSet(act.getINeuron().getPropagationTargets());

        if(act.lastRound != null) {
            act.lastRound.outputLinks
                    .values()
                    .forEach(l -> {
                        new Entry(l.output)
                                .addCandidate(l.synapse, act)
                                .addToQueue(queue);
                        propagationTargets.remove(l.output.getNeuron());
                    });
            act.lastRound.unlink();
            act.lastRound = null;
        }

        act.followDown(doc.getNewVisitedId(), cAct -> {
            if(cAct.getINeuron() instanceof InhibitoryNeuron) return;

            Synapse s = act.getNeuron().getOutputSynapse(cAct.getNeuron());
            if(s == null || act.outputLinkExists(cAct.getINeuron())) return;

            new Entry(cAct)
                    .addCandidate(s, act)
                    .addToQueue(queue);
            propagationTargets.remove(cAct.getNeuron());
        });

        propagationTargets
                .stream()
                .map(n -> n.get().getProvider())
                .map(n -> act.getNeuron().getOutputSynapse(n))
                .forEach(s ->
                        new Entry(new Activation(doc, s.getOutput(), false, null, 0))
                                .addCandidate(s, act)
                                .addToQueue(queue)
                );

        process(queue, processMode);
    }

    private static class Entry {
        Activation act;
        NavigableSet<Link> candidates = new TreeSet<>(Comparator.
                <Link, Fired>comparing(l -> l.getInput().getFired())
                .thenComparing(l -> l.getSynapse().getInput())
        );

        private Entry() {
        }

        public Entry(Activation act) {
            this.act = act;
       }

        public Entry addCandidate(Synapse s, Activation input) {
            candidates.add(new Link(s, input, null));
            return this;
        }

        public Entry cloneEntry(boolean branch) {
            Entry ce = new Entry();
            ce.act = act.cloneAct(branch);
            candidates.forEach(l -> ce.addCandidate(l.synapse, l.input));
            return ce;
        }

        public void addToQueue(ArrayDeque<Entry> queue) {
            if (!candidates.isEmpty()) {
                queue.addLast(this);
            }
        }

        public String toString() {
            return "act:" + act;
        }
    }

    private void process(ArrayDeque<Entry> queue, boolean processMode) {
        while (!queue.isEmpty()) {
            Entry e = queue.pollFirst();
            Link l = e.candidates.pollFirst();

            if(e.act.isFinal && !l.isSelfRef()) {
                e.addToQueue(queue);
                e = e.cloneEntry(e.act.isInitialRound() && l.isConflict());
            }

            e.act.addLink(l, processMode);
            findLinkingCandidates(e, l);
            e.addToQueue(queue);
        }
    }

    public void findLinkingCandidates(Entry e, Link l) {
        if(((e.act.getINeuron() instanceof InhibitoryNeuron) || l.isConflict())) return;

        l.input.followDown(l.input.getDocument().getNewVisitedId(), act -> {
            Synapse is = e.act.getNeuron().getInputSynapse(act.getNeuron());
            if (is == null || l.synapse == is) return;

            if (act.getOutputLinks(is).noneMatch(la -> la.output == e.act)) {
                e.addCandidate(is, act);
            }
        });
    }
}
