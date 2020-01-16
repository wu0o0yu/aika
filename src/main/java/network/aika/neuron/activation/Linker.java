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


/**
 *
 * @author Lukas Molzberger
 */
public class Linker {

    public void linkForward(Activation act) {
        ArrayDeque<Entry> queue = new ArrayDeque<>();
        Document doc = act.getDocument();
        TreeSet<Neuron> propagationTargets = new TreeSet(act.getINeuron().getPropagationTargets());

        if(act.lastRound != null) {
            act.lastRound.outputLinks
                    .values()
                    .stream()
                    .forEach(l -> {
                         queue.add(new Entry(new Link(l.synapse, act, l.output)));
                        propagationTargets.remove(l.output.getNeuron());
                    });
        }

        act.followDown(doc.getNewVisitedId(), cAct -> {
            if(cAct.getINeuron() instanceof InhibitoryNeuron) return;

            Synapse s = act.getNeuron().getOutputSynapse(cAct.getNeuron());
            if(s == null || !act.outputLinks.subMap(
                    new Activation(Integer.MIN_VALUE, cAct.getINeuron()),
                    new Activation(Integer.MAX_VALUE, cAct.getINeuron())
            ).isEmpty()) return;

            queue.add(new Entry(new Link(s, act, cAct)));
            propagationTargets.remove(cAct.getNeuron());
        });

        propagationTargets
                .stream()
                .map(n -> n.get().getProvider())
                .map(n -> act.getNeuron().getOutputSynapse(n))
                .forEach(s -> queue.add(new Entry(new Link(s, act, lookupNewActivation(doc, s.getOutput(), null)))));

        process(queue);
    }


    private Activation lookupNewActivation(Document doc, INeuron n, Activation oldAct) {
        if(oldAct == null) {
            return new Activation(doc, n, null, 0);
        } else if(oldAct.nextRound != null) {
            return oldAct.nextRound;
        } else {
            return oldAct.cloneAct();
        }
    }


    private static class Entry {
        Activation act;
        NavigableSet<Link> candidates = new TreeSet<>(Comparator.
                <Link, Fired>comparing(l -> l.getInput().getFired())
                .thenComparing(l -> l.getSynapse().getInput())
        );


        private Entry() {
        }

        public Entry(Link l) {
            this.act = l.output;
            candidates.add(l);
        }

        public Entry cloneEntry() {
            Entry ce = new Entry();
            ce.act = act.cloneAct();
            ce.candidates.addAll(candidates);
            return ce;
        }
    }


    private void process(ArrayDeque<Entry> queue) {
        while (!queue.isEmpty()) {
            Entry e = queue.pollFirst();
            Link l = e.candidates.pollFirst();

            if(e.act.isFinal && !l.isSelfRef()) {
                if (!e.candidates.isEmpty()) {
                    queue.addLast(e);
                }
                e = e.cloneEntry();
            }

            e.act.addLink(l);

            findLinkingCandidates(e, l);

            if (!e.candidates.isEmpty()) {
                queue.addLast(e);
            }
        }
    }


    public void findLinkingCandidates(Entry e, Link l) {
        if(((e.act.getINeuron() instanceof InhibitoryNeuron) || l.isConflict())) return;

        l.input.followDown(l.input.getDocument().getNewVisitedId(), act -> {
            Synapse is = e.act.getNeuron().getInputSynapse(act.getNeuron());
            if (is == null) return;

            List<Link> ols = act
                    .getOutputLinks(is)
                    .filter(la -> la.output == e.act)
                    .collect(Collectors.toList());
            if (ols.isEmpty()) {
                ols.add(new Link(is, act, e.act));
            }
            e.candidates.addAll(ols);
        });
    }

}
