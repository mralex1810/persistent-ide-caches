package com.github.sudu.persistentidecaches.trigram;

import com.github.sudu.persistentidecaches.records.Trigram;
import com.github.sudu.persistentidecaches.utils.Counter;
import java.util.Map;

public class TrigramCounter extends Counter<Trigram> {

    public TrigramCounter() {
        super();
    }

    public TrigramCounter(final Map<Trigram, Integer> counter) {
        super(counter);
    }
}
