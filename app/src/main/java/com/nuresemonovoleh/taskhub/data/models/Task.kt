package com.nuresemonovoleh.taskhub.data.models

data class Task(
    val id: Int,
    val name: String,
    val attachments: List<Attachment> = emptyList(),
    val solutions: List<Solution> = emptyList(),
    val requests: List<Request> = emptyList(),
    val visibility: TaskVisibility
)
