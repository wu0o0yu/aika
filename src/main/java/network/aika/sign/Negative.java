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
package network.aika.sign;

import network.aika.FieldObject;
import network.aika.fields.FieldOutput;
import network.aika.fields.Fields;

/**
 *
 * @author Lukas Molzberger
 */
public class Negative implements Sign {
    @Override
    public Sign invert() {
        return POS;
    }

    @Override
    public FieldOutput getValue(FieldObject ref, FieldOutput v) {
        return Fields.invert(ref, "neg", v);
    }

    @Override
    public int index() {
        return 1;
    }

    public String toString() {
        return "NEG";
    }
}
