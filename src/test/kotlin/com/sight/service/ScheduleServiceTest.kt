package com.sight.service

import com.sight.core.auth.Requester
import com.sight.core.auth.UserRole
import com.sight.core.exception.BadRequestException
import com.sight.core.exception.ConflictException
import com.sight.core.exception.ForbiddenException
import com.sight.core.exception.NotFoundException
import com.sight.core.exception.UnauthorizedException
import com.sight.domain.group.Group
import com.sight.domain.group.GroupAccessGrade
import com.sight.domain.group.GroupCategory
import com.sight.domain.group.GroupState
import com.sight.domain.member.Member
import com.sight.domain.member.StudentStatus
import com.sight.domain.member.UserStatus
import com.sight.domain.schedule.Schedule
import com.sight.domain.schedule.ScheduleCategory
import com.sight.domain.schedule.ScheduleMemberApply
import com.sight.domain.schedule.ScheduleState
import com.sight.domain.seminar.BigSeminar
import com.sight.repository.BigSeminarRepository
import com.sight.repository.GroupMemberRepository
import com.sight.repository.GroupRepository
import com.sight.repository.MemberRepository
import com.sight.repository.ScheduleMemberApplyRepository
import com.sight.repository.ScheduleRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ScheduleServiceTest {
    private val scheduleRepository: ScheduleRepository = mock()
    private val scheduleMemberApplyRepository: ScheduleMemberApplyRepository = mock()
    private val memberRepository: MemberRepository = mock()
    private val bigSeminarRepository: BigSeminarRepository = mock()
    private val groupMemberRepository: GroupMemberRepository = mock()
    private val groupRepository: GroupRepository = mock()
    private val pointService: PointService = mock()
    private lateinit var scheduleService: ScheduleService

    @BeforeEach
    fun setUp() {
        scheduleService =
            ScheduleService(
                scheduleRepository = scheduleRepository,
                scheduleMemberApplyRepository = scheduleMemberApplyRepository,
                memberRepository = memberRepository,
                bigSeminarRepository = bigSeminarRepository,
                groupMemberRepository = groupMemberRepository,
                groupRepository = groupRepository,
                pointService = pointService,
            )
    }

    @Test
    fun `listSchedules는 지정된 시간 이후의 일정 목록을 반환한다`() {
        // given
        val from = LocalDateTime.of(2024, 1, 1, 0, 0)
        val limit = 5
        val schedules =
            listOf(
                Schedule(
                    id = 1L,
                    category = ScheduleCategory.CLUB,
                    title = "동아리 정기 모임",
                    author = 1L,
                    state = ScheduleState.PUBLIC,
                    scheduledAt = LocalDateTime.of(2024, 1, 2, 14, 0),
                    endAt = LocalDateTime.of(2024, 1, 2, 16, 0),
                ),
                Schedule(
                    id = 2L,
                    category = ScheduleCategory.BIG_SEMINAR,
                    title = "총회",
                    author = 2L,
                    state = ScheduleState.PUBLIC,
                    scheduledAt = LocalDateTime.of(2024, 1, 3, 18, 0),
                    endAt = LocalDateTime.of(2024, 1, 3, 20, 0),
                ),
            )
        given(scheduleRepository.findUpcoming(any(), any())).willReturn(schedules)

        // when
        val result = scheduleService.listSchedules(from, limit)

        // then
        assertEquals(2, result.size)
        assertEquals("동아리 정기 모임", result[0].title)
        assertEquals("총회", result[1].title)
        verify(scheduleRepository).findUpcoming(any(), any())
    }

    @Test
    fun `listSchedules는 from이 null이면 findAllActive를 호출한다`() {
        given(scheduleRepository.findAllActive(any())).willReturn(emptyList())

        scheduleService.listSchedules(null, 50)

        verify(scheduleRepository).findAllActive(any())
    }

    @Test
    fun `listSchedules는 일정이 없을 때 빈 목록을 반환한다`() {
        // given
        val from = LocalDateTime.of(2024, 1, 1, 0, 0)
        val limit = 5
        given(scheduleRepository.findUpcoming(any(), any())).willReturn(emptyList())

        // when
        val result = scheduleService.listSchedules(from, limit)

        // then
        assertTrue(result.isEmpty())
        verify(scheduleRepository).findUpcoming(any(), any())
    }

    @Test
    fun `getScheduleById는 존재하는 일정을 반환한다`() {
        val schedule =
            Schedule(
                id = 1L,
                category = ScheduleCategory.CLUB,
                title = "동아리 정기 모임",
                author = 1L,
                state = ScheduleState.PUBLIC,
                scheduledAt = LocalDateTime.of(2026, 5, 18, 14, 0),
                endAt = LocalDateTime.of(2026, 5, 18, 16, 0),
            )
        given(scheduleRepository.findActiveById(1L)).willReturn(schedule)

        val result = scheduleService.getScheduleById(1L)

        assertEquals(1L, result.id)
        assertEquals("동아리 정기 모임", result.title)
        verify(scheduleRepository).findActiveById(1L)
    }

    @Test
    fun `getScheduleById는 존재하지 않는 일정에 NotFoundException을 던진다`() {
        given(scheduleRepository.findActiveById(999L)).willReturn(null)

        assertThrows<NotFoundException> {
            scheduleService.getScheduleById(999L)
        }
    }

    @Test
    fun `getScheduleWithDetails는 작성자명과 그룹명을 함께 반환한다`() {
        val schedule =
            Schedule(
                id = 1L,
                category = ScheduleCategory.GROUP_ACTIVITY,
                title = "스터디",
                author = 10L,
                state = ScheduleState.PUBLIC,
                scheduledAt = LocalDateTime.of(2026, 5, 18, 14, 0),
                endAt = LocalDateTime.of(2026, 5, 18, 16, 0),
                groupId = 20L,
            )
        val member =
            Member(
                id = 10L,
                name = "khlug_user",
                admission = "21",
                realname = "홍길동",
                college = "소프트웨어융합대학",
                grade = 3L,
                studentStatus = StudentStatus.UNDERGRADUATE,
                status = UserStatus.ACTIVE,
            )
        val group =
            Group(
                id = 20L,
                category = GroupCategory.STUDY,
                title = "코틀린 스터디",
                author = 10L,
                master = 10L,
                state = GroupState.PROGRESS,
                grade = GroupAccessGrade.MEMBER,
            )
        given(scheduleRepository.findActiveById(1L)).willReturn(schedule)
        given(memberRepository.findById(10L)).willReturn(Optional.of(member))
        given(groupRepository.findById(20L)).willReturn(Optional.of(group))

        val (resultSchedule, authorName, groupTitle) = scheduleService.getScheduleWithDetails(1L)

        assertEquals(1L, resultSchedule.id)
        assertEquals("khlug_user", authorName)
        assertEquals("코틀린 스터디", groupTitle)
    }

    @Test
    fun `getScheduleWithDetails는 groupId가 null이면 groupTitle이 null이다`() {
        val schedule =
            Schedule(
                id = 1L,
                category = ScheduleCategory.CLUB,
                title = "동아리 모임",
                author = 10L,
                state = ScheduleState.PUBLIC,
                scheduledAt = LocalDateTime.of(2026, 5, 18, 14, 0),
                endAt = LocalDateTime.of(2026, 5, 18, 16, 0),
                groupId = null,
            )
        val member =
            Member(
                id = 10L,
                name = "khlug_user",
                admission = "21",
                realname = "홍길동",
                college = "소프트웨어융합대학",
                grade = 3L,
                studentStatus = StudentStatus.UNDERGRADUATE,
                status = UserStatus.ACTIVE,
            )
        given(scheduleRepository.findActiveById(1L)).willReturn(schedule)
        given(memberRepository.findById(10L)).willReturn(Optional.of(member))

        val (_, authorName, groupTitle) = scheduleService.getScheduleWithDetails(1L)

        assertEquals("khlug_user", authorName)
        assertNull(groupTitle)
        verify(groupRepository, never()).findById(any())
    }

    @Test
    fun `createSchedule은 generateCheckCode가 false면 checkCode를 null로 저장한다`() {
        val requester = Requester(userId = 1L, role = UserRole.MANAGER)
        given(scheduleRepository.save(any<Schedule>())).willAnswer { it.arguments[0] as Schedule }

        val result =
            scheduleService.createSchedule(
                requester = requester,
                title = "test",
                category = ScheduleCategory.CLUB,
                location = "khlug_406",
                scheduledAt = LocalDateTime.of(2026, 5, 18, 14, 0),
                endAt = LocalDateTime.of(2026, 5, 18, 16, 0),
                expoint = 10,
                generateCheckCode = false,
            )

        assertEquals(ScheduleCategory.CLUB, result.category)
        assertEquals(1L, result.author)
        assertEquals("khlug_406", result.location)
        assertNull(result.checkCode)
        verify(scheduleRepository).save(any<Schedule>())
    }

    @Test
    fun `createSchedule은 generateCheckCode가 true면 4자리 숫자 checkCode를 생성한다`() {
        val requester = Requester(userId = 1L, role = UserRole.MANAGER)
        given(scheduleRepository.save(any<Schedule>())).willAnswer { it.arguments[0] as Schedule }

        val result =
            scheduleService.createSchedule(
                requester = requester,
                title = "test",
                category = ScheduleCategory.CLUB,
                location = null,
                scheduledAt = LocalDateTime.of(2026, 5, 18, 14, 0),
                endAt = LocalDateTime.of(2026, 5, 18, 16, 0),
                expoint = 0,
                generateCheckCode = true,
            )

        val checkCode = result.checkCode
        assertNotNull(checkCode)
        assertTrue(checkCode.matches(Regex("^\\d{4}$")))
    }

    @Test
    fun `createSchedule은 운영진 카테고리가 아니면 BadRequestException 던진다`() {
        val requester = Requester(userId = 1L, role = UserRole.MANAGER)

        listOf(ScheduleCategory.GROUP_ACTIVITY, ScheduleCategory.BIG_SEMINAR).forEach { category ->
            assertThrows<BadRequestException> {
                scheduleService.createSchedule(
                    requester = requester,
                    title = "test",
                    category = category,
                    location = null,
                    scheduledAt = LocalDateTime.of(2026, 5, 18, 14, 0),
                    endAt = LocalDateTime.of(2026, 5, 18, 16, 0),
                    expoint = 0,
                    generateCheckCode = false,
                )
            }
        }
    }

    @Test
    fun `createSchedule은 endAt이 scheduledAt 이후가 아니면 BadRequestException 던진다`() {
        val requester = Requester(userId = 1L, role = UserRole.MANAGER)

        assertThrows<BadRequestException> {
            scheduleService.createSchedule(
                requester = requester,
                title = "test",
                category = ScheduleCategory.CLUB,
                location = null,
                scheduledAt = LocalDateTime.of(2026, 5, 18, 16, 0),
                endAt = LocalDateTime.of(2026, 5, 18, 14, 0),
                expoint = 0,
                generateCheckCode = false,
            )
        }
    }

    @Test
    fun `createGroupActivitySchedule은 그룹 멤버이면 GROUP_ACTIVITY로 expoint 0 checkCode 없이 생성한다`() {
        val requester = Requester(userId = 1L, role = UserRole.USER)
        val group =
            Group(
                id = 10L,
                category = GroupCategory.STUDY,
                title = "스터디",
                author = 1L,
                master = 1L,
                state = GroupState.PROGRESS,
                grade = GroupAccessGrade.MEMBER,
            )
        given(groupRepository.findById(10L)).willReturn(Optional.of(group))
        given(groupMemberRepository.existsByGroupIdAndMemberId(10L, 1L)).willReturn(true)
        given(scheduleRepository.save(any<Schedule>())).willAnswer { it.arguments[0] as Schedule }

        val result =
            scheduleService.createGroupActivitySchedule(
                requester = requester,
                title = "스터디",
                location = null,
                scheduledAt = LocalDateTime.of(2026, 5, 18, 14, 0),
                endAt = LocalDateTime.of(2026, 5, 18, 16, 0),
                groupId = 10L,
            )

        assertEquals(ScheduleCategory.GROUP_ACTIVITY, result.category)
        assertEquals(0, result.expoint)
        assertEquals(1L, result.author)
        assertEquals(10L, result.groupId)
        assertNull(result.checkCode)
    }

    @Test
    fun `createGroupActivitySchedule은 해당 그룹 멤버가 아니면 ForbiddenException을 던진다`() {
        val requester = Requester(userId = 1L, role = UserRole.USER)
        val group =
            Group(
                id = 10L,
                category = GroupCategory.STUDY,
                title = "스터디",
                author = 1L,
                master = 1L,
                state = GroupState.PROGRESS,
                grade = GroupAccessGrade.MEMBER,
            )
        given(groupRepository.findById(10L)).willReturn(Optional.of(group))
        given(groupMemberRepository.existsByGroupIdAndMemberId(10L, 1L)).willReturn(false)

        assertThrows<ForbiddenException> {
            scheduleService.createGroupActivitySchedule(
                requester = requester,
                title = "스터디",
                location = null,
                scheduledAt = LocalDateTime.of(2026, 5, 18, 14, 0),
                endAt = LocalDateTime.of(2026, 5, 18, 16, 0),
                groupId = 10L,
            )
        }

        verify(scheduleRepository, never()).save(any())
    }

    @Test
    fun `createBigSeminarSchedule은 BIG_SEMINAR 일정과 빅세미나 레코드를 함께 생성한다`() {
        val requester = Requester(userId = 1L, role = UserRole.MANAGER)
        given(scheduleRepository.save(any<Schedule>())).willAnswer { it.arguments[0] as Schedule }
        given(bigSeminarRepository.save(any<BigSeminar>())).willAnswer { it.arguments[0] as BigSeminar }

        val (schedule, bigSeminar) =
            scheduleService.createBigSeminarSchedule(
                requester = requester,
                title = "총회",
                location = null,
                scheduledAt = LocalDateTime.of(2026, 5, 18, 14, 0),
                endAt = LocalDateTime.of(2026, 5, 18, 16, 0),
                expoint = 50,
                generateCheckCode = false,
                isSummerSeason = true,
                isSpeakAfter = false,
            )

        assertEquals(ScheduleCategory.BIG_SEMINAR, schedule.category)
        assertEquals(schedule.id, bigSeminar.scheduleId)
        assertTrue(bigSeminar.isSummerSeason)
        assertFalse(bigSeminar.isSpeakAfter)
        verify(bigSeminarRepository).save(any<BigSeminar>())
    }

    @Test
    fun `updateSchedule은 운영진 카테고리 일정을 수정하며 카테고리는 유지된다`() {
        val existing = scheduleOf(category = ScheduleCategory.CLUB, checkCode = "1234")
        given(scheduleRepository.findActiveById(1L)).willReturn(existing)
        given(scheduleRepository.save(any<Schedule>())).willAnswer { it.arguments[0] as Schedule }
        val requester = Requester(userId = 99L, role = UserRole.MANAGER)

        val result =
            scheduleService.updateSchedule(
                requester = requester,
                id = 1L,
                title = "new",
                location = "khlug_406",
                scheduledAt = LocalDateTime.of(2026, 5, 20, 14, 0),
                endAt = LocalDateTime.of(2026, 5, 20, 16, 0),
                expoint = 5,
            )

        assertEquals("new", result.title)
        assertEquals(ScheduleCategory.CLUB, result.category)
        assertEquals("1234", result.checkCode)
        assertEquals(5, result.expoint)
    }

    @Test
    fun `updateSchedule은 대상이 운영진 카테고리가 아니면 BadRequestException 던진다`() {
        val existing = scheduleOf(category = ScheduleCategory.GROUP_ACTIVITY)
        given(scheduleRepository.findActiveById(1L)).willReturn(existing)
        val requester = Requester(userId = 1L, role = UserRole.MANAGER)

        assertThrows<BadRequestException> {
            scheduleService.updateSchedule(
                requester = requester,
                id = 1L,
                title = "x",
                location = null,
                scheduledAt = LocalDateTime.of(2026, 5, 18, 14, 0),
                endAt = LocalDateTime.of(2026, 5, 18, 16, 0),
                expoint = 0,
            )
        }
    }

    @Test
    fun `updateSchedule은 없는 일정에 NotFoundException 던진다`() {
        given(scheduleRepository.findActiveById(999L)).willReturn(null)
        val requester = Requester(userId = 1L, role = UserRole.MANAGER)

        assertThrows<NotFoundException> {
            scheduleService.updateSchedule(
                requester = requester,
                id = 999L,
                title = "x",
                location = null,
                scheduledAt = LocalDateTime.of(2026, 5, 18, 14, 0),
                endAt = LocalDateTime.of(2026, 5, 18, 16, 0),
                expoint = 0,
            )
        }
    }

    @Test
    fun `updateGroupActivitySchedule은 본인 작성 그룹활동을 수정한다`() {
        val existing = scheduleOf(category = ScheduleCategory.GROUP_ACTIVITY, author = 10L)
        given(scheduleRepository.findActiveById(1L)).willReturn(existing)
        given(scheduleRepository.save(any<Schedule>())).willAnswer { it.arguments[0] as Schedule }
        val requester = Requester(userId = 10L, role = UserRole.USER)

        val result =
            scheduleService.updateGroupActivitySchedule(
                requester = requester,
                id = 1L,
                title = "new",
                location = null,
                scheduledAt = LocalDateTime.of(2026, 5, 20, 14, 0),
                endAt = LocalDateTime.of(2026, 5, 20, 16, 0),
            )

        assertEquals("new", result.title)
        assertEquals(ScheduleCategory.GROUP_ACTIVITY, result.category)
    }

    @Test
    fun `updateGroupActivitySchedule은 USER가 타인 작성 일정을 수정하면 ForbiddenException 던진다`() {
        val existing = scheduleOf(category = ScheduleCategory.GROUP_ACTIVITY, author = 10L)
        given(scheduleRepository.findActiveById(1L)).willReturn(existing)
        val requester = Requester(userId = 99L, role = UserRole.USER)

        assertThrows<ForbiddenException> {
            scheduleService.updateGroupActivitySchedule(
                requester = requester,
                id = 1L,
                title = "x",
                location = null,
                scheduledAt = LocalDateTime.of(2026, 5, 18, 14, 0),
                endAt = LocalDateTime.of(2026, 5, 18, 16, 0),
            )
        }
    }

    @Test
    fun `updateGroupActivitySchedule은 대상이 그룹활동이 아니면 BadRequestException 던진다`() {
        val existing = scheduleOf(category = ScheduleCategory.CLUB, author = 10L)
        given(scheduleRepository.findActiveById(1L)).willReturn(existing)
        val requester = Requester(userId = 10L, role = UserRole.MANAGER)

        assertThrows<BadRequestException> {
            scheduleService.updateGroupActivitySchedule(
                requester = requester,
                id = 1L,
                title = "x",
                location = null,
                scheduledAt = LocalDateTime.of(2026, 5, 18, 14, 0),
                endAt = LocalDateTime.of(2026, 5, 18, 16, 0),
            )
        }
    }

    @Test
    fun `updateBigSeminarSchedule은 총회 일정과 빅세미나 정보를 갱신한다`() {
        val existing = scheduleOf(category = ScheduleCategory.BIG_SEMINAR)
        given(scheduleRepository.findActiveById(1L)).willReturn(existing)
        given(scheduleRepository.save(any<Schedule>())).willAnswer { it.arguments[0] as Schedule }
        given(bigSeminarRepository.findByScheduleId(1L)).willReturn(null)
        given(bigSeminarRepository.save(any<BigSeminar>())).willAnswer { it.arguments[0] as BigSeminar }
        val requester = Requester(userId = 1L, role = UserRole.MANAGER)

        val (schedule, bigSeminar) =
            scheduleService.updateBigSeminarSchedule(
                requester = requester,
                id = 1L,
                title = "new",
                location = null,
                scheduledAt = LocalDateTime.of(2026, 5, 20, 14, 0),
                endAt = LocalDateTime.of(2026, 5, 20, 16, 0),
                expoint = 50,
                isSummerSeason = false,
                isSpeakAfter = true,
            )

        assertEquals("new", schedule.title)
        assertEquals(ScheduleCategory.BIG_SEMINAR, schedule.category)
        assertFalse(bigSeminar.isSummerSeason)
        assertTrue(bigSeminar.isSpeakAfter)
    }

    @Test
    fun `updateBigSeminarSchedule은 대상이 총회가 아니면 BadRequestException 던진다`() {
        val existing = scheduleOf(category = ScheduleCategory.CLUB)
        given(scheduleRepository.findActiveById(1L)).willReturn(existing)
        val requester = Requester(userId = 1L, role = UserRole.MANAGER)

        assertThrows<BadRequestException> {
            scheduleService.updateBigSeminarSchedule(
                requester = requester,
                id = 1L,
                title = "x",
                location = null,
                scheduledAt = LocalDateTime.of(2026, 5, 18, 14, 0),
                endAt = LocalDateTime.of(2026, 5, 18, 16, 0),
                expoint = 0,
                isSummerSeason = true,
                isSpeakAfter = true,
            )
        }
    }

    @Test
    fun `updateScheduleCategory는 BIG_SEMINAR로 변경 시 빅세미나 레코드를 생성한다`() {
        val existing = scheduleOf(category = ScheduleCategory.CLUB)
        given(scheduleRepository.findActiveById(1L)).willReturn(existing)
        given(scheduleRepository.save(any<Schedule>())).willAnswer { it.arguments[0] as Schedule }
        given(bigSeminarRepository.findByScheduleId(1L)).willReturn(null)
        given(bigSeminarRepository.save(any<BigSeminar>())).willAnswer { it.arguments[0] as BigSeminar }
        val requester = Requester(userId = 1L, role = UserRole.MANAGER)

        val (schedule, bigSeminar) =
            scheduleService.updateScheduleCategory(
                requester = requester,
                id = 1L,
                category = ScheduleCategory.BIG_SEMINAR,
                isSummerSeason = true,
                isSpeakAfter = false,
            )

        assertEquals(ScheduleCategory.BIG_SEMINAR, schedule.category)
        assertNotNull(bigSeminar)
        verify(bigSeminarRepository).save(any<BigSeminar>())
    }

    @Test
    fun `updateScheduleCategory는 BIG_SEMINAR로 변경하는데 빅세미나 필드가 없으면 BadRequestException 던진다`() {
        val existing = scheduleOf(category = ScheduleCategory.CLUB)
        given(scheduleRepository.findActiveById(1L)).willReturn(existing)
        given(scheduleRepository.save(any<Schedule>())).willAnswer { it.arguments[0] as Schedule }
        val requester = Requester(userId = 1L, role = UserRole.MANAGER)

        assertThrows<BadRequestException> {
            scheduleService.updateScheduleCategory(
                requester = requester,
                id = 1L,
                category = ScheduleCategory.BIG_SEMINAR,
                isSummerSeason = null,
                isSpeakAfter = null,
            )
        }
    }

    @Test
    fun `updateScheduleCategory는 BIG_SEMINAR에서 다른 카테고리로 변경 시 빅세미나 레코드를 삭제한다`() {
        val existing = scheduleOf(category = ScheduleCategory.BIG_SEMINAR)
        given(scheduleRepository.findActiveById(1L)).willReturn(existing)
        given(scheduleRepository.save(any<Schedule>())).willAnswer { it.arguments[0] as Schedule }
        val requester = Requester(userId = 1L, role = UserRole.MANAGER)

        val (schedule, bigSeminar) =
            scheduleService.updateScheduleCategory(
                requester = requester,
                id = 1L,
                category = ScheduleCategory.CLUB,
                isSummerSeason = null,
                isSpeakAfter = null,
            )

        assertEquals(ScheduleCategory.CLUB, schedule.category)
        assertNull(bigSeminar)
        verify(bigSeminarRepository).deleteByScheduleId(1L)
    }

    @Test
    fun `updateScheduleCategory는 GROUP_ACTIVITY로 변경 시 BadRequestException 던진다`() {
        val requester = Requester(userId = 1L, role = UserRole.MANAGER)

        assertThrows<BadRequestException> {
            scheduleService.updateScheduleCategory(
                requester = requester,
                id = 1L,
                category = ScheduleCategory.GROUP_ACTIVITY,
                isSummerSeason = null,
                isSpeakAfter = null,
            )
        }
    }

    @Test
    fun `deleteSchedule은 운영진 일정 state를 TRASH로 전환한다`() {
        val existing = scheduleOf(category = ScheduleCategory.CLUB)
        given(scheduleRepository.findActiveById(1L)).willReturn(existing)
        given(scheduleRepository.save(any<Schedule>())).willAnswer { it.arguments[0] as Schedule }
        val requester = Requester(userId = 99L, role = UserRole.MANAGER)

        scheduleService.deleteSchedule(requester, 1L)

        val captor = org.mockito.kotlin.argumentCaptor<Schedule>()
        verify(scheduleRepository).save(captor.capture())
        assertEquals(ScheduleState.TRASH, captor.firstValue.state)
    }

    @Test
    fun `deleteSchedule은 대상이 운영진 카테고리가 아니면 BadRequestException 던진다`() {
        val existing = scheduleOf(category = ScheduleCategory.GROUP_ACTIVITY)
        given(scheduleRepository.findActiveById(1L)).willReturn(existing)
        val requester = Requester(userId = 1L, role = UserRole.MANAGER)

        assertThrows<BadRequestException> {
            scheduleService.deleteSchedule(requester, 1L)
        }
    }

    @Test
    fun `deleteSchedule은 없는 일정에 NotFoundException 던진다`() {
        given(scheduleRepository.findActiveById(999L)).willReturn(null)
        val requester = Requester(userId = 1L, role = UserRole.MANAGER)

        assertThrows<NotFoundException> {
            scheduleService.deleteSchedule(requester, 999L)
        }
    }

    @Test
    fun `deleteGroupActivitySchedule은 본인 작성 그룹활동을 TRASH로 전환한다`() {
        val existing = scheduleOf(category = ScheduleCategory.GROUP_ACTIVITY, author = 10L)
        given(scheduleRepository.findActiveById(1L)).willReturn(existing)
        given(scheduleRepository.save(any<Schedule>())).willAnswer { it.arguments[0] as Schedule }
        val requester = Requester(userId = 10L, role = UserRole.USER)

        scheduleService.deleteGroupActivitySchedule(requester, 1L)

        val captor = org.mockito.kotlin.argumentCaptor<Schedule>()
        verify(scheduleRepository).save(captor.capture())
        assertEquals(ScheduleState.TRASH, captor.firstValue.state)
    }

    @Test
    fun `deleteGroupActivitySchedule은 USER가 타인 작성 일정을 삭제하면 ForbiddenException 던진다`() {
        val existing = scheduleOf(category = ScheduleCategory.GROUP_ACTIVITY, author = 10L)
        given(scheduleRepository.findActiveById(1L)).willReturn(existing)
        val requester = Requester(userId = 99L, role = UserRole.USER)

        assertThrows<ForbiddenException> {
            scheduleService.deleteGroupActivitySchedule(requester, 1L)
        }
    }

    @Test
    fun `deleteBigSeminarSchedule은 총회 일정을 TRASH로 전환하고 빅세미나를 삭제한다`() {
        val existing = scheduleOf(category = ScheduleCategory.BIG_SEMINAR)
        given(scheduleRepository.findActiveById(1L)).willReturn(existing)
        given(scheduleRepository.save(any<Schedule>())).willAnswer { it.arguments[0] as Schedule }
        val requester = Requester(userId = 1L, role = UserRole.MANAGER)

        scheduleService.deleteBigSeminarSchedule(requester, 1L)

        val captor = org.mockito.kotlin.argumentCaptor<Schedule>()
        verify(scheduleRepository).save(captor.capture())
        assertEquals(ScheduleState.TRASH, captor.firstValue.state)
        verify(bigSeminarRepository).deleteByScheduleId(1L)
    }

    @Test
    fun `listActiveSchedules는 checkCode가 있고 진행 중인 일정만 반환한다`() {
        val now = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
        val activeSchedule =
            Schedule(
                id = 1L,
                category = ScheduleCategory.CLUB,
                title = "출석 진행 중",
                author = 10L,
                state = ScheduleState.PUBLIC,
                scheduledAt = now.minusHours(1),
                endAt = now.plusHours(1),
                checkCode = "1234",
            )
        val endedSchedule =
            activeSchedule.copy(
                id = 2L,
                title = "종료된 일정",
                scheduledAt = now.minusHours(3),
                endAt = now.minusHours(1),
                checkCode = "1234",
            )
        val futureSchedule =
            activeSchedule.copy(
                id = 3L,
                title = "시작 전 일정",
                scheduledAt = now.plusHours(1),
                endAt = now.plusHours(2),
                checkCode = "1234",
            )
        val noCheckCodeSchedule =
            activeSchedule.copy(
                id = 4L,
                title = "코드 없는 일정",
                checkCode = null,
            )
        given(scheduleRepository.findAttendanceActive(any(), any()))
            .willReturn(listOf(activeSchedule, endedSchedule, futureSchedule, noCheckCodeSchedule))

        val result = scheduleService.listActiveSchedules()

        assertEquals(listOf(activeSchedule), result)
        verify(scheduleRepository).findAttendanceActive(any(), any())
    }

    @Test
    fun `listActiveSchedules는 출석 진행 중인 일정이 없으면 빈 목록을 반환한다`() {
        given(scheduleRepository.findAttendanceActive(any(), any())).willReturn(emptyList())

        val result = scheduleService.listActiveSchedules()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `listSchedules는 limit 개수만큼 일정을 반환한다`() {
        // given
        val from = LocalDateTime.of(2024, 1, 1, 0, 0)
        val limit = 2
        val schedules =
            listOf(
                Schedule(
                    id = 1L,
                    category = ScheduleCategory.CLUB,
                    title = "일정1",
                    author = 1L,
                    state = ScheduleState.PUBLIC,
                    scheduledAt = LocalDateTime.of(2024, 1, 2, 14, 0),
                    endAt = LocalDateTime.of(2024, 1, 2, 16, 0),
                ),
                Schedule(
                    id = 2L,
                    category = ScheduleCategory.ACADEMIC,
                    title = "일정2",
                    author = 1L,
                    state = ScheduleState.PUBLIC,
                    scheduledAt = LocalDateTime.of(2024, 1, 3, 14, 0),
                    endAt = LocalDateTime.of(2024, 1, 3, 16, 0),
                ),
            )
        given(scheduleRepository.findUpcoming(any(), any())).willReturn(schedules)

        // when
        val result = scheduleService.listSchedules(from, limit)

        // then
        assertEquals(2, result.size)
    }

    @Test
    fun `listScheduleAttendances는 일정의 출석자 목록을 반환한다`() {
        val scheduleId = 100L
        val schedule =
            Schedule(
                id = scheduleId,
                category = ScheduleCategory.CLUB,
                title = "테스트 일정",
                author = 1L,
                state = ScheduleState.PUBLIC,
                scheduledAt = LocalDateTime.of(2026, 5, 15, 10, 0),
                endAt = LocalDateTime.of(2026, 5, 15, 18, 0),
            )
        val createdAt = LocalDateTime.of(2026, 5, 15, 10, 0)
        val applies =
            listOf(
                ScheduleMemberApply(
                    memberId = 1L,
                    scheduleId = scheduleId,
                    attendedAt = LocalDateTime.of(2026, 5, 15, 14, 0),
                    createdAt = createdAt,
                ),
                ScheduleMemberApply(
                    memberId = 2L,
                    scheduleId = scheduleId,
                    attendedAt = null,
                    createdAt = createdAt.plusHours(1),
                ),
            )

        given(scheduleRepository.findActiveById(scheduleId)).willReturn(schedule)
        given(scheduleMemberApplyRepository.findByScheduleIdOrderByCreatedAtAsc(scheduleId)).willReturn(applies)

        val result = scheduleService.listScheduleAttendances(scheduleId)

        assertEquals(2, result.count)
        assertEquals(2, result.attendances.size)
        assertEquals(1L, result.attendances[0].userId)
        assertTrue(result.attendances[0].isChecked)
        assertEquals(createdAt, result.attendances[0].createdAt)
        assertEquals(2L, result.attendances[1].userId)
        assertFalse(result.attendances[1].isChecked)
        verify(scheduleRepository).findActiveById(scheduleId)
        verify(scheduleMemberApplyRepository).findByScheduleIdOrderByCreatedAtAsc(scheduleId)
    }

    @Test
    fun `listScheduleAttendances는 출석자가 없으면 빈 목록을 반환한다`() {
        val scheduleId = 100L
        val schedule =
            Schedule(
                id = scheduleId,
                category = ScheduleCategory.CLUB,
                title = "테스트 일정",
                author = 1L,
                state = ScheduleState.PUBLIC,
                scheduledAt = LocalDateTime.of(2026, 5, 15, 10, 0),
                endAt = LocalDateTime.of(2026, 5, 15, 18, 0),
            )

        given(scheduleRepository.findActiveById(scheduleId)).willReturn(schedule)
        given(scheduleMemberApplyRepository.findByScheduleIdOrderByCreatedAtAsc(scheduleId))
            .willReturn(emptyList())

        val result = scheduleService.listScheduleAttendances(scheduleId)

        assertEquals(0, result.count)
        assertTrue(result.attendances.isEmpty())
        verify(scheduleRepository).findActiveById(scheduleId)
        verify(scheduleMemberApplyRepository).findByScheduleIdOrderByCreatedAtAsc(scheduleId)
    }

    @Test
    fun `listScheduleAttendances는 존재하지 않는 일정이면 NotFoundException을 발생시킨다`() {
        val scheduleId = 999L

        given(scheduleRepository.findActiveById(scheduleId)).willReturn(null)

        assertThrows<NotFoundException> {
            scheduleService.listScheduleAttendances(scheduleId)
        }

        verify(scheduleRepository).findActiveById(scheduleId)
        verify(scheduleMemberApplyRepository, never()).findByScheduleIdOrderByCreatedAtAsc(any())
    }

    @Test
    fun `checkScheduleAttendance는 유효한 코드로 출석 처리하고 ExPoint를 적립한다`() {
        val requester = Requester(userId = 10L, role = UserRole.USER)
        val schedule = attendanceSchedule(expoint = 15, checkCode = "1234")
        given(scheduleRepository.findActiveById(schedule.id)).willReturn(schedule)
        given(scheduleMemberApplyRepository.existsByMemberIdAndScheduleId(requester.userId, schedule.id)).willReturn(false)
        given(scheduleMemberApplyRepository.save(any<ScheduleMemberApply>()))
            .willAnswer { it.arguments[0] as ScheduleMemberApply }

        val result =
            scheduleService.checkScheduleAttendance(
                requester = requester,
                scheduleId = schedule.id,
                code = "1234",
            )

        assertEquals(schedule.id, result.scheduleId)
        assertEquals(requester.userId, result.userId)
        assertEquals(15, result.expointGranted)
        verify(scheduleMemberApplyRepository).save(any<ScheduleMemberApply>())
        verify(pointService).givePoint(requester.userId, 15, "${schedule.title} 출석")
    }

    @Test
    fun `checkScheduleAttendance는 같은 일정에 두 번 출석체크하면 ConflictException을 던진다`() {
        val requester = Requester(userId = 10L, role = UserRole.USER)
        val schedule = attendanceSchedule()
        given(scheduleRepository.findActiveById(schedule.id)).willReturn(schedule)
        given(scheduleMemberApplyRepository.existsByMemberIdAndScheduleId(requester.userId, schedule.id)).willReturn(true)

        assertThrows<ConflictException> {
            scheduleService.checkScheduleAttendance(
                requester = requester,
                scheduleId = schedule.id,
                code = "1234",
            )
        }

        verify(scheduleMemberApplyRepository, never()).save(any<ScheduleMemberApply>())
        verify(pointService, never()).givePoint(any(), any(), any())
    }

    @Test
    fun `checkScheduleAttendance는 코드가 일치하지 않으면 UnauthorizedException을 던진다`() {
        val requester = Requester(userId = 10L, role = UserRole.USER)
        val schedule = attendanceSchedule(checkCode = "1234")
        given(scheduleRepository.findActiveById(schedule.id)).willReturn(schedule)
        given(scheduleMemberApplyRepository.existsByMemberIdAndScheduleId(requester.userId, schedule.id)).willReturn(false)

        assertThrows<UnauthorizedException> {
            scheduleService.checkScheduleAttendance(
                requester = requester,
                scheduleId = schedule.id,
                code = "9999",
            )
        }

        verify(scheduleMemberApplyRepository, never()).save(any<ScheduleMemberApply>())
        verify(pointService, never()).givePoint(any(), any(), any())
    }

    @Test
    fun `checkScheduleAttendance는 출석체크 시간 밖이면 BadRequestException을 던진다`() {
        val requester = Requester(userId = 10L, role = UserRole.USER)
        val now = LocalDateTime.now()
        val beforeSchedule =
            attendanceSchedule(
                scheduledAt = now.plusHours(1),
                endAt = now.plusHours(2),
            )
        val afterSchedule =
            attendanceSchedule(
                scheduledAt = now.minusHours(2),
                endAt = now.minusHours(1),
            )
        given(scheduleRepository.findActiveById(beforeSchedule.id)).willReturn(beforeSchedule, afterSchedule)
        given(scheduleMemberApplyRepository.existsByMemberIdAndScheduleId(requester.userId, beforeSchedule.id)).willReturn(false)

        assertThrows<BadRequestException> {
            scheduleService.checkScheduleAttendance(
                requester = requester,
                scheduleId = beforeSchedule.id,
                code = "1234",
            )
        }
        assertThrows<BadRequestException> {
            scheduleService.checkScheduleAttendance(
                requester = requester,
                scheduleId = beforeSchedule.id,
                code = "1234",
            )
        }

        verify(scheduleMemberApplyRepository, never()).save(any<ScheduleMemberApply>())
        verify(pointService, never()).givePoint(any(), any(), any())
    }

    @Test
    fun `checkScheduleAttendance는 checkCode가 null이면 BadRequestException을 던진다`() {
        val requester = Requester(userId = 10L, role = UserRole.USER)
        val schedule = attendanceSchedule(checkCode = null)
        given(scheduleRepository.findActiveById(schedule.id)).willReturn(schedule)
        given(scheduleMemberApplyRepository.existsByMemberIdAndScheduleId(requester.userId, schedule.id)).willReturn(false)

        assertThrows<BadRequestException> {
            scheduleService.checkScheduleAttendance(
                requester = requester,
                scheduleId = schedule.id,
                code = "1234",
            )
        }

        verify(scheduleMemberApplyRepository, never()).save(any<ScheduleMemberApply>())
        verify(pointService, never()).givePoint(any(), any(), any())
    }

    @Test
    fun `checkScheduleAttendance는 expoint가 0이어도 출석 처리하고 포인트는 적립하지 않는다`() {
        val requester = Requester(userId = 10L, role = UserRole.MANAGER)
        val schedule = attendanceSchedule(expoint = 0, checkCode = "1234")
        given(scheduleRepository.findActiveById(schedule.id)).willReturn(schedule)
        given(scheduleMemberApplyRepository.existsByMemberIdAndScheduleId(requester.userId, schedule.id)).willReturn(false)
        given(scheduleMemberApplyRepository.save(any<ScheduleMemberApply>()))
            .willAnswer { it.arguments[0] as ScheduleMemberApply }

        val result =
            scheduleService.checkScheduleAttendance(
                requester = requester,
                scheduleId = schedule.id,
                code = "1234",
            )

        assertEquals(schedule.id, result.scheduleId)
        assertEquals(requester.userId, result.userId)
        assertEquals(0, result.expointGranted)
        verify(scheduleMemberApplyRepository).save(any<ScheduleMemberApply>())
        verify(pointService, never()).givePoint(any(), any(), any())
    }

    @Test
    fun `checkScheduleAttendance는 존재하지 않는 일정이면 NotFoundException을 던진다`() {
        val requester = Requester(userId = 10L, role = UserRole.USER)
        val scheduleId = 999L
        given(scheduleRepository.findActiveById(scheduleId)).willReturn(null)

        assertThrows<NotFoundException> {
            scheduleService.checkScheduleAttendance(
                requester = requester,
                scheduleId = scheduleId,
                code = "1234",
            )
        }

        verify(scheduleMemberApplyRepository, never()).existsByMemberIdAndScheduleId(any(), any())
        verify(scheduleMemberApplyRepository, never()).save(any<ScheduleMemberApply>())
        verify(pointService, never()).givePoint(any(), any(), any())
    }

    @Test
    fun `addScheduleAttendances는 운영진이 여러 사용자를 출석 처리하고 ExPoint를 적립한다`() {
        val requester = Requester(userId = 1L, role = UserRole.MANAGER)
        val schedule = attendanceSchedule(expoint = 10)
        val userIds = listOf(10L, 20L, 30L)
        given(scheduleRepository.findActiveById(schedule.id)).willReturn(schedule)
        userIds.forEach { userId ->
            given(memberRepository.existsById(userId)).willReturn(true)
        }
        given(scheduleMemberApplyRepository.findByMemberIdInAndScheduleId(userIds, schedule.id)).willReturn(emptyList())

        scheduleService.addScheduleAttendances(
            requester = requester,
            scheduleId = schedule.id,
            userIds = userIds,
        )

        verify(scheduleMemberApplyRepository).saveAll(any<Iterable<ScheduleMemberApply>>())
        userIds.forEach { userId ->
            verify(pointService).givePoint(userId, schedule.expoint, "${schedule.title} 출석 (관리자 추가)")
        }
    }

    @Test
    fun `addScheduleAttendances는 userIds가 빈 리스트면 BadRequestException을 던진다`() {
        val requester = Requester(userId = 1L, role = UserRole.MANAGER)

        assertThrows<BadRequestException> {
            scheduleService.addScheduleAttendances(
                requester = requester,
                scheduleId = 100L,
                userIds = emptyList(),
            )
        }

        verify(scheduleRepository, never()).findActiveById(any())
        verify(scheduleMemberApplyRepository, never()).saveAll(any<Iterable<ScheduleMemberApply>>())
        verify(pointService, never()).givePoint(any(), any(), any())
    }

    @Test
    fun `addScheduleAttendances는 중복된 userId가 있으면 BadRequestException을 던진다`() {
        val requester = Requester(userId = 1L, role = UserRole.MANAGER)

        assertThrows<BadRequestException> {
            scheduleService.addScheduleAttendances(
                requester = requester,
                scheduleId = 100L,
                userIds = listOf(10L, 10L),
            )
        }

        verify(scheduleRepository, never()).findActiveById(any())
        verify(scheduleMemberApplyRepository, never()).saveAll(any<Iterable<ScheduleMemberApply>>())
        verify(pointService, never()).givePoint(any(), any(), any())
    }

    @Test
    fun `addScheduleAttendances는 이미 출석 처리된 사용자가 포함되면 ConflictException을 던진다`() {
        val requester = Requester(userId = 1L, role = UserRole.MANAGER)
        val schedule = attendanceSchedule()
        val userIds = listOf(10L, 20L)
        val existingAttendance =
            ScheduleMemberApply(
                memberId = 20L,
                scheduleId = schedule.id,
                attendedAt = LocalDateTime.now().minusMinutes(10),
            )
        given(scheduleRepository.findActiveById(schedule.id)).willReturn(schedule)
        userIds.forEach { userId ->
            given(memberRepository.existsById(userId)).willReturn(true)
        }
        given(scheduleMemberApplyRepository.findByMemberIdInAndScheduleId(userIds, schedule.id))
            .willReturn(listOf(existingAttendance))

        assertThrows<ConflictException> {
            scheduleService.addScheduleAttendances(
                requester = requester,
                scheduleId = schedule.id,
                userIds = userIds,
            )
        }

        verify(scheduleMemberApplyRepository, never()).saveAll(any<Iterable<ScheduleMemberApply>>())
        verify(pointService, never()).givePoint(any(), any(), any())
    }

    @Test
    fun `addScheduleAttendances는 존재하지 않는 일정이면 NotFoundException을 던진다`() {
        val requester = Requester(userId = 1L, role = UserRole.MANAGER)
        val scheduleId = 999L
        given(scheduleRepository.findActiveById(scheduleId)).willReturn(null)

        assertThrows<NotFoundException> {
            scheduleService.addScheduleAttendances(
                requester = requester,
                scheduleId = scheduleId,
                userIds = listOf(10L),
            )
        }

        verify(memberRepository, never()).existsById(any())
        verify(scheduleMemberApplyRepository, never()).saveAll(any<Iterable<ScheduleMemberApply>>())
        verify(pointService, never()).givePoint(any(), any(), any())
    }

    @Test
    fun `addScheduleAttendances는 존재하지 않는 사용자가 포함되면 NotFoundException을 던진다`() {
        val requester = Requester(userId = 1L, role = UserRole.MANAGER)
        val schedule = attendanceSchedule()
        given(scheduleRepository.findActiveById(schedule.id)).willReturn(schedule)
        given(memberRepository.existsById(10L)).willReturn(true)
        given(memberRepository.existsById(999L)).willReturn(false)

        assertThrows<NotFoundException> {
            scheduleService.addScheduleAttendances(
                requester = requester,
                scheduleId = schedule.id,
                userIds = listOf(10L, 999L),
            )
        }

        verify(scheduleMemberApplyRepository, never()).findByMemberIdInAndScheduleId(any(), any())
        verify(scheduleMemberApplyRepository, never()).saveAll(any<Iterable<ScheduleMemberApply>>())
        verify(pointService, never()).givePoint(any(), any(), any())
    }

    @Test
    fun `addScheduleAttendances는 MANAGER가 아니면 ForbiddenException을 던진다`() {
        val requester = Requester(userId = 1L, role = UserRole.USER)

        assertThrows<ForbiddenException> {
            scheduleService.addScheduleAttendances(
                requester = requester,
                scheduleId = 100L,
                userIds = listOf(10L),
            )
        }

        verify(scheduleRepository, never()).findActiveById(any())
        verify(scheduleMemberApplyRepository, never()).saveAll(any<Iterable<ScheduleMemberApply>>())
        verify(pointService, never()).givePoint(any(), any(), any())
    }

    @Test
    fun `removeScheduleAttendance는 출석 기록을 삭제하고 ExPoint를 회수한다`() {
        val requester = Requester(userId = 1L, role = UserRole.MANAGER)
        val userId = 10L
        val schedule = attendanceSchedule(expoint = 15)
        val attendance =
            ScheduleMemberApply(
                memberId = userId,
                scheduleId = schedule.id,
                attendedAt = LocalDateTime.now().minusMinutes(10),
            )
        given(scheduleRepository.findActiveById(schedule.id)).willReturn(schedule)
        given(scheduleMemberApplyRepository.findByMemberIdAndScheduleId(userId, schedule.id)).willReturn(attendance)

        scheduleService.removeScheduleAttendance(
            requester = requester,
            scheduleId = schedule.id,
            userId = userId,
        )

        verify(scheduleMemberApplyRepository).delete(attendance)
        verify(pointService).givePoint(userId, -15, "${schedule.title} 출석 취소")
    }

    @Test
    fun `removeScheduleAttendance는 ExPoint 회수를 음수 적립으로 기록한다`() {
        val requester = Requester(userId = 1L, role = UserRole.MANAGER)
        val userId = 10L
        val schedule = attendanceSchedule(expoint = 7)
        val attendance =
            ScheduleMemberApply(
                memberId = userId,
                scheduleId = schedule.id,
                attendedAt = LocalDateTime.now().minusMinutes(10),
            )
        given(scheduleRepository.findActiveById(schedule.id)).willReturn(schedule)
        given(scheduleMemberApplyRepository.findByMemberIdAndScheduleId(userId, schedule.id)).willReturn(attendance)

        scheduleService.removeScheduleAttendance(
            requester = requester,
            scheduleId = schedule.id,
            userId = userId,
        )

        verify(pointService).givePoint(userId, -schedule.expoint, "${schedule.title} 출석 취소")
    }

    @Test
    fun `removeScheduleAttendance는 출석한 적이 없으면 NotFoundException을 던진다`() {
        val requester = Requester(userId = 1L, role = UserRole.MANAGER)
        val userId = 10L
        val schedule = attendanceSchedule()
        given(scheduleRepository.findActiveById(schedule.id)).willReturn(schedule)
        given(scheduleMemberApplyRepository.findByMemberIdAndScheduleId(userId, schedule.id)).willReturn(null)

        assertThrows<NotFoundException> {
            scheduleService.removeScheduleAttendance(
                requester = requester,
                scheduleId = schedule.id,
                userId = userId,
            )
        }

        verify(scheduleMemberApplyRepository, never()).delete(any<ScheduleMemberApply>())
        verify(pointService, never()).givePoint(any(), any(), any())
    }

    @Test
    fun `removeScheduleAttendance는 MANAGER가 아니면 ForbiddenException을 던진다`() {
        val requester = Requester(userId = 1L, role = UserRole.USER)

        assertThrows<ForbiddenException> {
            scheduleService.removeScheduleAttendance(
                requester = requester,
                scheduleId = 100L,
                userId = 10L,
            )
        }

        verify(scheduleRepository, never()).findActiveById(any())
        verify(scheduleMemberApplyRepository, never()).delete(any<ScheduleMemberApply>())
        verify(pointService, never()).givePoint(any(), any(), any())
    }

    @Test
    fun `removeScheduleAttendance는 일정이 없으면 NotFoundException을 던진다`() {
        val requester = Requester(userId = 1L, role = UserRole.MANAGER)
        val scheduleId = 999L
        given(scheduleRepository.findActiveById(scheduleId)).willReturn(null)

        assertThrows<NotFoundException> {
            scheduleService.removeScheduleAttendance(
                requester = requester,
                scheduleId = scheduleId,
                userId = 10L,
            )
        }

        verify(scheduleMemberApplyRepository, never()).findByMemberIdAndScheduleId(any(), any())
        verify(scheduleMemberApplyRepository, never()).delete(any<ScheduleMemberApply>())
        verify(pointService, never()).givePoint(any(), any(), any())
    }

    @Test
    fun `removeScheduleAttendance는 같은 출석 기록을 두 번 삭제하면 두 번째는 NotFoundException을 던진다`() {
        val requester = Requester(userId = 1L, role = UserRole.MANAGER)
        val userId = 10L
        val schedule = attendanceSchedule()
        val attendance =
            ScheduleMemberApply(
                memberId = userId,
                scheduleId = schedule.id,
                attendedAt = LocalDateTime.now().minusMinutes(10),
            )
        given(scheduleRepository.findActiveById(schedule.id)).willReturn(schedule)
        given(scheduleMemberApplyRepository.findByMemberIdAndScheduleId(userId, schedule.id)).willReturn(attendance, null)

        scheduleService.removeScheduleAttendance(
            requester = requester,
            scheduleId = schedule.id,
            userId = userId,
        )
        assertThrows<NotFoundException> {
            scheduleService.removeScheduleAttendance(
                requester = requester,
                scheduleId = schedule.id,
                userId = userId,
            )
        }

        verify(scheduleMemberApplyRepository).delete(attendance)
        verify(pointService).givePoint(userId, -schedule.expoint, "${schedule.title} 출석 취소")
    }

    private fun scheduleOf(
        id: Long = 1L,
        category: ScheduleCategory = ScheduleCategory.CLUB,
        author: Long = 10L,
        checkCode: String? = null,
    ): Schedule {
        return Schedule(
            id = id,
            category = category,
            title = "일정",
            author = author,
            state = ScheduleState.PUBLIC,
            scheduledAt = LocalDateTime.of(2026, 5, 18, 14, 0),
            endAt = LocalDateTime.of(2026, 5, 18, 16, 0),
            checkCode = checkCode,
        )
    }

    private fun attendanceSchedule(
        id: Long = 100L,
        expoint: Int = 10,
        checkCode: String? = "1234",
        scheduledAt: LocalDateTime = LocalDateTime.now(ZoneId.of("Asia/Seoul")).minusHours(1),
        endAt: LocalDateTime = LocalDateTime.now(ZoneId.of("Asia/Seoul")).plusHours(1),
    ): Schedule {
        return Schedule(
            id = id,
            category = ScheduleCategory.CLUB,
            title = "출석 테스트 일정",
            author = 1L,
            state = ScheduleState.PUBLIC,
            scheduledAt = scheduledAt,
            endAt = endAt,
            expoint = expoint,
            checkCode = checkCode,
        )
    }
}
