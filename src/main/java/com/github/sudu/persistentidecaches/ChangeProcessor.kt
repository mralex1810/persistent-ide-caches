package com.github.sudu.persistentidecaches;

import com.github.sudu.persistentidecaches.changes.Change;
import java.util.List;

public interface ChangeProcessor {

    void prepare(List<? extends Change> changes);


    void processChanges(List<? extends Change> changes);
}
