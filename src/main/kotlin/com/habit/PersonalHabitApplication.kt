package com.habit

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class PersonalHabitApplication

fun main(args: Array<String>) {
    runApplication<PersonalHabitApplication>(*args)
}
