package com.kreait.slack.broker.extensions

import com.kreait.slack.broker.store.team.Team

fun Team.Companion.sample(): Team = Team("TestTeamId", "TestTeamName", Team.Bot.sample())

fun Team.Bot.Companion.sample(): Team.Bot = Team.Bot("UserId", "acccessToken")
