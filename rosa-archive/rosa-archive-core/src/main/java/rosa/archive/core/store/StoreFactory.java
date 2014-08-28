package rosa.archive.core.store;

import rosa.archive.core.ByteStreamGroup;

public interface StoreFactory {
    Store create(ByteStreamGroup bsg);
}
