package com.example.note.admin

import com.example.note.common.exception.ApiException
import com.example.note.support.Fixtures
import com.example.note.user.entities.User
import com.example.note.user.enums.Role
import com.example.note.user.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.util.Optional

class AdminServiceTest {

    private val userRepository = mockk<UserRepository>()
    private val service = AdminService(userRepository)

    @Test
    fun `listUsers delegates straight to the repository`() {
        val pageable = PageRequest.of(0, 10)
        val page: Page<User> = PageImpl(listOf(Fixtures.user()))
        every { userRepository.findAll(pageable) } returns page

        assertThat(service.listUsers(pageable)).isSameAs(page)
    }

    @Test
    fun `updateRoles rejects an admin trying to modify their own roles`() {
        val selfId = ObjectId()

        assertThatThrownBy {
            service.updateRoles(selfId, selfId.toHexString(), setOf("USER"))
        }
            .isInstanceOf(ApiException.BadRequest::class.java)
            .hasMessage("error.admin.cannot_modify_self")

        verify(exactly = 0) { userRepository.findById(any()) }
    }

    @Test
    fun `updateRoles rejects an unknown role name`() {
        val target = ObjectId()

        assertThatThrownBy {
            service.updateRoles(target, ObjectId().toHexString(), setOf("SUPERUSER"))
        }
            .isInstanceOf(ApiException.BadRequest::class.java)
            .hasMessage("error.admin.invalid_role")
    }

    @Test
    fun `updateRoles fails when the target user does not exist`() {
        val target = ObjectId()
        every { userRepository.findById(target) } returns Optional.empty()

        assertThatThrownBy {
            service.updateRoles(target, ObjectId().toHexString(), setOf("ADMIN"))
        }.isInstanceOf(ApiException.NotFound::class.java)
    }

    @Test
    fun `updateRoles persists the parsed roles on the target user`() {
        val target = Fixtures.user(roles = setOf(Role.USER))
        every { userRepository.findById(target.id) } returns Optional.of(target)
        val saved = slot<User>()
        every { userRepository.save(capture(saved)) } answers { saved.captured }

        val result = service.updateRoles(target.id, ObjectId().toHexString(), setOf("USER", "ADMIN"))

        assertThat(result.roles).containsExactlyInAnyOrder(Role.USER, Role.ADMIN)
        assertThat(saved.captured.id).isEqualTo(target.id)
    }
}
