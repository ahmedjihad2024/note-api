package com.example.studing.common.exception

import com.example.studing.common.dto.ApiResponse
import jakarta.validation.ConstraintViolationException
import org.springframework.dao.DuplicateKeyException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.AuthenticationException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(ApiException::class)
    fun handleApi(ex: ApiException): ResponseEntity<ApiResponse<Nothing>> =
        respond(ex.errorCode, ex.message)

    @ExceptionHandler(BadCredentialsException::class)
    fun handleBadCreds(ex: BadCredentialsException): ResponseEntity<ApiResponse<Nothing>> =
        respond(ErrorCode.INVALID_CREDENTIALS, ex.message)

    @ExceptionHandler(AuthenticationException::class)
    fun handleAuth(ex: AuthenticationException): ResponseEntity<ApiResponse<Nothing>> =
        respond(ErrorCode.UNAUTHORIZED, ex.message)

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(ex: AccessDeniedException): ResponseEntity<ApiResponse<Nothing>> =
        respond(ErrorCode.FORBIDDEN, ex.message)

    @ExceptionHandler(DuplicateKeyException::class)
    fun handleDuplicate(ex: DuplicateKeyException): ResponseEntity<ApiResponse<Nothing>> =
        respond(ErrorCode.CONFLICT, ex.message)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleBodyValidation(ex: MethodArgumentNotValidException): ResponseEntity<ApiResponse<Nothing>> {
        val msg = ex.bindingResult.fieldErrors
            .joinToString { "${it.field}: ${it.defaultMessage ?: "invalid"}" }
        return respond(ErrorCode.VALIDATION_ERROR, msg)
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleParamValidation(ex: ConstraintViolationException): ResponseEntity<ApiResponse<Nothing>> {
        val msg = ex.constraintViolations.joinToString { "${it.propertyPath}: ${it.message}" }
        return respond(ErrorCode.VALIDATION_ERROR, msg)
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArg(ex: IllegalArgumentException): ResponseEntity<ApiResponse<Nothing>> =
        respond(ErrorCode.BAD_REQUEST, ex.message)

    @ExceptionHandler(Exception::class)
    fun handleAny(ex: Exception): ResponseEntity<ApiResponse<Nothing>> =
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.fail(ErrorCode.INTERNAL_ERROR.code, ex.message ?: "Unexpected error."))

    private fun respond(code: ErrorCode, message: String?): ResponseEntity<ApiResponse<Nothing>> =
        ResponseEntity.status(code.status)
            .body(ApiResponse.fail(code.code, message ?: code.code))
}
