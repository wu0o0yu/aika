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
package network.aika.fields;

/**
 * @author Lukas Molzberger
 */
public class FeedbackFunction extends IdentityFunction {

    public FeedbackFunction(FieldObject ref, String label) {
        super(ref, label);
    }

    boolean triggerMode = true;

    @Override
    protected int getNumberOfFunctionArguments() {
        return 2;
    }

    @Override
    protected double computeUpdate(AbstractFieldLink fl, double u) {
        if(triggerMode && fl.connected) {
            triggerMode = false;
            return fl.getUpdatedInputValue() - 1.0;
        }

        return u;
    }

    public boolean isTriggerMode() {
        return triggerMode;
    }

    public void setTriggerMode() {
        triggerUpdate(false, 1.0 - value);
        triggerMode = true;
    }

    @Override
    public void receiveUpdate(AbstractFieldLink fl, boolean nextRound, double u) {
        if(fl.getArgument() != 0)
            return; // Ignore the annealing updates

        double update = computeUpdate(fl, u);
        if(update == 0.0)
            return;

        triggerUpdate(true, update);
    }
}