package com.github.solayw.jpamigration

import java.sql.Connection
import java.sql.ResultSet
import java.sql.Statement
import javax.sql.RowSet

fun <T> Connection.query(sql :String, cvt: (rs: ResultSet) -> T?): List<T?> {
    var state: Statement? = null
    var rs: ResultSet? = null
    try {
        state = createStatement()
        rs = state.executeQuery(sql)
        val res = ArrayList<T?>()
        while (rs.next()) {
            res.add(cvt(rs))
        }
        return res
    } finally {
        rs?.close()
        state?.close()
    }
}