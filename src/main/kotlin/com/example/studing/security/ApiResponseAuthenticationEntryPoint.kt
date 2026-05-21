package com.example.studing.security

import com.example.studing.common.dto.ApiResponse
import com.example.studing.common.exception.ErrorCode
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.http.MediaType
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class ApiResponseAuthenticationEntryPoint(
    private val objectMapper: ObjectMapper,
    private val messageSource: MessageSource,
) : AuthenticationEntryPoint {

    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException,
    ) {
        response.status = HttpServletResponse.SC_UNAUTHORIZED
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = Charsets.UTF_8.name()
        val message = messageSource.getMessage(
            "error.unauthorized.default",
            null,
            "Authentication required.",
            LocaleContextHolder.getLocale(),
        ) ?: "Authentication required."
        val body = ApiResponse.fail(ErrorCode.UNAUTHORIZED.code, message)
        objectMapper.writeValue(response.outputStream, body)
    }
}
