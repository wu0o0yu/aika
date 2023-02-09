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
import network.aika.steps.Phase;
import network.aika.steps.Step;
import network.aika.utils.Utils;

import static network.aika.steps.Phase.*;


/**
 *
 * @author Lukas Molzberger
 */
public class AnnealStep extends Step<Thought> {

    public static double STEP_SIZE = 0.05;

    public static void add(Thought t) {
        Step.add(new AnnealStep(t, STEP_SIZE * t.getStepScale()));
    }

    public static void add(Thought t, Double normAnnealStepSize) {
        Step.add(new AnnealStep(t, normAnnealStepSize));
    }

    private Double normAnnealStepSize;

    public AnnealStep(Thought t, Double normAnnealStepSize) {
        super(t);
        this.normAnnealStepSize = normAnnealStepSize;
    }

    @Override
    public void process() {
        Thought t = getElement();

        double nextAnnealValue = normAnnealStepSize + t.getAnnealing().getCurrentValue();
        t.getAnnealing().setValue(nextAnnealValue);

        if (nextAnnealValue < 1.0)
            AnnealStep.add(t);
    }

    @Override
    public Phase getPhase() {
        return ANNEAL;
    }

    @Override
    public String toString() {
        return "docId:" + getElement().getId() + " annealStepSize:" + Utils.round(normAnnealStepSize) + " " + getElement().getAnnealing();
    }
}
