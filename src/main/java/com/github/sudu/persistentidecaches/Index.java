package com.github.sudu.persistentidecaches;

import com.github.sudu.persistentidecaches.records.Revision;

public interface Index<Key, Value> extends ChangeProcessor {


    Value getValue(Key key, Revision revision);

    void checkout(Revision revision);
}
