package com.github.solayw.jpamigration

import io.github.classgraph.ClassGraph
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.sql.DriverManager
import java.util.*
import javax.persistence.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

open class Generator(private val option: Option) {
    fun generate() {
        val connection = DriverManager.getConnection(option.url, option.username, option.password)
        val tables = HashMap<String, MutableList<ColumnDefinition>>()
        connection.query("select * from information_schema.COLUMNS where TABLE_SCHEMA='${option.schema}' order by ORDINAL_POSITION") {
            with(it) {
                val tableName = getString("TABLE_NAME")
                val c = ColumnDefinition(
                    tableName,
                    getString("IS_NULLABLE").equals("YES"),
                    getString("COLUMN_TYPE")
                )
                var list = tables[tableName]
                if (list == null) {
                    list = ArrayList<ColumnDefinition>()
                    tables[tableName] = list
                }
                list.add(c)
            }
        }
        val ddl = ArrayList<String>()
        ClassGraph()
            .enableAllInfo()
            .acceptPackages(option.scanPackage)
            .scan().use { scanResult ->
                for (routeClassInfo in scanResult.getClassesWithAnnotation("javax.persistence.Entity")) {
                    val clazz = routeClassInfo.loadClass()
                    val tableAnno = clazz.getAnnotation(Table::class.java)
                    val tableName = tableAnno?.name ?: option.defaultTableName(clazz)
                    val tableExists = tables.containsKey(tableName)
                    val definitions = allDefinitions(clazz, null)
                    if(!tableExists) {
                        var sql = StringBuilder("create table(")
                        for ((idx, definition) in definitions.withIndex()) {
                            var s = "\r\n\t" + definition.ddl()
                            if (idx != definitions.lastIndex) {
                                s += ","
                            }
                            sql.append(s)
                        }
                        sql.append("\r\n);");
                        ddl.add(sql.toString())
                    } else {
                        val existingColumns = HashMap<String, ColumnDefinition>()
                        tables[tableName]!!.forEach {
                            existingColumns[it.name] = it
                        }
                        for ((idx, def) in definitions.withIndex()) {
                            val existingColumn = existingColumns.get(def.name)
                            if(existingColumn == null) {
                                ddl.add(
                                    "alter table `$tableName` add column ${def.ddl()};"
                                )
                            } else {
                                if(!existingColumn.same(def)) {
                                    ddl.add(
                                        "alter table `$tableName` modify ${def.ddl()};"
                                    )
                                }
                            }
                            existingColumns.remove(def.name)
                        }
                        existingColumns.forEach{
                            ddl.add(
                                "alter table drop `$it`;"
                            )
                        }
                    }
                }
            }
    }



    private fun allDefinitions(clazz: Class<*>, override: AttributeOverrides? = null): List<ColumnDefinition> {
        val fields = ArrayList<Field>()
        var _clazz = clazz
        while (_clazz != Object::class.java) {
            fields.addAll(_clazz.declaredFields.filter {
                !Modifier.isStatic(it.modifiers)
                        && !Modifier.isTransient(it.modifiers)
                        && it.getAnnotation(Transient::class.java) == null
            })
            _clazz = _clazz.superclass
        }
        val def = ArrayList<ColumnDefinition>()
        fields.forEach { field ->
            val o = override?.value?.find { it.name == field.name  }
            def.addAll(columnDefinition(field, o?.column))
        }
        return def
    }
    private fun columnDefinition(f: Field, override: Column?): List<ColumnDefinition> {
        val list = ArrayList<ColumnDefinition>()
        val embeded = f.getAnnotation(Embedded::class.java)
        if(embeded != null) {
            if(override != null) {
                throw RuntimeException("${f.toString()} is embedded and should not mark as Column")
            }
            list.addAll(allDefinitions(f.type, null))
        } else {
            val columnAnno = override?: f.getAnnotation(Column::class.java)
            val name = columnAnno?.name ?: option.defaultColumnName(f)
            val nullable = columnAnno?.nullable ?: !f.type.isPrimitive
            val def = columnAnno?.columnDefinition ?: definitionFromType(f)
            list.add(ColumnDefinition(name, nullable, def))
        }
        return list
    }
    private fun definitionFromType(f: Field) :String {
        val clazz = f.type
        if(clazz == Int::class.java) {
            return "int"
        } else if(clazz == Long::class.java) {
            return "bigint"
        } else if(clazz == String::class.java) {
            return option.defaultStringDefinition
        }
        throw RuntimeException("unknown column type for " + f.toString())
    }






}



