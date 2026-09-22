package me.mihaidubceac.simplechatapplication.model

import kotlinx.serialization.Serializable

@Serializable
data class ApiConnectRequest(
    val id: String,
    val name: String,
)
