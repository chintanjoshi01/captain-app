package com.eresto.captain.model

import com.google.gson.annotations.SerializedName

data class CommonKey<T>(
    @SerializedName("data")
    var data: T?,
    @SerializedName("status")
    var status: Int,
    @SerializedName("message")
    var message: String?
)

