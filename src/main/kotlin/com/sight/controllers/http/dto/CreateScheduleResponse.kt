package com.sight.controllers.http.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.sight.domain.schedule.Schedule
import com.sight.domain.schedule.ScheduleCategory
import com.sight.domain.seminar.BigSeminar

@JsonInclude(JsonInclude.Include.NON_NULL)
data class CreateScheduleResponse(
    val id: Long,
    val title: String,
    val category: ScheduleCategory,
    val location: String?,
    val state: String,
    val scheduledAt: String,
    val endAt: String,
    val expoint: Int,
    val checkCode: String?,
    val author: Long,
    val createdAt: String,
    // 세미나 일정에 한해 포함 (그 외 카테고리에서는 응답에서 누락)
    val isSummerSeason: Boolean?,
    val isSpeakAfter: Boolean?,
) {
    companion object {
        fun from(
            schedule: Schedule,
            bigSeminar: BigSeminar? = null,
        ): CreateScheduleResponse {
            return CreateScheduleResponse(
                id = schedule.id,
                title = schedule.title,
                category = schedule.category,
                location = schedule.location,
                state = schedule.state.state,
                scheduledAt = schedule.scheduledAt.toString(),
                endAt = schedule.endAt.toString(),
                expoint = schedule.expoint,
                checkCode = schedule.checkCode,
                author = schedule.author,
                createdAt = schedule.createdAt.toString(),
                isSummerSeason = bigSeminar?.isSummerSeason,
                isSpeakAfter = bigSeminar?.isSpeakAfter,
            )
        }
    }
}
