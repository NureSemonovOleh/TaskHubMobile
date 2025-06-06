package com.nuresemonovoleh.taskhub.data.models

data class TaskGroup(
    val tasks: List<Task> = emptyList(),
    val name: String,
    val description: String,
)
