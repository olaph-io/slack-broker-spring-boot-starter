package com.kreait.slack.broker.autoconfiguration

import com.kreait.slack.api.SlackClient
import com.kreait.slack.api.contract.jackson.BlockActions
import com.kreait.slack.api.contract.jackson.InteractiveMessage
import com.kreait.slack.api.spring.DefaultSlackClient
import com.kreait.slack.broker.autoconfiguration.credentials.CredentialsProvider
import com.kreait.slack.broker.autoconfiguration.credentials.DefaultCredentialsProviderChain
import com.kreait.slack.broker.broker.CommandBroker
import com.kreait.slack.broker.broker.EventBroker
import com.kreait.slack.broker.broker.InstallationBroker
import com.kreait.slack.broker.broker.InteractiveComponentBroker
import com.kreait.slack.broker.configuration.EventArgumentResolver
import com.kreait.slack.broker.configuration.InteractiveResponseArgumentResolver
import com.kreait.slack.broker.configuration.SlackCommandArgumentResolver
import com.kreait.slack.broker.exception.SlackExceptionHandler
import com.kreait.slack.broker.metrics.CommandMetrics
import com.kreait.slack.broker.metrics.CommandMetricsCollector
import com.kreait.slack.broker.metrics.EventMetrics
import com.kreait.slack.broker.metrics.EventMetricsCollector
import com.kreait.slack.broker.metrics.InstallationMetrics
import com.kreait.slack.broker.metrics.InstallationMetricsCollector
import com.kreait.slack.broker.metrics.InteractiveComponentMetrics
import com.kreait.slack.broker.metrics.InteractiveComponentMetricsCollector
import com.kreait.slack.broker.receiver.CommandNotFoundReceiver
import com.kreait.slack.broker.receiver.EventReceiver
import com.kreait.slack.broker.receiver.InstallationReceiver
import com.kreait.slack.broker.receiver.InteractiveComponentReceiver
import com.kreait.slack.broker.receiver.MismatchCommandReciever
import com.kreait.slack.broker.receiver.SL4JLoggingReceiver
import com.kreait.slack.broker.receiver.SlashCommandReceiver
import com.kreait.slack.broker.store.event.EventStore
import com.kreait.slack.broker.store.event.InMemoryEventStore
import com.kreait.slack.broker.store.team.TeamStore
import com.kreait.slack.broker.store.user.UserManager
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.boot.autoconfigure.AutoConfigureBefore
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@EnableConfigurationProperties(SlackBrokerConfigurationProperties::class)
@Configuration
open class SlackBrokerAutoConfiguration(private val configuration: SlackBrokerConfigurationProperties) {

    @Configuration
    open class BrokerAutoConfiguration(private val configuration: SlackBrokerConfigurationProperties, private val credentialsProvider: CredentialsProvider) : WebMvcConfigurer {

        @ConditionalOnMissingBean
        @Bean
        open fun eventStore(): EventStore {
            return InMemoryEventStore()
        }

        @Bean
        open fun eventBroker(slackEventReceivers: List<EventReceiver>, teamStore: TeamStore, eventStore: EventStore, metricsCollector: EventMetricsCollector?): EventBroker {
            return EventBroker(slackEventReceivers, teamStore, eventStore, metricsCollector)
        }

        @Bean
        open fun commandBroker(slackEventReceivers: List<SlashCommandReceiver>, teamStore: TeamStore, mismatchCommandReceiver: MismatchCommandReciever?, metricsCollector: CommandMetricsCollector?): CommandBroker {
            return CommandBroker(slackEventReceivers, teamStore, mismatchCommandReceiver, metricsCollector)
        }

        @Bean
        open fun componentBroker(slackInteractiveMessageReceivers: List<InteractiveComponentReceiver<InteractiveMessage>>, slackBlockActionReceivers: List<InteractiveComponentReceiver<BlockActions>>, teamStore: TeamStore, metricsCollector: InteractiveComponentMetricsCollector?): InteractiveComponentBroker {
            return InteractiveComponentBroker(slackBlockActionReceivers, slackInteractiveMessageReceivers, teamStore, metricsCollector)
        }

        override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
            val signingSecret = credentialsProvider.applicationCredentials().signingSecret

            resolvers.add(SlackCommandArgumentResolver(signingSecret))
            resolvers.add(InteractiveResponseArgumentResolver(signingSecret))
            resolvers.add(EventArgumentResolver(signingSecret))
        }

        @ConditionalOnProperty(prefix = SlackBrokerConfigurationProperties.Companion.LOGGING_PROPERTY_PREFIX, name = ["enabled"], havingValue = "true", matchIfMissing = true)
        @Bean
        open fun sL4JLoggingReceiver(): SL4JLoggingReceiver {
            return SL4JLoggingReceiver()
        }

        @ConditionalOnMissingBean
        @ConditionalOnProperty(prefix = SlackBrokerConfigurationProperties.Companion.MISMATCH_PROPERTY_PREFIX, name = ["enabled"], havingValue = "true", matchIfMissing = true)
        @Bean
        open fun commandNotFoundMismatchReceiver(slackClient: SlackClient): MismatchCommandReciever {
            return CommandNotFoundReceiver(slackClient, configuration.commands.mismatch.text)
        }
    }

    @Configuration
    open class InstallationAutoConfiguration(private val configuration: SlackBrokerConfigurationProperties) {

        @ConditionalOnProperty(prefix = SlackBrokerConfigurationProperties.Companion.INSTALLATION_PROPERTY_PREFIX,
                name = ["error-redirect-url", "success-redirect-url"])
        @Bean
        open fun installationBroker(installationReceivers: List<InstallationReceiver>,
                                    teamStore: TeamStore,
                                    userManager: UserManager?,
                                    slackClient: SlackClient,
                                    credentialsProvider: CredentialsProvider,
                                    metricsCollector: InstallationMetricsCollector?): InstallationBroker {

            val installation = this.configuration.installation
            val applicationCredentials = credentialsProvider.applicationCredentials()

            return InstallationBroker(
                    installationReceivers,
                    metricsCollector,
                    teamStore,
                    userManager,
                    slackClient,
                    InstallationBroker.Config(applicationCredentials.clientId, applicationCredentials.clientSecret, installation.successRedirectUrl, installation.errorRedirectUrl)
            )
        }
    }

    @AutoConfigureBefore(InstallationAutoConfiguration::class, BrokerAutoConfiguration::class)
    @ConditionalOnClass(MeterRegistry::class)
    @Configuration
    open class SlackBrokerMetricsAutoConfiguration {

        @ConditionalOnMissingBean
        @Bean
        open fun installationMetrics(): InstallationMetrics {
            return InstallationMetrics()
        }

        @ConditionalOnMissingBean
        @Bean
        open fun eventMetrics(): EventMetrics {
            return EventMetrics()
        }

        @ConditionalOnMissingBean
        @Bean
        open fun commandMetrics(): CommandMetrics {
            return CommandMetrics()
        }

        @ConditionalOnMissingBean
        @Bean
        open fun interactiveComponentMetrics(): InteractiveComponentMetrics {
            return InteractiveComponentMetrics()
        }
    }

    @ConditionalOnMissingBean
    @Bean
    open fun slackClient(): SlackClient {
        return DefaultSlackClient()
    }

    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @Bean
    open fun slackExceptionHandler(): SlackExceptionHandler {
        return SlackExceptionHandler(configuration.application.errorResponse)
    }

    @ConditionalOnMissingBean
    @Bean
    open fun slackCredentialsProvider(): CredentialsProvider {
        return DefaultCredentialsProviderChain()
    }

    @Bean
    open fun slackEvaluationReport(): EvaluationReport {
        return EvaluationReport()
    }
}
