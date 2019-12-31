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
import network.aika.neuron.Synapse;

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


    public void process() {
        while(!queue.isEmpty()) {
            Entry entry = queue.pollFirst();

            Activation targetAct = entry.act;

            Link cand = entry.candidates.pollFirst();
            Document doc = cand.input.getDocument();

            List<Link> newCandidates = new ArrayList<>();
            List<Link> conflicts = new ArrayList<>();

            cand.input.followDown(doc.getNewVisitedId(), (act, isConflict) -> {
                Synapse is = targetAct.getINeuron().getInputSynapse(act.getNeuron());
                if(is == null || act == cand.input) {
                    return false;
                }

                List<Link> ols = act
                        .getOutputLinks(is)
                        .filter(l -> l.output == targetAct)
                        .collect(Collectors.toList());
                if (ols.isEmpty()) {
                    ols.add(new Link(is, act, targetAct));
                }
                (!isConflict ? newCandidates : conflicts).addAll(ols);

                return false;
            });

            if (!conflicts.isEmpty()) {
                Entry alternativeEntry = new Entry(targetAct.cloneAct(), new TreeSet<>(entry.candidates));
                alternativeEntry.candidates.addAll(conflicts);

                queue.addLast(alternativeEntry);
            }

            targetAct.addLink(cand);

            entry.candidates.addAll(newCandidates);

            if (!entry.candidates.isEmpty()) {
                queue.addLast(entry);
            }
        }
    }


    public void add(Link il) {
        NavigableSet<Link> candidates = new TreeSet<>(INPUT_COMP);

        candidates.add(il);

        queue.addLast(new Entry(il.output, candidates));
    }
}
