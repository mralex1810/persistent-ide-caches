package com.github.sudu.persistentidecaches.utils

fun interface TriConsumer<T, U, V> {
    fun accept(t: T, u: U, v: V)
}
