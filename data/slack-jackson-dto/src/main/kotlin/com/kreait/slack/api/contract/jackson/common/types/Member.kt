package com.kreait.slack.api.contract.jackson.common.types

import com.fasterxml.jackson.annotation.JsonProperty
import com.kreait.slack.api.contract.jackson.util.InstantToInt
import com.kreait.slack.api.contract.jackson.util.JacksonDataClass
import java.time.Instant

@JacksonDataClass
data class Member(
        @JsonProperty("id") val id: String,
        @JsonProperty("team_id") val teamId: String,
        @JsonProperty("name") val name: String,
        @JsonProperty("deleted") val deleted: Boolean,
        @JsonProperty("color") val color: String?,
        @JsonProperty("real_name") val realName: String?,
        @JsonProperty("tz") val tz: String?,
        @JsonProperty("tz_label") val timezoneLabel: String?,
        @JsonProperty("tz_offset") val timezoneOffset: Int?,
        @JsonProperty("profile") val profile: UserProfile,
        @JsonProperty("is_admin") val isAdmin: Boolean,
        @JsonProperty("is_owner") val isOwner: Boolean,
        @JsonProperty("is_primary_owner") val isPrimaryOwner: Boolean,
        @JsonProperty("is_restricted") val isRestricted: Boolean,
        @JsonProperty("is_ultra_restricted") val isUltraRestricted: Boolean,
        @JsonProperty("is_bot") val isBot: Boolean,
        @InstantToInt @JsonProperty("updated") val lastModifiedAt: Instant,
        @JsonProperty("is_app_user") val isAppUser: Boolean,
        @JsonProperty("has_2fa") val has2fa: Boolean,
        @JsonProperty("locale") val locale: String?
) {
    companion object
}
