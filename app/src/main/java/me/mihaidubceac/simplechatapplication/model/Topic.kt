package me.mihaidubceac.simplechatapplication.model

data class Topic(
    val id: String,
    val name: String,
)

val DefaultTopic = Topic(id = "general", name = "General")
