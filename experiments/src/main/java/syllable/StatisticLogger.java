package syllable;

import network.aika.elements.activations.Activation;
import network.aika.elements.activations.LatentRelationActivation;
import network.aika.text.Document;
import network.aika.utils.Utils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static syllable.StatisticLogger.Key.*;

public class StatisticLogger  {



    CSVPrinter printer;

    Map<Key, int[]> counter = new TreeMap<>();

    public void open(File f)  {
        try {
            if(f.exists())
                f.delete();

            List<String> headerLabels = Arrays.stream(values())
                    .map(k -> k.name())
                    .collect(Collectors.toList());

            FileWriter fw = new FileWriter(f);
            printer = new CSVPrinter(fw, CSVFormat.DEFAULT
                    .withHeader(headerLabels.toArray(new String[0])));
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

    enum Key {
        ACTS,
        FIRED_ACTS,
        INACTIVE_ACTS,
        STRONG_ACTS,
        REL_ACTS,
        NEW_INSTANCES,
        ABSTRACT_ACTS,
    }

    private void count(Activation act) {
        count(ACTS);
        if(act.isFired())
            count(FIRED_ACTS);
        else
            count(INACTIVE_ACTS);

        if(act.getNet().getCurrentValue() > 0.9)
            count(STRONG_ACTS);

        if(act instanceof LatentRelationActivation)
            count(REL_ACTS);

        if(act.isNewInstance())
            count(NEW_INSTANCES);

        if(act.getNeuron().isAbstract())
            count(ABSTRACT_ACTS);
    }

    public void count(Key key) {
        int[] c = counter.computeIfAbsent(key, k -> new int[1]);
        c[0]++;
    }

    public void clearAllCounters() {
        counter.clear();
    }

    public void log(Document doc) {
        doc.getActivations()
                .forEach(act ->
                    count(act)
                );

        try {
            List<String> entry =
                    Arrays.stream(values())
                            .map(k -> "" + getCount(k))
                            .collect(Collectors.toList());

            printer.printRecord(entry.toArray());
            printer.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        clearAllCounters();
    }

    private int getCount(Key k) {
        int[] c = counter.get(k);
        if(c == null)
            return 0;
        return c[0];
    }
}
