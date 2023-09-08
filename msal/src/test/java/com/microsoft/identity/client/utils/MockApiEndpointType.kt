package com.microsoft.identity.client.utils

enum class MockApiEndpointType(val stringValue: String) {
    SignInInitiate("SignInInitiate"),
    SignInChallenge("SignInChallenge"),
    SignInToken("SignInToken"),
    SignUpStart("SignUpStart"),
    SignUpChallenge("SignUpChallenge"),
    SignUpContinue("SignUpContinue"),
    SSPRStart("SSPRStart"),
    SSPRChallenge("SSPRChallenge"),
    SSPRContinue("SSPRContinue"),
    SSPRSubmit("SSPRSubmit"),
    SSPRPoll("SSPRPoll")
}
