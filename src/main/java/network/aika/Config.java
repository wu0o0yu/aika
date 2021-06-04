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

import network.aika.neuron.activation.Activation;
import network.aika.utils.Writable;

import java.util.function.Supplier;

/**
 *
 * @author Lukas Molzberger
 */
public class Config {
    private Double alpha = null; //0.99;
    private double learnRate;

    private boolean enableTraining;

    public double getLearnRate() {
        return learnRate;
    }

    public Config setLearnRate(double learnRate) {
        this.learnRate = learnRate;
        return this;
    }

    public Double getAlpha() {
        return alpha;
    }

    public Config setAlpha(Double alpha) {
        this.alpha = alpha;
        return this;
    }



    public boolean isEnableTraining() {
        return enableTraining;
    }

    public Config setEnableTraining(boolean enableTraining) {
        this.enableTraining = enableTraining;
        return this;
    }

    public String getLabel(Activation act) {
        return "";
    }



    public String toString() {
        return "Alpha: " + alpha + "\n" +
                "LearnRate" + learnRate + "\n\n";
    }
}
