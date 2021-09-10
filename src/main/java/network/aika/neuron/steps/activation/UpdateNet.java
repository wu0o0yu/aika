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
package network.aika.neuron.steps.activation;

import network.aika.neuron.activation.Activation;
import network.aika.neuron.steps.Step;
import network.aika.utils.Utils;


/**
 *
 * @author Lukas Molzberger
 */
public abstract class UpdateNet extends Step<Activation> {

    public UpdateNet(Activation act) {
        super(act);
    }

    public void updateNet(double netDelta) {
        updateNet(getElement(), netDelta);
    }

    public static void updateNet(Activation act, double netDelta) {
        act.updateNet(netDelta);
        Utils.checkTolerance(act, netDelta);

        PropagateGradientsNet.add(act);
        CheckIfFired.add(act);
    }
}
