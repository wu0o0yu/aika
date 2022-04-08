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
public class Division extends AbstractBiFunction {

    public Division(String label, FieldOutput in1, FieldOutput in2) {
        super(label, in1, in2);
        registerInputListener();
    }

    @Override
    public double getCurrentValue() {
        return FieldOutput.getCurrentValue(in1) / FieldOutput.getCurrentValue(in2);
    }

    @Override
    protected double computeUpdate(int arg, double u) {
        switch (arg) {
            case 1:
                double v2 = FieldOutput.getCurrentValue(in2);
                return (u * v2) / Math.pow(v2, 2.0);
            case 2:
                return -(u * FieldOutput.getCurrentValue(in1)) / Math.pow(FieldOutput.getCurrentValue(in2), 2.0);
            default:
                throw new IllegalArgumentException();
        }
    }
}
