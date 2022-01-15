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

import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Link;
import network.aika.sign.Sign;
import network.aika.steps.activation.PostTraining;
import network.aika.utils.Bound;

/**
 *
 * @author Lukas Molzberger
 */
public abstract class ExcitatorySynapse<I extends Neuron, O extends ExcitatoryNeuron<?, OA>, IA extends Activation, OA extends Activation> extends Synapse<I, O, IA, OA> {

    @Override
    public boolean isWeak() {
        return isWeak(getOutput().getWeightSum().getCurrentValue());
    }

    public boolean isWeak(double weightSum) {
        double bias = getOutput().getBias().getCurrentValue();
        double w = weight.getCurrentValue();

        boolean weightIsAbleToExceedThreshold = w + bias > 0.0;
        boolean weightSumIsAbleToExceedThreshold = weightSum + bias > 0.0;
        boolean weightIsAbleToSuppressThresholdExceededByWeightSum = (weightSum - w) + bias <= 0.0;

        return !(weightIsAbleToExceedThreshold ||
                (weightSumIsAbleToExceedThreshold && weightIsAbleToSuppressThresholdExceededByWeightSum));
    }

    @Override
    protected void weightUpdate(double u) {
        super.weightUpdate(u);
        getOutput().getWeightSum().addAndTriggerUpdate(u);
    }

    @Override
    public void updateSynapse(Link l, double delta) {
        if(l.getInput().isFired()) {
            weight.addAndTriggerUpdate(delta);
        } else {
            weight.addAndTriggerUpdate(-delta);
            getOutput().getBias().addAndTriggerUpdate(delta);

            if(delta < 0.0)
                PostTraining.add(l.getOutput());
        }

        checkConstraints();
    }

    protected void checkConstraints() {
        assert !isNegative();
    }

    @Override
    protected Bound getProbabilityBound(Sign si, Sign so) {
        return si == Sign.POS ? Bound.LOWER : Bound.UPPER;
    }

    @Override
    public void setModified() {
        getOutput().setModified();
    }
}
