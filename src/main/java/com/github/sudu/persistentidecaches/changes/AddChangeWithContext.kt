package com.github.sudu.persistentidecaches.changes

import com.github.sudu.persistentidecaches.records.FilePointer

/**
 * Saves context of added string. Needs two or more chars of context from both sides of added string.
 */
class AddChangeWithContext(
    timestamp: Long, place: FilePointer?,
    addedString: String?,
    /**
     * Points on first char of added string.
     */
    val startIndex: Int,
    /**
     * Points on char next to last char of added string.
     */
    val endIndex: Int
) : AddChange(timestamp, place, addedString!!) {
    override fun toString(): String {
        return "AddChangeWithContext{" +
                "startIndex=" + startIndex +
                ", endIndex=" + endIndex +
                "} " + super.toString()
    }
}
