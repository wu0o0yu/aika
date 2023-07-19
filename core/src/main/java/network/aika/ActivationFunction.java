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

/**
 *
 * @author Lukas Molzberger
 */
public enum ActivationFunction {

    RECTIFIED_HYPERBOLIC_TANGENT(
            x -> Math.max(0.0, Math.tanh(x)),
            x -> x >= 0.0 ? 1.0 - Math.pow(Math.tanh(x), 2.0) : 0.0
    ),
    LIMITED_RECTIFIED_LINEAR_UNIT(
            x -> Math.max(0.0, Math.min(1.0, x)),
            x -> x >= 0.0 && x <= 1.0 ? 1.0 : 0.0
    );

    private final Function f;
    private final Function outerGrad;

    ActivationFunction(Function f, Function outerGrad) {
        this.f = f;
        this.outerGrad = outerGrad;
    }

    public double f(double x) {
        return f.f(x);
    }

    public double outerGrad(double x) {
        return outerGrad.f(x);
    }

    interface Function {
        double f(double x);
    }
}