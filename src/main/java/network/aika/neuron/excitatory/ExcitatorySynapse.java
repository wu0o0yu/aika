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
import network.aika.neuron.activation.PatternActivation;
import network.aika.neuron.steps.UpdateNet;
import network.aika.neuron.steps.activation.PostTraining;

/**
 *
 * @author Lukas Molzberger
 */
public abstract class ExcitatorySynapse<I extends Neuron, O extends ExcitatoryNeuron<?, A>, A extends Activation> extends Synapse<I, O, A> {


    @Override
    public boolean isWeak() {
        return isWeak(getOutput().getWeightsSum());
    }

    public boolean isWeak(double weightSum) {
        boolean weightIsAbleToExceedThreshold = weight + getOutput().getInitialNet() > 0.0;
        boolean weightSumIsAbleToExceedThreshold = weightSum + getOutput().getInitialNet() > 0.0;
        boolean weightIsAbleToSuppressThresholdExceededByWeightSum = (weightSum - weight) + getOutput().getInitialNet() <= 0.0;

        return !(weightIsAbleToExceedThreshold ||
                (weightSumIsAbleToExceedThreshold && weightIsAbleToSuppressThresholdExceededByWeightSum));
    }

    @Override
    public void addWeight(double weightDelta) {
        super.addWeight(weightDelta);
        getOutput().addWeight(weightDelta);
    }

    @Override
    public boolean allowLinking(Activation bindingSignal) {
        return bindingSignal instanceof PatternActivation;
    }

    @Override
    public void updateSynapse(Link l, double delta) {
        if(l.getInput().isFired()) {
            addWeight(delta);
            l.updateNetByWeight(delta);
        } else {
            addWeight(-delta);
            l.updateNetByWeight(-delta);

            getOutput().addConjunctiveBias(delta);
            if(delta < 0.0)
                PostTraining.add(l.getOutput());

            UpdateNet.updateNet(l.getOutput(), delta);
        }

        checkConstraints();
    }

    protected void checkConstraints() {
        assert !isNegative();
    }

    @Override
    public void setModified() {
        getOutput().setModified();
    }
}
