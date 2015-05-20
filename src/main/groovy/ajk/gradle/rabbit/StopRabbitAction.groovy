package ajk.gradle.rabbit

import org.gradle.api.Project

import static File.createTempFile
import static ajk.gradle.rabbit.RabbitPlugin.CYAN
import static ajk.gradle.rabbit.RabbitPlugin.NORMAL
import static org.apache.tools.ant.taskdefs.condition.Os.FAMILY_WINDOWS
import static org.apache.tools.ant.taskdefs.condition.Os.isFamily

class StopRabbitAction {
    private AntBuilder ant

    StopRabbitAction(Project project) {
        ant = project.ant
    }

    void execute() {
        println "${CYAN}* rabbit:$NORMAL stopping RabbitMQ"
        File f = createTempFile("killThemAll", isFamily(FAMILY_WINDOWS) ? ".bat" : ".sh")
        f.deleteOnExit()

        if (isFamily(FAMILY_WINDOWS)) {
            f << """
wmic process where (commandline like "%%erl.exe%%" and not name="wmic.exe") delete
wmic process where (commandline like "%%epmd%%" and not name="wmic.exe") delete
"""
        } else {
            f << """#!/bin/bash
ps ax | grep -i 'epmd' | grep java | grep -v grep | awk '{print \$1}' | xargs kill -SIGINT
ps ax | grep -i 'rabbit' | grep java | grep -v grep | awk '{print \$1}' | xargs kill -SIGINT
"""
        }

        ant.chmod(perm: "755", file: f.getAbsolutePath())
        [f.getAbsolutePath()].execute().waitForOrKill(10 * 1000)

        f.delete()

    }
}
