package com.github.solayw.jpamigration

@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD,  AnnotationTarget.CLASS)
annotation class Comment(val value: String)
