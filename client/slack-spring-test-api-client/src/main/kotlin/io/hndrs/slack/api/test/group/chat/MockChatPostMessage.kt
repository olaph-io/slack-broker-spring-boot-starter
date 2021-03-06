package io.hndrs.slack.api.test.group.chat

import io.hndrs.slack.api.contract.jackson.group.chat.ErrorPostMessageResponse
import io.hndrs.slack.api.contract.jackson.group.chat.PostMessageRequest
import io.hndrs.slack.api.contract.jackson.group.chat.SuccessfulPostMessageResponse
import io.hndrs.slack.api.group.ApiCallResult
import io.hndrs.slack.api.group.chat.ChatMethodGroup
import io.hndrs.slack.api.group.chat.ChatPostMessageMethod
import io.hndrs.slack.api.test.MockMethod

/**
 * Testable implementation of [ChatMethodGroup.postMessage]
 */
open class MockChatPostMessage : io.hndrs.slack.api.group.chat.ChatPostMessageMethod(),
    MockMethod<SuccessfulPostMessageResponse, ErrorPostMessageResponse, PostMessageRequest> {

    override var successResponse: SuccessfulPostMessageResponse? = null
    override var failureResponse: ErrorPostMessageResponse? = null

    override fun request(): io.hndrs.slack.api.group.ApiCallResult<SuccessfulPostMessageResponse, ErrorPostMessageResponse> {

        this.successResponse?.let { this.onSuccess?.invoke(it) }
        this.failureResponse?.let { this.onFailure?.invoke(it) }

        return io.hndrs.slack.api.group.ApiCallResult(this.successResponse, this.failureResponse)
    }

    override fun params(): PostMessageRequest = params
}
