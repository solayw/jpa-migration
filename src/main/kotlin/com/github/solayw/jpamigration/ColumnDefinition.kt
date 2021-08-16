package com.github.solayw.jpamigration

class ColumnDefinition(
    val name: String,
    val nullable: Boolean,
    val columnType: String,
) {
    fun ddl(): String {
        var s = "'$name' $columnType"
        if(!nullable) {
            s += " not null"
        }
        return s
    }

    fun same(other: ColumnDefinition): Boolean {
        return name == other.name
                && nullable == other.nullable
                && columnType == other.columnType
    }
}