package com.sight.domain.schedule

enum class ScheduleCategory(val label: String) {
    CLUB("동아리"),
    ACADEMIC("학사"),
    EXTERNAL("외부"),
    MANAGEMENT("운영"),
    GROUP_ACTIVITY("그룹활동"),
    BIG_SEMINAR("총회"),
    AFTERPARTY("뒷풀이"),
    OTHER("기타"),
    ;

    /** 일반 회원이 생성할 수 있는 그룹 활동 카테고리. */
    val isGroupActivity: Boolean get() = this == GROUP_ACTIVITY

    /** BigSeminar 레코드 처리가 동반되는 총회 카테고리. */
    val isBigSeminar: Boolean get() = this == BIG_SEMINAR

    /** 운영진 전용 카테고리(그룹 활동·총회 제외). */
    val isManagerCategory: Boolean get() = !isGroupActivity && !isBigSeminar
}
