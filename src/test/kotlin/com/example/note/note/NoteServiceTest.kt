package com.example.note.note

import com.example.note.common.exception.ApiException
import com.example.note.note.dto.NoteRequest
import com.example.note.note.dto.UpdateNoteRequest
import com.example.note.support.Fixtures
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.bson.types.ObjectId
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import java.util.Optional

class NoteServiceTest {

    private val noteRepository = mockk<NoteRepository>()
    private val service = NoteService(noteRepository)

    private val currentUser = ObjectId()

    @BeforeEach
    fun authenticate() {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(currentUser.toHexString(), null, emptyList())
    }

    @AfterEach
    fun clearAuth() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `create saves a note owned by the current user`() {
        val saved = slot<Note>()
        every { noteRepository.save(capture(saved)) } answers { saved.captured }

        service.create(NoteRequest(title = "T", content = "C", color = null))

        assertThat(saved.captured.ownerId).isEqualTo(currentUser)
    }

    @Test
    fun `getNotes fetches the current user's page`() {
        val pageable = PageRequest.of(0, 10)
        val page = PageImpl(listOf(Fixtures.note(ownerId = currentUser)))
        every { noteRepository.findByOwnerId(currentUser, pageable) } returns page

        assertThat(service.getNotes(pageable)).isSameAs(page)
    }

    @Test
    fun `getOwned returns the note when the caller owns it`() {
        val note = Fixtures.note(ownerId = currentUser)
        every { noteRepository.findById(note.id) } returns Optional.of(note)

        assertThat(service.getOwned(note.id)).isSameAs(note)
    }

    @Test
    fun `getOwned throws NotFound when the note is missing`() {
        val id = ObjectId()
        every { noteRepository.findById(id) } returns Optional.empty()

        assertThatThrownBy { service.getOwned(id) }
            .isInstanceOf(ApiException.NotFound::class.java)
    }

    @Test
    fun `getOwned throws Forbidden when the note belongs to someone else`() {
        val note = Fixtures.note(ownerId = ObjectId())
        every { noteRepository.findById(note.id) } returns Optional.of(note)

        assertThatThrownBy { service.getOwned(note.id) }
            .isInstanceOf(ApiException.Forbidden::class.java)
    }

    @Test
    fun `update overlays only the provided fields and stamps updateAt`() {
        val existing = Fixtures.note(ownerId = currentUser, title = "Old", content = "Body", color = 1L)
        every { noteRepository.findById(existing.id) } returns Optional.of(existing)
        val saved = slot<Note>()
        every { noteRepository.save(capture(saved)) } answers { saved.captured }

        service.update(existing.id, UpdateNoteRequest(title = "New", content = null, color = null))

        assertThat(saved.captured.title).isEqualTo("New")
        assertThat(saved.captured.content).isEqualTo("Body")
        assertThat(saved.captured.color).isEqualTo(1L)
        assertThat(saved.captured.updateAt).isNotNull()
    }

    @Test
    fun `delete removes a note the caller owns`() {
        val note = Fixtures.note(ownerId = currentUser)
        every { noteRepository.findById(note.id) } returns Optional.of(note)
        every { noteRepository.delete(note) } returns Unit

        service.delete(note.id)

        verify { noteRepository.delete(note) }
    }

    @Test
    fun `operations fail with Unauthorized when there is no authentication`() {
        SecurityContextHolder.clearContext()

        assertThatThrownBy { service.getNotes(PageRequest.of(0, 10)) }
            .isInstanceOf(ApiException.Unauthorized::class.java)
    }
}
