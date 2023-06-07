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

import java.util.Collection;
import java.util.Collections;

/**
 * @author Lukas Molzberger
 */
public class ConstantField extends Field implements FieldOutput {

    public static final ConstantField ZERO = new ConstantField(null, "ZERO", 0.0);
    public static final ConstantField ONE = new ConstantField(null, "ONE", 1.0);

    public ConstantField(FieldObject ref, String label, double value) {
        super(ref, label, null);
        setInitialValue(FIRST_ROUND, value);
    }

    @Override
    public void addInput(FieldLink fl) {
    }

    @Override
    public void removeInput(FieldLink fl) {
    }

    @Override
    public Collection<FieldLink> getInputs() {
        return Collections.emptyList();
    }

    @Override
    public int getNextArg() {
        return 0;
    }
}
