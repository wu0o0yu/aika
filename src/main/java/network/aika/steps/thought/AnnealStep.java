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
package network.aika.steps.thought;

import network.aika.Thought;
import network.aika.elements.links.NegativeFeedbackLink;
import network.aika.elements.neurons.Neuron;
import network.aika.fields.AbstractFieldLink;
import network.aika.fields.Field;
import network.aika.steps.Phase;
import network.aika.steps.Step;
import network.aika.steps.activation.Save;
import network.aika.text.Document;

import static network.aika.steps.Phase.*;


/**
 *
 * @author Lukas Molzberger
 */
public class AnnealStep extends Step<Thought> {

    public static void add(Thought t) {
        Step.add(new AnnealStep(t));
    }

    public AnnealStep(Thought t) {
        super(t);
    }

    @Override
    public void process() {
        Thought t = getElement();

        double annealStep = t.getAnnealing().getCurrentValue();
        System.out.println("Anneal-Step: " + annealStep);
        annealStep += getMaxAnnealStep();

        t.getAnnealing().setValue(annealStep);

        if (annealStep < 1.0)
            AnnealStep.add(t);
    }

    public double getMaxAnnealStep() {
        double maxAnnealStep = 1.0;
        for (AbstractFieldLink fl : getElement().getAnnealing().getReceivers()) {
            if (!(fl.getOutput() instanceof Field))
                continue;

            Field f = (Field) fl.getOutput();
            NegativeFeedbackLink negFeedbackLink = (NegativeFeedbackLink) f.getReference();
            double x = negFeedbackLink.getMaxInput().getCurrentValue();
            double w = negFeedbackLink.getSynapse().getWeight().getCurrentValue();
            double wi = x * w;
            if(wi >= 0.0)
                continue;

            for (AbstractFieldLink flNet : f.getReceivers()) {
                if (!(flNet.getOutput() instanceof Field))
                    continue;

                Field fNet = (Field) flNet.getOutput();
                if (fNet.getCurrentValue() > 0.0) {
                    maxAnnealStep = Math.min(maxAnnealStep, fNet.getCurrentValue() / -wi);
                }
            }
        }
        return maxAnnealStep;
    }

    @Override
    public Phase getPhase() {
        return ANNEAL;
    }

    @Override
    public String toString() {
        return ((Document)getElement()).getContent();
    }
}
