package com.github.sudu.persistentidecaches;

import com.github.sudu.persistentidecaches.records.Revision;

public interface Revisions {

    Revision getParent(Revision revision);

    Revision addRevision(Revision parent);

    Revision addLastRevision();

    Revision getCurrentRevision();

    void setCurrentRevision(Revision revision);
}
