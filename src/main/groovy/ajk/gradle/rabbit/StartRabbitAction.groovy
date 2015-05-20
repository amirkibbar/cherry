package ajk.gradle.rabbit

import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

import static ajk.gradle.rabbit.RabbitPlugin.CYAN
import static ajk.gradle.rabbit.RabbitPlugin.GREEN
import static ajk.gradle.rabbit.RabbitPlugin.NORMAL
import static ajk.gradle.rabbit.RabbitPlugin.RED
import static org.apache.tools.ant.taskdefs.condition.Os.FAMILY_WINDOWS
import static org.apache.tools.ant.taskdefs.condition.Os.isFamily

class StartRabbitAction {
    static final String DEFAULT_RABBIT_VERSION = "3.5.2"
    static final String DEFAULT_ERLANG_VERSION = "17.5"

    @Input
    @Optional
    String rabbitVersion

    @Input
    @Optional
    String erlangVersion

    @Input
    @Optional
    String erlangHome

    @Input
    @Optional
    int nodePort

    @Input
    @Optional
    int mgmtPort

    @Input
    @Optional
    int epmdPort

    @Input
    @Optional
    File toolsDir

    private Project project

    private AntBuilder ant

    StartRabbitAction(Project project) {
        this.project = project
        this.ant = project.ant
    }

    void execute() {
        File toolsDir = toolsDir ?: new File("$project.rootDir/gradle/tools")
        ErlangActions erlang = new ErlangActions(project, toolsDir, erlangHome, erlangVersion ?: DEFAULT_ERLANG_VERSION)

        if (!erlang.installed) {
            erlang.install()
        }

        // verify that the installation was successful
        if (!erlang.installed) {
            println "${CYAN}* rabbit:$RED couldn't install erlang, please install it manually and try again$NORMAL"
            throw new IllegalStateException("couldn't install erlang, please install it manually")
        }

        RabbitActions rabbit = new RabbitActions(project, toolsDir, rabbitVersion ?: DEFAULT_RABBIT_VERSION)

        if (!rabbit.installed) {
            rabbit.install()
        }

        nodePort = nodePort ?: 5672
        mgmtPort = mgmtPort ?: 15672
        epmdPort = epmdPort ?: 4369

        File baseDir = new File("$project.buildDir/rabbit")
        File configFile = new File("$baseDir/rabbitmq.config")

        println "${CYAN}* rabbit:$NORMAL starting on port $nodePort and management port $mgmtPort"
        println "${CYAN}* rabbit:$NORMAL RabbitMQ Base directory: $baseDir"

        if (baseDir.exists() && !baseDir.delete()) {
            println "${CYAN}* rabbit:$RED couldn't delete $baseDir, please make sure RabbitMQ is down and try again$NORMAL"
            throw new IllegalStateException("couldn't delete $baseDir, please make sure RabbitMQ is down and try again")
        }

        ant.touch(file: configFile, mkdirs: true)
        configFile << """[
 {rabbit, [{tcp_listeners, [$nodePort]}]},

 {rabbitmq_management, [{listener, [{port, $mgmtPort}]}]},

 {loopback_users, []}
].
"""
        ant.chmod(perm: "755", dir: "$rabbit.home/sbin", includes: "*")
        println "${CYAN}* rabbit:$NORMAL starting RabbitMQ node"
        File rabbitScript = new File("$rabbit.home/sbin/rabbitmq-server${isFamily(FAMILY_WINDOWS) ? '.bat' : ''}")

        [
                rabbitScript.absolutePath,
                "-detached"
        ].execute(
                [
                        """PATH=${new File("$erlang.home/bin")}""",
                        "ERLANG_HOME=$erlang.home",
                        "RABBITMQ_BASE=$baseDir",
                        "ERL_EPMD_PORT=$epmdPort",
                        "RABBITMQ_NODENAME=${System.currentTimeMillis()}@${InetAddress.localHost.hostName}"
                ],
                rabbit.home)

        println "${CYAN}* rabbit:$NORMAL enabling the management plugin"
        File pluginsScript = new File("$rabbit.home/sbin/rabbitmq-plugins${isFamily(FAMILY_WINDOWS) ? '.bat' : ''}")

        [
                pluginsScript,
                "enable",
                "rabbitmq_management"
        ].execute(["""PATH=${new File("$erlang.home/bin")}""",
                   "ERLANG_HOME=$erlang.home",
                   "RABBITMQ_BASE=$baseDir"],
                rabbit.home)

        println "${CYAN}* rabbit:$NORMAL waiting for RabbitMQ to start, testing ports $nodePort and $mgmtPort"
        ant.waitfor(maxwait: 2, maxwaitunit: "minute", timeoutproperty: "rabbitTimeout") {
            and {
                socket(server: "localhost", port: nodePort)
                socket(server: "localhost", port: mgmtPort)
            }
        }

        if (ant.properties['rabbitTimeout'] != null) {
            println "${CYAN}* rabbit:$RED could not start RabbitMQ$NORMAL"
            throw new RuntimeException("failed to start RabbitMQ")
        } else {
            println "${CYAN}* rabbit:$GREEN RabbitMQ is now up$NORMAL"
        }
    }
}
