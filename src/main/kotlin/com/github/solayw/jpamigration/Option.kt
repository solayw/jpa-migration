package com.github.solayw.jpamigration

import java.lang.reflect.Field
import javax.persistence.Table

fun toUnderScope(s: String): String{
    val sb = StringBuilder()
    var first = true
    for (c in s.toCharArray()) {
        if(c in 'A'..'Z') {
            if(!first) {
                sb.append('_')
            }
            sb.append(c.toLowerCase())
        } else {
            sb.append(c)
        }
        first = false
    }
    return sb.toString()
}
val entityToName: (Class<*>) -> String = {
     toUnderScope(it.simpleName)
}

val filedToName: (Field) -> String = {
    toUnderScope(it.name)
}
class Option(
    val url: String,
    val username: String,
    val password: String,
    val schema: String,
    val scanPackage: String,
    val defaultTableName: (Class<*>) -> String = entityToName,
    val defaultColumnName: (Field) -> String = filedToName,
    val defaultStringDefinition: String = "varchar(255)",

    ) {
}