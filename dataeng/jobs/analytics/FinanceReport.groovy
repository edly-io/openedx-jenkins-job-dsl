package analytics
import static org.edx.jenkins.dsl.AnalyticsConstants.common_multiscm
import static org.edx.jenkins.dsl.AnalyticsConstants.common_parameters
import static org.edx.jenkins.dsl.AnalyticsConstants.to_date_interval_parameter
import static org.edx.jenkins.dsl.AnalyticsConstants.common_log_rotator
import static org.edx.jenkins.dsl.AnalyticsConstants.common_wrappers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_publishers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_triggers

class FinanceReport {
    public static def payments_validation_job = { dslFactory, allVars ->
        dslFactory.job("payments-validation") {
            logRotator common_log_rotator(allVars)
            parameters common_parameters(allVars)
            parameters to_date_interval_parameter(allVars)
            multiscm common_multiscm(allVars)
            wrappers common_wrappers(allVars)
            publishers common_publishers(allVars)
            steps {
                shell(dslFactory.readFileFromWorkspace('dataeng/resources/payments-validation.sh'))
            }
        }
    }

    public static def cybersource_pull_job = { dslFactory, allVars ->
        dslFactory.job("cybersource-pull") {
            logRotator common_log_rotator(allVars)
            parameters common_parameters(allVars)
            parameters {
                // Override EMR_RELEASE_LABEL for this job.
                stringParam('EMR_RELEASE_LABEL', allVars.get('EMR_RELEASE_LABEL'))
                stringParam('MERCHANT_ID', allVars.get('MERCHANT_ID'))
                stringParam('OUTPUT_ROOT', allVars.get('OUTPUT_ROOT'))
                stringParam('RUN_DATE', allVars.get('RUN_DATE'))
            }
            multiscm common_multiscm(allVars)
            triggers common_triggers(allVars)
            wrappers common_wrappers(allVars)
            publishers common_publishers(allVars)
            publishers {
                downstream("finance-report", 'SUCCESS')
            }
            steps {
                shell(dslFactory.readFileFromWorkspace('dataeng/resources/cybersource-pull.sh'))
            }
        }
    }

    public static def finance_report_job = { dslFactory, allVars ->
        dslFactory.job("finance-report") {

            // DENG-633
            disabled(true)

            logRotator common_log_rotator(allVars)
            multiscm common_multiscm(allVars)
            publishers common_publishers(allVars)
            publishers {
                downstream("payments-validation", 'SUCCESS')
            }
            parameters common_parameters(allVars)
            parameters to_date_interval_parameter(allVars)
            parameters {
                stringParam('OUTPUT_SCHEMA', 'finance', '')
            }
            environmentVariables {
                env('OPSGENIE_HEARTBEAT_NAME', allVars.get('OPSGENIE_HEARTBEAT_NAME'))
                env('OPSGENIE_HEARTBEAT_DURATION_NUM', allVars.get('OPSGENIE_HEARTBEAT_DURATION_NUM'))
                env('OPSGENIE_HEARTBEAT_DURATION_UNIT', allVars.get('OPSGENIE_HEARTBEAT_DURATION_UNIT'))
            }
            wrappers common_wrappers(allVars)
            wrappers {
                credentialsBinding {
                    string('OPSGENIE_HEARTBEAT_CONFIG_KEY', 'opsgenie_heartbeat_config_key')
                }
            }
            steps {
                shell(dslFactory.readFileFromWorkspace('dataeng/resources/opsgenie-enable-heartbeat.sh'))
                shell(dslFactory.readFileFromWorkspace('dataeng/resources/finance-report.sh'))
                shell(dslFactory.readFileFromWorkspace('dataeng/resources/opsgenie-disable-heartbeat.sh'))
            }
        }
    }
}
