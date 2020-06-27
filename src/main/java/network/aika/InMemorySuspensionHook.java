package network.aika;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;


public class InMemorySuspensionHook implements SuspensionHook {

    private AtomicInteger currentId = new AtomicInteger(0);

    private Map<Long, byte[]> storage = new TreeMap<>();
    private final Map<String, Long> labels = new HashMap<>();

    @Override
    public long createId() {
        return currentId.addAndGet(1);
    }

    @Override
    public void store(Long id, byte[] data) {
        storage.put(id, data);
    }

    @Override
    public void storeLabel(String label, Long id) {
        labels.put(label, id);
    }

    @Override
    public byte[] retrieve(long id) {
        return storage.get(id);
    }

    @Override
    public Long getIdByLabel(String label) {
        return labels.get(label);
    }

    @Override
    public Stream<Long> getAllIds() {
        return storage.keySet().stream();
    }
}
