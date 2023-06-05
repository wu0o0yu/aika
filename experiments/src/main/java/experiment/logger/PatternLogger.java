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
package experiment.logger;

import experiment.LabelUtil;
import network.aika.elements.activations.BindingActivation;
import network.aika.elements.activations.PatternActivation;
import network.aika.elements.links.PatternLink;
import network.aika.elements.neurons.BindingNeuron;
import network.aika.elements.neurons.PatternNeuron;
import network.aika.elements.synapses.PatternSynapse;
import network.aika.fields.FieldOutput;
import network.aika.text.Document;
import org.apache.commons.csv.CSVPrinter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static experiment.logger.ExperimentLogger.CSV_FORMAT;
import static java.lang.Integer.MAX_VALUE;
import static network.aika.utils.Utils.doubleToString;

/**
 * @author Lukas Molzberger
 */
public class PatternLogger {

    CSVPrinter printer;

    public PatternLogger() {
    }

    public PatternLogger(File path, PatternActivation act) {
        open(new File(path, "pattern-" + act.getNeuron().getId() + "-" + act.getLabel() + ".csv"));
    }


    public void open(File f)  {
        try {
            if(f.exists())
                f.delete();

            List<String> headerLabels = new ArrayList<>();
            headerLabels.addAll(
                    List.of(
                            "doc-id",
                            "content",
                            "orig.-label",
                            "cur.-label",
                            "match",
                            "match-pre-anneal",
                            "match-fired",
                            "act-id",
                            "n-id",
                            "net",
                            "net-pre-anneal",
                            "f'(net)",
                            "down-grad.",
                            "uv",
                            "neg-uv",
                            "bias",
                            "bias-sum"
                    )
            );

            for(int i = 0; i < 5; i++) {
                headerLabels.addAll(
                    List.of(
                            "|",
                            i + "-syn-weight",
                            i + "-syn-bias",
                            i + "-label",
                            i + "-act-id",
                            i + "-n-id",
                            i + "-net",
                            i + "-net-pre-anneal",
                            i + "-f'(net)",
                            i + "-up-grad.",
                            i + "-uv",
                            i + "-neg-uv",
                            i + "-bias",
                            i + "-bias-sum"
                    )
                );
            }

            FileWriter fw = new FileWriter(f);
            printer = new CSVPrinter(
                    fw,
                    CSV_FORMAT.withHeader(headerLabels.toArray(new String[0]))
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void close() {
        try {
            printer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void log(PatternActivation pAct) {
        PatternNeuron pn = pAct.getNeuron();
        Document doc = (Document) pAct.getThought();
        try {
            List entry = new ArrayList();

            entry.addAll(
                    List.of(
                            doc.getId(),
                            doc.getContent(),
                            pAct.getLabel(),
                            LabelUtil.generateLabel(pn),
                            LabelUtil.generateLabel(pAct, false, false),
                            LabelUtil.generateLabel(pAct, false, true),
                            LabelUtil.generateLabel(pAct, true, false),
                            pAct.getId(),
                            pn.getId() + (pn.isAbstract() ? "-abstr" : ""),
                            print(pAct.getNet()),
                            print(pAct.getNetPreAnneal()),
                            print(pAct.getNetOuterGradient()),
                            print(pAct.getGradient()),
                            print(pAct.getUpdateValue()),
                            print(pAct.getNegUpdateValue()),
                            print(pn.getBias()),
                            print(pn.getSynapseBiasSum())
                    )
            );

            List<PatternSynapse> inputSynapses = pn.getInputSynapsesByType(PatternSynapse.class)
                    .toList();

            for(int i = 0; i < Math.min(5, inputSynapses.size()); i++) {
                PatternSynapse s = inputSynapses.get(i);
                BindingNeuron bn = s.getInput();

                PatternLink il = (PatternLink) pAct.getInputLink(bn);
                BindingActivation iAct = il != null ? il.getInput() : null;

                entry.addAll(
                        iAct != null ?
                                getEntry(s, bn, iAct) :
                                getEntry(s, bn)
                );
            }

            printer.printRecord(entry.toArray());
            printer.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static List<? extends Serializable> getEntry(PatternSynapse s, BindingNeuron bn, BindingActivation iAct) {
        return List.of(
                "|",
                print(s.getWeight()),
                print(s.getSynapseBias()),
                iAct.getLabel(),
                iAct.getId(),
                bn.getId() + (bn.isAbstract() ? "-abstr" : ""),
                print(iAct.getNet()),
                print(iAct.getNetPreAnneal()),
                print(iAct.getNetOuterGradient()),
                print(iAct.getGradient()),
                print(iAct.getUpdateValue()),
                print(iAct.getNegUpdateValue()),
                print(bn.getBias()),
                print(bn.getSynapseBiasSum())
        );
    }

    private static List<? extends Serializable> getEntry(PatternSynapse s, BindingNeuron bn) {
        return List.of(
                "|",
                print(s.getWeight()),
                print(s.getSynapseBias()),
                "--",
                "--",
                bn.getId() + (bn.isAbstract() ? "-abstr" : ""),
                "--",
                "--",
                "--",
                "--",
                "--",
                "--",
                print(bn.getBias()),
                print(bn.getSynapseBiasSum())
        );
    }

    private static String print(FieldOutput f) {
        if(f == null)
            return "--";

        return doubleToString(f.getValue(MAX_VALUE), "#.#######");
    }
}
