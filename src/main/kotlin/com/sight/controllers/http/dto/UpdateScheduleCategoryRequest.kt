package com.sight.controllers.http.dto

import com.sight.domain.schedule.ScheduleCategory
import jakarta.validation.constraints.NotNull

data class UpdateScheduleCategoryRequest(
    @field:NotNull
    val category: ScheduleCategory,
    // 변경 대상이 SEMINAR일 때만 필수. 그 외 카테고리에서는 무시된다. (서비스에서 검증)
    val isSummerSeason: Boolean?,
    val isSpeakAfter: Boolean?,
)
