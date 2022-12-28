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

import network.aika.FieldObject;
import network.aika.steps.Phase;


/**
 * @author Lukas Molzberger
 */
public class LimitedField extends QueueSumField {

    private double limit;


    public LimitedField(FieldObject refObj, Phase p, String label, double limit) {
        super(refObj, p, label);
        this.limit = limit;
    }

    @Override
    public void setValue(double v) {
        super.setValue(getLimitedValue(v));
    }

    @Override
    public void receiveUpdate(AbstractFieldLink fl, double u) {
        double cv = currentValue;
        double nv = getLimitedValue(cv + u);
        super.receiveUpdate(fl, nv - cv);
    }

    private double getLimitedValue(double nv) {
        return Math.min(limit, nv);
    }
}
