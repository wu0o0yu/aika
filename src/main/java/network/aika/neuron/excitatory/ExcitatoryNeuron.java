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
package network.aika.neuron.excitatory;

import network.aika.Config;
import network.aika.Document;
import network.aika.Model;
import network.aika.neuron.*;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Link;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Function;

/**
 *
 * @author Lukas Molzberger
 */
public abstract class ExcitatoryNeuron<S extends ExcitatorySynapse> extends ConjunctiveNeuron<S> {

    private static final Logger log = LoggerFactory.getLogger(ExcitatoryNeuron.class);

    public ExcitatoryNeuron() {
        super();
    }

    public ExcitatoryNeuron(Neuron p) {
        super(p);
    }

    public ExcitatoryNeuron(Model model, String label) {
        super(model, label);
    }

    public double computeWeightGradient(Link il) {
        return computeGradient(il, 0, l -> l.getInput().value);
    }

    public double computeGradient(Link il, int depth, Function<Link, Double> f) {
        if(depth > 2) return 0.0;

        double g = f.apply(il) * getActivationFunction().outerGrad(il.getOutput().net);

        double sum = 0.0;
        for (Sign s : Sign.values()) {
            sum += s.getSign() * getCost(s) * g;
            for (Link ol : il.getOutput().outputLinks.values()) {
                sum += ol.getOutput().getINeuron().computeGradient(ol, depth + 1, l -> g * il.getSynapse().getWeight());
            }
        }

        return sum;
    }

    protected abstract void createCandidateSynapse(Config c, Activation iAct, Activation targetAct);

    public void train(Config c, Activation act) {
        addDummyLinks(act);
        createCandidateSynapses(c, act);
    }

    protected void addDummyLinks(Activation act) {
        inputSynapses
                .values()
                .stream()
                .filter(s -> !act.inputLinks.containsKey(s))
                .forEach(s -> new Link(s, null, act).link());
    }

    private void createCandidateSynapses(Config c, Activation targetAct) {
        Document doc = targetAct.getDocument();

        if(log.isDebugEnabled()) {
            log.debug("Created Synapses for Neuron: " + targetAct.getINeuron().getId() + ":" + targetAct.getINeuron().getLabel());
        }

        ArrayList<Activation> candidates = new ArrayList<>();

        collectLinkingCandidates(targetAct, act -> {
            Synapse is = targetAct.getNeuron().getInputSynapse(act.getNeuron());
            if(is != null) return;

            candidates.add(act);
        });

        candidates
                .forEach(act -> createCandidateSynapse(c, act, targetAct));
    }

    public String typeToString() {
        return "EXCITATORY";
    }
}
