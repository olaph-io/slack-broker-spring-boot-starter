package io.hndrs.slack.api.group.team

/**
 * Convenience class to handle the team operations
 *
 *  [Slack Api Documentation](https://api.slack.com/methods)
 */
interface TeamMethodGroup {

    /**
     * 	Retrieve a team's profile.
     */
    fun getProfile(authToken: String): io.hndrs.slack.api.group.team.TeamGetProfileMethod
}
