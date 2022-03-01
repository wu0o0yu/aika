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
public class FieldDivision extends AbstractBiFunction {

    public FieldDivision(String label, FieldOutput in1, boolean register1, FieldOutput in2, boolean register2) {
        super(label, in1, register1, in2, register2);
    }

    public FieldDivision(String label, FieldOutput in1, boolean register1, FieldOutput in2, boolean register2, FieldInput... out) {
        super(label, in1, register1, in2, register2, out);
    }

    @Override
    public double getCurrentValue() {
        return FieldOutput.getCurrentValue(in1) / FieldOutput.getCurrentValue(in2);
    }

    @Override
    public double getNewValue() {
        switch (currentArgument) {
            case 1:
                return in1.getNewValue() / in2.getCurrentValue();
            case 2:
                return in1.getCurrentValue() / in2.getNewValue();
            default:
                throw new IllegalArgumentException();
        }
    }

    @Override
    public double getUpdate() {
        switch (currentArgument) {
            case 1:
                if (in1.updateAvailable()) {
                    double v2 = FieldOutput.getCurrentValue(in2);
                    return (in1.getUpdate() * v2) / Math.pow(v2, 2.0);
                }
                break;
            case 2:
                if (in2.updateAvailable()) {
                    return -(in2.getUpdate() * FieldOutput.getCurrentValue(in1)) / Math.pow(FieldOutput.getCurrentValue(in2), 2.0);
                }
                break;
            default:
                throw new IllegalArgumentException();
        }
        return 0.0;
    }

}
