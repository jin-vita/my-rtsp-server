package com.pedro.sample


import com.google.gson.annotations.SerializedName

data class DeviceSaveResponse(
    @SerializedName("code")
    val code: Int,
    @SerializedName("data")
    val `data`: Data,
    @SerializedName("header")
    val header: Header,
    @SerializedName("message")
    val message: String
) {
    data class Data(
        @SerializedName("affectedRows")
        val affectedRows: Int,
        @SerializedName("changedRows")
        val changedRows: Int,
        @SerializedName("fieldCount")
        val fieldCount: Int,
        @SerializedName("insertId")
        val insertId: Int,
        @SerializedName("message")
        val message: String,
        @SerializedName("protocol41")
        val protocol41: Boolean,
        @SerializedName("serverStatus")
        val serverStatus: Int,
        @SerializedName("warningCount")
        val warningCount: Int
    )

    data class Header(
        @SerializedName("requestCode")
        val requestCode: String
    )
}