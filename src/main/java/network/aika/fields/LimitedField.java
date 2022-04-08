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
public class LimitedField extends Field {

    private double limit;

    public LimitedField(Object refObj, String label, double limit) {
        super(refObj, label);
        this.limit = limit;
    }

    public LimitedField(Object refObj, String label, double limit, double initialValue) {
        super(refObj, label, initialValue);
        this.limit = limit;
    }

    public LimitedField(Object refObj, String label, double limit, FieldUpdateEvent fieldListener) {
        super(refObj, label, fieldListener);
        this.limit = limit;
    }

    @Override
    public boolean set(double v) {
        return super.set(getLimitedValue(v));
    }

    @Override
    public boolean receiveUpdate(double u) {
        double cv = isInitialized() ? getCurrentValue() : 0.0;
        double nv = getLimitedValue(cv + u);
        return super.receiveUpdate(nv - cv);
    }

    private double getLimitedValue(double nv) {
        return Math.max(limit, nv);
    }
}
