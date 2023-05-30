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
package experiment;

import meta.AbstractTemplateModel;
import meta.SyllableTemplateModel;
import network.aika.Config;
import network.aika.Model;
import network.aika.debugger.AIKADebugger;
import network.aika.elements.activations.Activation;
import network.aika.elements.activations.BindingActivation;
import network.aika.elements.activations.PatternActivation;
import network.aika.elements.activations.TokenActivation;
import network.aika.steps.Phase;
import network.aika.text.Document;
import org.apache.commons.io.IOUtils;
import experiment.logger.ExperimentLogger;
import experiment.logger.LoggingListener;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import static network.aika.utils.Utils.doubleToString;


/**
 * @author Lukas Molzberger
 */
public class SyllablesExperiment {

    public static void processTokens(AbstractTemplateModel m, Document doc, Iterable<String> tokens, int separatorLength) {
        int i = 0;
        int pos = 0;

        List<TokenActivation> tokenActs = new ArrayList<>();
        for(String t: tokens) {
            int j = i + t.length();

            tokenActs.add(
                    doc.addToken(
                            m.lookupInputToken(t),
                            pos,
                            i,
                            j
                    )
            );

            pos++;

            i = j + separatorLength;
        }

        doc.setActivationCheckCallback(act ->
                m.evaluatePrimaryBindingActs(act)
        );

        m.setTokenInputNet(tokenActs);
    }

    public static Config getConfig() {
        return new Config() {
            public void updateLabel(Activation templateAct, Activation instanceAct) {
                Document doc = (Document) templateAct.getThought();
                String label = doc.getTextSegment(templateAct.getRange());

                instanceAct.getNeuron().setLabel(label);

                logInstantiation(instanceAct, label);
            }
        };
    }

    public static void main(String[] args) throws IOException {
        new SyllablesExperiment()
                .testTraining();
    }

    public void testTraining() throws IOException {
        List<String> inputs = new ArrayList<>();
        inputs.add("der");
        inputs.add("der");

        inputs.addAll(getInputs());
        train(inputs);
    }

    private void train(List<String> inputs) {
        Model model = new Model();

        AbstractTemplateModel syllableModel = new SyllableTemplateModel(model);

        model.setN(0);

        // Counting letters loop
        inputs.forEach(w -> {
            Document doc = initDocument(model, w);
            doc.getConfig()
                    .setTrainingEnabled(false)
                    .setMetaInstantiationEnabled(false)
                    .setCountingEnabled(true);

//            AIKADebugger.createAndShowGUI(doc);

            processTokens(
                    syllableModel,
                    doc,
                    convertToCharTokens(w),
                    0
            );

            doc.process(Phase.ANNEAL);

            doc.postProcessing();
            doc.updateModel();
            doc.disconnect();
        });

        syllableModel.initTemplates();

        int[] counter = new int[1];

        ExperimentLogger el = new ExperimentLogger();

        inputs.forEach(w -> {
            Document doc = initDocument(model, w);
            doc.getConfig()
                    .setTrainingEnabled(true)
                    .setMetaInstantiationEnabled(true)
                    .setCountingEnabled(true);

            AIKADebugger debugger = null;
            System.out.println(counter[0] + " " + w);
            if(counter[0] >= 49) {// 3, 6, 11, 18, 100, 39
                debugger = AIKADebugger.createAndShowGUI(doc);
            }

            if(w.equalsIgnoreCase("herankam")) {
              //  debugger = AIKADebugger.createAndShowGUI(doc);
            }

            processTokens(
                    syllableModel,
                    doc,
                    convertToCharTokens(w),
                    0
            );

            waitForClick(debugger);

            doc.process(Phase.INFERENCE);

            LoggingListener logger = null;

            if(debugger != null) {
                logger = new LoggingListener();
                doc.addEventListener(logger);
            }

            doc.close();

//            el.annealingLogInit(doc);

            doc.anneal();

            waitForClick(debugger);

            doc.instantiateTemplates();

            waitForClick(debugger);

            doc.close();

            doc.train();

            logPatternMatches(doc);

            el.log(doc);

            waitForClick(debugger);

            if(logger != null)
                doc.removeEventListener(logger);

            doc.postProcessing();
            doc.updateModel();
            doc.disconnect();

            counter[0]++;
        });

        el.close();
    }

    private static void logPatternMatches(Document doc) {
        doc.getActivations()
                .stream()
                .filter(act -> act instanceof PatternActivation)
                .filter(act -> !(act instanceof TokenActivation))
                .forEach(act ->
                        logPatternMatch((PatternActivation) act)
                );
    }

    private static void logPatternMatch(PatternActivation act) {
        if(act.getNetPreAnneal().getCurrentValue() <= 0.0 || act.isAbstract())
            return;

        System.out.println("   " +
                (act.isFired() ? "Matching " : "Inactive Match ") +
                (act.isAbstract() ? "abstract " : "") +
                act.getClass().getSimpleName() +
                " '" + act.getLabel() + "'" +
                (!act.isAbstract() ? " '" + LabelUtil.generateLabel(act.getNeuron()) + "'" : "") +
                " nId:" + act.getNeuron().getId() +
                " r:" + act.getRange() +
                " grad:" + doubleToString(act.getGradient().getCurrentValue(), "#.######")
        );
    }

    private static void logInstantiation(Activation instanceAct, String label) {
        if(instanceAct instanceof BindingActivation)
            return;

        System.out.println("   Instantiating " +
                instanceAct.getClass().getSimpleName() +
                " '" + label + "'" +
                " nId:" + instanceAct.getNeuron().getId() +
                " r:" + instanceAct.getRange()
        );
    }

    private static void waitForClick(AIKADebugger debugger) {
        if(debugger != null)
            debugger.getStepManager().waitForClick();
    }

    public List<String> convertToCharTokens(String w) {
        ArrayList<String> result = new ArrayList<>();
        for(char c: w.toCharArray()) {
            result.add("" + c);
        }
        return result;
    }

    private Document initDocument(Model m, String txt) {
        Document doc = new Document(m, txt);
        doc.setConfig(
                getConfig()
                        .setAlpha(null)
                        .setLearnRate(0.01)
                        .setTrainingEnabled(false)
        );

        return doc;
    }

    private List<String> getInputs() throws IOException {
        String[] files = new String[]{
                "Aschenputtel",
                "BruederchenUndSchwesterchen",
                "DasTapfereSchneiderlein",
                "DerFroschkoenig",
                "DerGestiefelteKater",
                "DerGoldeneSchluessel",
                "DerSuesseBrei",
                "DerTeufelMitDenDreiGoldenenHaaren",
                "DerWolfUndDieSiebenJungenGeisslein",
                "DieBremerStadtmusikanten",
                "DieDreiFedern",
                "DieSterntaler",
                "DieWeisseSchlange",
                "DieZwoelfBrueder",
                "Dornroeschen",
                "FrauHolle",
                "HaenselUndGretel",
                "HansImGlueck",
                "JorindeUndJoringel",
                "KatzeUndMausInGesellschaft",
                "MaerchenVonEinemDerAuszogDasFuerchtenZuLernen",
                "Marienkind",
                "Rapunzel",
                "Rotkaeppchen",
                "Rumpelstilzchen",
                "SchneeweisschenUndRosenrot",
                "Schneewitchen",
                "TischleinDeckDich",
                "VonDemFischerUndSeinerFrau"
        };

        ArrayList<String> inputs = new ArrayList<>();

        for (String fn : files) {
            InputStream is = getClass().getResourceAsStream("../corpora/public-domain-txt/" + fn + ".txt");
            assert is != null;

            StringWriter writer = new StringWriter();
            IOUtils.copy(is, writer, "UTF-8");
            String txt = writer.toString();

            txt = txt.replace('.', ' ');
            txt = txt.replace(',', ' ');
            txt = txt.replace('?', ' ');
            txt = txt.replace('!', ' ');
            txt = txt.replace('"', ' ');
            txt = txt.replace('-', ' ');
            txt = txt.replace(':', ' ');
            txt = txt.replace(';', ' ');
            txt = txt.replace('\n', ' ');
            txt = txt.replace("  ", " ");
            txt = txt.replace("  ", " ");

            for(String word: txt.split(" ")) {
                String w = word.toLowerCase().trim();
                if(w.isBlank())
                    continue;

                inputs.add(w);
            }
        }
        return inputs;
    }

}
