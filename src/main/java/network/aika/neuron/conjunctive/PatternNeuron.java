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
package network.aika.neuron.conjunctive;

import network.aika.Model;
import network.aika.Thought;
import network.aika.neuron.Range;
import network.aika.neuron.SampleSpace;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.PatternActivation;
import network.aika.neuron.disjunctive.CategorySynapse;
import network.aika.neuron.disjunctive.PatternCategorySynapse;
import network.aika.sign.Sign;
import network.aika.utils.Bound;
import network.aika.utils.Utils;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import static network.aika.sign.Sign.POS;

/**
 *
 * @author Lukas Molzberger
 */
public class PatternNeuron extends ConjunctiveNeuron<ConjunctiveSynapse, PatternActivation> {

    protected double frequency;

    public PatternNeuron() {
        super();
    }

    @Override
    public CategorySynapse newCategorySynapse() {
        return new PatternCategorySynapse();
    }

    @Override
    public PatternActivation createActivation(Thought t) {
        return new PatternActivation(t.createActivationId(), t, this);
    }

    @Override
    public PatternNeuron instantiateTemplate(boolean addProvider) {
        PatternNeuron n = new PatternNeuron();
        if(addProvider)
            n.addProvider(getModel());

        initFromTemplate(n);
        return n;
    }

    @Override
    public PatternCategoryInputSynapse getCategoryInputSynapse() {
        return inputSynapses.stream()
                .filter(s -> s instanceof PatternCategoryInputSynapse)
                .map(s -> (PatternCategoryInputSynapse) s)
                .findAny()
                .orElse(null);
    }

    @Override
    protected void updateSumOfLowerWeights() {
    }

    @Override
    public void count(PatternActivation act) {
        double oldN = sampleSpace.getN();

        Range absoluteRange = act.getAbsoluteRange();
        sampleSpace.countSkippedInstances(absoluteRange);

        sampleSpace.count();
        frequency += 1.0;

        Double alpha = act.getConfig().getAlpha();
        if (alpha != null)
            applyMovingAverage(
                    Math.pow(alpha, sampleSpace.getN() - oldN)
            );

        sampleSpace.updateLastPosition(absoluteRange);
        setModified();
    }

    public void applyMovingAverage(double alpha) {
        sampleSpace.applyMovingAverage(alpha);
        frequency *= alpha;
        setModified();
    }


    public double getFrequency() {
        return frequency;
    }

    public double getFrequency(Sign s, double n) {
        return s == POS ?
                frequency :
                n - frequency;
    }

    public void setFrequency(double f) {
        frequency = f;
        setModified();
    }

    public double getSurprisal(Sign s, Range range, boolean addCurrentInstance) {
        double n = sampleSpace.getN(range);
        double p = getProbability(s, n, addCurrentInstance);
        return Utils.surprisal(p);
    }

    public double getProbability(Sign s, double n, boolean addCurrentInstance) {
        double f = getFrequency(s, n);

        if(addCurrentInstance) {
            f += 1.0;
            n += 1.0;
        }

        return Bound.UPPER.probability(f, n);
    }

    @Override
    public void write(DataOutput out) throws IOException {
        super.write(out);

        out.writeDouble(frequency);
    }

    @Override
    public void readFields(DataInput in, Model m) throws Exception {
        super.readFields(in, m);

        frequency = in.readDouble();
    }
}
