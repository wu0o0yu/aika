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
package network.aika;


import network.aika.neuron.INeuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Direction;
import network.aika.neuron.activation.linker.Linker;
import network.aika.neuron.activation.Queue;
import network.aika.neuron.TNeuron;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


/**
 * The {@code Document} class represents a single document which may be either used for processing a text or as
 * training input. A document consists of the raw text, the interpretations and the activations.
 *
 * <p>When the document is not needed any more, the method {@code clearActivations} must be called, since Aika only
 * supports a single document per thread and model.
 *
 * @author Lukas Molzberger
 */
public class Document implements Comparable<Document> {
    private static final Logger log = LoggerFactory.getLogger(Document.class);

    public static int MAX_ROUND = 20;

    private final int id;
    private final StringBuilder content;

    private long visitedCounter = 1;
    private int activationIdCounter = 0;

    private Model model;

    private Queue queue = new Queue();

    private TreeMap<INeuron, Set<Synapse>> modifiedWeights = new TreeMap<>();

    private TreeMap<Integer, Activation> activationsById = new TreeMap<>();

    public long createV;


    public Document(Model model, String content) {
        this(model, model.getNewDocumentId(), content);
    }

    public Document(Model model, int id, String content) {
        this.id = id;
        this.content = new StringBuilder(content);

        this.model = model;
    }

    public int getId() {
        return id;
    }

    public Model getModel() {
        return model;
    }

    public Queue getQueue() {
        return queue;
    }

    public void process() {
        activationsById
                .values()
                .stream()
                .filter(act -> act.assumePosRecLinks)
                .forEach(act -> Linker.linkPosRec(act));

        queue.process();

        activationsById
                .values()
                .stream()
                .forEach(act -> act.computeP());

        queue.process();
    }

    public long getNewVisitedId() {
        return visitedCounter++;
    }

    public int getNewActivationId() {
        return activationIdCounter++;
    }

    public void append(String txt) {
        content.append(txt);
    }

    public char charAt(int i) {
        return content.charAt(i);
    }

    public String getContent() {
        return content.toString();
    }

    public int length() {
        return content.length();
    }

    public String toString() {
		return content.toString();
	}

    public String getText(Integer begin, Integer end) {
        if(begin != null && end != null) {
            return content.substring(
                    Math.max(0, Math.min(begin, length())),
                    Math.max(0, Math.min(end, length()))
            );
        } else {
            return "";
        }
    }

    public void addActivation(Activation act) {
        activationsById.put(act.getId(), act);
    }

    public Collection<Activation> getActivations() {
        return activationsById.values();
    }

    public int getNumberOfActivations() {
        return activationsById.size();
    }

    public void train(Config c) {
        long v = getNewVisitedId();
        createV = v;
/*
        model.applyMovingAverage();
        for(TNeuron n: activatedNeurons) {
            n.applyMovingAverage(v);
        }
*/
        getActivations()
                .stream()
                .filter(act -> act.isActive())
                .forEach(act -> {
                    TNeuron n = act.getINeuron();
                    n.count(act);
                });

//        propagate();

        commit();

        getModel().N += length();
    }


    public void notifyWeightModified(Synapse synapse) {
        Set<Synapse> is = modifiedWeights.get(synapse.getOutput());
        if(is == null) {
            is = new TreeSet<>(Direction.INPUT.getSynapseComparator());
            modifiedWeights.put(synapse.getOutput(), is);
        }
        is.add(synapse);
    }


    public Map<INeuron, Set<Synapse>> getModifiedWeights() {
        return modifiedWeights;
    }

    /**
     * Updates the model after the training step.
     * It applies the weight and bias delta values and reflects the changes in the logic node structure.
     */
    public void commit() {
        modifiedWeights.forEach((n, inputSyns) -> {
            n.commit(inputSyns);
        });
        modifiedWeights.clear();
    }


    public String activationsToString() {
        StringBuilder sb = new StringBuilder();

        sb.append("Id -");

        sb.append(" Decision -");

        sb.append(" Range | Text Snippet");
        sb.append(" | Identity -");
        sb.append(" Neuron Label -");
        sb.append(" Upper Bound -");
        sb.append(" Value | Net | Weight -");
        sb.append(" Input Value |");
        sb.append("\n");
        sb.append("\n");

        for(Activation act: activationsById.values()) {
/*            if(!act.isActive()) {
                continue;
            }
*/
            sb.append(act.toString());
            sb.append("\n");
        }

        return sb.toString();
    }

    @Override
    public int compareTo(Document doc) {
        return Integer.compare(id, doc.id);
    }
}
