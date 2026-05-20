package com.example.studing.common.exception

sealed class ApiException(
    val errorCode: ErrorCode,
    message: String,
) : RuntimeException(message) {

    class NotFound(resource: String) : ApiException(ErrorCode.NOT_FOUND, "$resource not found.")
    class Unauthorized(reason: String = "Unauthorized.") : ApiException(ErrorCode.UNAUTHORIZED, reason)
    class Forbidden(reason: String = "Forbidden.") : ApiException(ErrorCode.FORBIDDEN, reason)
    class Conflict(reason: String) : ApiException(ErrorCode.CONFLICT, reason)
    class BadRequest(reason: String) : ApiException(ErrorCode.BAD_REQUEST, reason)
}
