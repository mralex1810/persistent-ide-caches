package com.github.sudu.persistentidecaches.records;

public record LongInt(long l, int i) {

    @Override
    public int hashCode() {
        return (int)( 31 * l + i);
    }
}
