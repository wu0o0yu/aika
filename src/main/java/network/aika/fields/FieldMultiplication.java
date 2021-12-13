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

import network.aika.utils.Utils;

/**
 * @author Lukas Molzberger
 */
public class FieldMultiplication implements FieldOutput {
    private FieldOutput in1;
    private FieldOutput in2;

    public FieldMultiplication(FieldOutput in1, FieldOutput in2) {
        this.in1 = in1;
        this.in2 = in2;
    }

    @Override
    public double getCurrentValue() {
        return in1.getCurrentValue() * in2.getCurrentValue();
    }

    @Override
    public double getNewValue(boolean ack) {
        return in1.getNewValue(ack) * in2.getNewValue(ack);
    }

    @Override
    public boolean updateAvailable() {
        return updateAvailable(1) || updateAvailable(2);
    }

    @Override
    public double getUpdate(boolean ack) {
        double result = 0.0;
        if(in1.updateAvailable())
            result += in1.getUpdate(ack) * in2.getCurrentValue();

        if(in2.updateAvailable())
            result += in2.getUpdate(ack) * in1.getCurrentValue();

        return result;
    }

    public boolean updateAvailable(int updateArg) {
        switch (updateArg) {
            case 1:
                return !Utils.belowTolerance(in2.getCurrentValue()) && in1.updateAvailable();
            case 2:
                return !Utils.belowTolerance(in1.getCurrentValue()) && in2.updateAvailable();
            default:
                return false;
        }
    }

    public double getUpdate(int updateArg, boolean ack) {
        double result = 0.0;
        if(updateArg == 1) {
            if (in1.updateAvailable())
                result += in1.getUpdate(ack) * in2.getCurrentValue();
        } else if(updateArg == 2) {
            if (in2.updateAvailable())
                result += in2.getUpdate(ack) * in1.getCurrentValue();
        }
        return result;
    }

    @Override
    public void acknowledgePropagated() {
        in1.acknowledgePropagated();
        in2.acknowledgePropagated();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[u:");
        if(updateAvailable())
            sb.append(Utils.round(getUpdate(false)));
        else sb.append("-");
        sb.append(",v:");
        sb.append(Utils.round(getCurrentValue()));
        sb.append("]");
        return sb.toString();
    }
}
