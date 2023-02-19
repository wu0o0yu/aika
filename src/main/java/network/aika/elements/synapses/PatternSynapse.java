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
package network.aika.elements.synapses;

import network.aika.Model;
import network.aika.elements.activations.Activation;
import network.aika.elements.activations.BindingActivation;
import network.aika.elements.activations.PatternActivation;
import network.aika.elements.links.PatternLink;
import network.aika.elements.neurons.Range;
import network.aika.elements.neurons.SampleSpace;
import network.aika.elements.neurons.BindingNeuron;
import network.aika.sign.Sign;
import network.aika.utils.Bound;
import network.aika.utils.Utils;
import network.aika.visitor.linking.LinkingOperator;
import network.aika.visitor.linking.pattern.PatternDownVisitor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import static network.aika.sign.Sign.NEG;
import static network.aika.sign.Sign.POS;


/**
 *
 * @author Lukas Molzberger
 */
public class PatternSynapse extends AbstractPatternSynapse<
        PatternSynapse,
        BindingNeuron,
        PatternLink,
        BindingActivation
        >
{

    protected double frequencyIPosOPos;
    protected double frequencyIPosONeg;
    protected double frequencyINegOPos;

    protected SampleSpace sampleSpace = new SampleSpace();

    public PatternSynapse() {
        super();
    }

    @Override
    public PatternLink createLink(BindingActivation input, PatternActivation output) {
        return new PatternLink(this, input, output);
    }

    @Override
    public void startVisitor(LinkingOperator c, Activation act) {
        new PatternDownVisitor(act.getThought(), c)
                .start(act);
    }

    public SampleSpace getSampleSpace() {
        return sampleSpace;
    }

    public double getFrequency(Sign is, Sign os, double n) {
        if(is == POS && os == POS) {
            return frequencyIPosOPos;
        } else if(is == POS && os == NEG) {
            return frequencyIPosONeg;
        } else if(is == NEG && os == POS) {
            return frequencyINegOPos;
        }

        //TODO:
        return Math.max(n - (frequencyIPosOPos + frequencyIPosONeg + frequencyINegOPos), 0);
    }

    public void setFrequency(Sign is, Sign os, double f) {
        if(is == POS && os == POS) {
            frequencyIPosOPos = f;
        } else if(is == POS && os == NEG) {
            frequencyIPosONeg = f;
        } else if(is == NEG && os == POS) {
            frequencyINegOPos = f;
        } else {
            throw new UnsupportedOperationException();
        }
        setModified();
    }

    public void applyMovingAverage(double alpha) {
        sampleSpace.applyMovingAverage(alpha);
        frequencyIPosOPos *= alpha;
        frequencyIPosONeg *= alpha;
        frequencyINegOPos *= alpha;
        setModified();
    }

    @Override
    public void count(PatternLink l) {
        double oldN = sampleSpace.getN();

        if(l.getInput() == null)
            return; // TODO: fix

        boolean iActive = l.getInput() != null && l.getInput().isFired();
        boolean oActive = l.getOutput().isFired();

        Range absoluteRange = l.getInput().getAbsoluteRange();
        if(absoluteRange == null)
            return;

        sampleSpace.countSkippedInstances(absoluteRange);

        sampleSpace.count();

        if(oActive) {
            Double alpha = l.getConfig().getAlpha();
            if (alpha != null)
                applyMovingAverage(
                        Math.pow(alpha, sampleSpace.getN() - oldN)
                );
        }

        if(iActive && oActive) {
            frequencyIPosOPos += 1.0;
            setModified();
        } else if(iActive) {
            frequencyIPosONeg += 1.0;
            setModified();
        } else if(oActive) {
            frequencyINegOPos += 1.0;
            setModified();
        }

        sampleSpace.updateLastPosition(absoluteRange);
    }

    public double getSurprisal(Sign si, Sign so, Range range, boolean addCurrentInstance) {
        double n = sampleSpace.getN(range);
        double p = getProbability(si, so, n, addCurrentInstance);
        return Utils.surprisal(p);
    }

    public double getProbability(Sign si, Sign so, double n, boolean addCurrentInstance) {
        double f = getFrequency(si, so, n);

        // Add the current instance
        if(addCurrentInstance) {
            f += 1.0;
            n += 1.0;
        }

        return Bound.UPPER.probability(f, n);
    }

    @Override
    public void write(DataOutput out) throws IOException {
        super.write(out);

        out.writeDouble(frequencyIPosOPos);
        out.writeDouble(frequencyIPosONeg);
        out.writeDouble(frequencyINegOPos);

        sampleSpace.write(out);
    }

    @Override
    public void readFields(DataInput in, Model m) throws IOException {
        super.readFields(in, m);

        frequencyIPosOPos = in.readDouble();
        frequencyIPosONeg = in.readDouble();
        frequencyINegOPos = in.readDouble();

        sampleSpace = SampleSpace.read(in, m);
    }
}
