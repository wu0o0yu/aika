package network.aika.experiments.syllable;

import network.aika.Config;
import network.aika.Model;
import network.aika.debugger.AIKADebugger;
import network.aika.elements.activations.Activation;
import network.aika.elements.activations.CategoryActivation;
import network.aika.elements.activations.PatternActivation;
import network.aika.elements.activations.TokenActivation;
import network.aika.elements.links.CategoryLink;
import network.aika.elements.links.Link;
import network.aika.sign.Sign;
import network.aika.text.Document;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static network.aika.steps.Phase.ANNEAL;
import static network.aika.steps.Phase.INFERENCE;


public class SyllablesTest {

    public static void processTokens(SyllableTemplateModel m, Document doc, Iterable<String> tokens, int separatorLength) {
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

        PatternActivation maxSurprisalAct = tokenActs.stream().filter(PatternActivation.class::isInstance)
                .map(PatternActivation.class::cast)
                .max(Comparator.comparingDouble(act ->
                        act.getNeuron().getSurprisal(
                                Sign.POS,
                                act.getAbsoluteRange(),
                                true
                        )
                )).orElse(null);

        doc.setActivationCheckCallback(act -> {
            if (act == null)
                return true;

            Link l = act.getInputLink(m.letterCategory);
            if(l == null)
                return false;

            CategoryActivation cAct = (CategoryActivation) l.getInput();
            if (cAct == null)
                return false;

            CategoryLink catLink = (CategoryLink) cAct.getInputLinks().findAny().orElse(null);
            if(catLink == null)
                return false;

            return maxSurprisalAct == catLink.getInput();
        });

        for(TokenActivation tAct: tokenActs) {
            tAct.setNet(10.0);
        }
    }


    public static Config getConfig() {
        return new Config() {
            public String getLabel(Activation act) {
                Document doc = (Document) act.getThought();
                return doc.getTextSegment(act.getRange());
            }
        };
    }

    @Test
    public void testTraining() throws IOException {
        List<String> inputs = new ArrayList<>();
        inputs.add("der");
        inputs.add("der");

        inputs.addAll(getInputs());
        train(inputs);
    }

    private void train(List<String> inputs) {
        Model model = new Model();

        SyllableTemplateModel syllableModel = new SyllableTemplateModel(model);

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

            doc.process(ANNEAL);

            doc.postProcessing();
            doc.updateModel();
            doc.disconnect();
        });

        syllableModel.initMeta(
                0.7,
                pos ->
                        switch (pos) {
                            case 0 -> 2.5;
                            case 1 -> 2.0;
                            case -1 -> 1.8;
                            default -> pos > 0 ? 0.3 : 0.2;
                        },
                pos ->
                        2.0 + (3.0 * (1.0 / ((double) Math.abs(pos) + 1))),
                pos ->
                        1.0,
                pos ->
                        Math.abs(pos) <= 1 ? 5.0 : 1.0
        );

        int[] counter = new int[1];

        ExperimentLogger el = new ExperimentLogger(43, 31, 24, 70);

        inputs.forEach(w -> {
            Document doc = initDocument(model, w);
            doc.getConfig()
                    .setTrainingEnabled(true)
                    .setMetaInstantiationEnabled(true)
                    .setCountingEnabled(true);

            AIKADebugger debugger = null;
            System.out.println(counter[0] + " " + w);
            if(counter[0] >= 5) {// 3, 6
                debugger = AIKADebugger.createAndShowGUI(doc);
            }

            processTokens(
                    syllableModel,
                    doc,
                    convertToCharTokens(w),
                    0
            );

            waitForClick(debugger);

            doc.process(INFERENCE);

            LoggingListener logger = null;

            if(debugger != null) {
                logger = new LoggingListener();
                doc.addEventListener(logger);
            }

            doc.close();
            doc.anneal();

            doc.process(ANNEAL);

            waitForClick(debugger);

            doc.instantiateTemplates();

            waitForClick(debugger);

            doc.close();

            doc.train();

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
                        .setLearnRate(-0.01)
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
            InputStream is = getClass().getResourceAsStream("../../../../corpora/public-domain-txt/" + fn + ".txt");
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
