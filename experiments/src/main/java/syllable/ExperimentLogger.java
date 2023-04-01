package syllable;

import network.aika.elements.activations.PatternActivation;
import network.aika.text.Document;

import java.io.File;
import java.util.TreeMap;

public class ExperimentLogger {

    File experimentPath = new File("experiments/src/main/resources/experiments/");

    TreeMap<Long, PatternLogger> patternLogger = new TreeMap<>();
    StatisticLogger statLogger = new StatisticLogger();

    public ExperimentLogger(long... patternNeuronIds) {
        statLogger.open(new File(experimentPath, "statistic.csv"));

        for(long id: patternNeuronIds) {
            PatternLogger pl = new PatternLogger();
            pl.open(new File(experimentPath, "pattern-" + id + ".csv"));
            patternLogger.put(id, pl);
        }
    }

    public void log(Document doc) {
        statLogger.log(doc);

        doc.getActivations()
                .stream()
                .filter(act -> act instanceof PatternActivation)
                .map(act -> (PatternActivation)act)
                .forEach(act -> {
                    PatternLogger pl = patternLogger.get(act.getNeuron().getId());
                    if(pl != null)
                        pl.log(act);
                });
    }

    public void close() {
        statLogger.close();

        patternLogger.values()
                .forEach(pl -> pl.close());
    }
}
