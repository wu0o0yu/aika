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
public class Division extends AbstractFunction {

    public Division(FieldObject ref, String label) {
        super(ref, label);
    }

    @Override
    protected int getNumberOfFunctionArguments() {
        return 2;
    }

    @Override
    protected double computeUpdate(AbstractFieldLink fl, double u) {
        return switch (fl.getArgument()) {
            case 0 -> updateDiv1(u);
            case 1 -> -(u * getInputValueByArg(0)) / Math.pow(fl.getInputValue(), 2.0);
            default -> throw new IllegalArgumentException();
        };
    }

    private double updateDiv1(double u) {
        double v2 = getInputValueByArg(1);
        return (u * v2) / Math.pow(v2, 2.0);
    }
}
