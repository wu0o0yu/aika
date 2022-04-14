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
package network.aika.neuron.bindingsignal;

import network.aika.direction.Direction;

import static network.aika.direction.Direction.OUTPUT;

/**
 * @author Lukas Molzberger
 */
public class Transition {

    private State input;
    private State output;
    private boolean check;
    private Integer propagate;

    private Transition(State input, State output, boolean check, Integer propagate) {
        this.input = input;
        this.output = output;
        this.check = check;
        this.propagate = propagate;
    }

    public static Transition transition(State input, State output, boolean check, Integer propagate) {
        return new Transition(input, output, check, propagate);
    }

    public State getInput() {
        return input;
    }

    public State getOutput() {
        return output;
    }

    public boolean isCheck() {
        return check;
    }

    public Integer getPropagate() {
        return propagate;
    }

    public State next(Direction dir) {
        return dir == OUTPUT ? output : input;
    }

    public boolean check(State from, Direction dir, boolean prop) {
        if(!check && !prop)
            return false;

        if(propagate == 0 && prop)
            return false;

        return from == (dir == OUTPUT ? input : output);
    }

    public String toString() {
        return "Input:" + input +
                " Output:" + output +
                " Check:" + check +
                " Propagate:" + (propagate == Integer.MAX_VALUE ? "UNLIMITED" : propagate);
    }
}
