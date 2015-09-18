package ajk.gradle.rabbit

import de.undercouch.gradle.tasks.download.DownloadAction
import org.gradle.api.Project

import static ajk.gradle.rabbit.RabbitPlugin.CYAN
import static ajk.gradle.rabbit.RabbitPlugin.NORMAL
import static ajk.gradle.rabbit.RabbitPlugin.YELLOW
import static org.apache.tools.ant.taskdefs.condition.Os.FAMILY_WINDOWS
import static org.apache.tools.ant.taskdefs.condition.Os.isFamily

class ErlangActions {
    String version
    File toolsDir
    Project project
    AntBuilder ant
    String erlangHome

    File home

    ErlangActions(Project project, File toolsDir, String erlangHome, String version) {
        this.project = project
        this.toolsDir = toolsDir
        this.erlangHome = erlangHome
        this.version = version
        ant = project.ant
        home = new File(erlangHome ?: "$toolsDir/erlang")
    }

    boolean isInstalled() {
        File erl = isFamily(FAMILY_WINDOWS) ? new File("$home/bin/erl.exe") : new File("/usr/bin/erl")
        return erl.canExecute()
    }

    void install() {
        File erlangHome = new File("$toolsDir/erlang")

        if (!isFamily(FAMILY_WINDOWS)) {
            println "${CYAN}* erlang:$YELLOW Erlang installation on linux requires root access$NORMAL"
            println "${CYAN}* erlang:$NORMAL please install erlang as follows:"
            println """
wget http://packages.erlang-solutions.com/ubuntu/erlang_solutions.asc
sudo apt-key add erlang_solutions.asc
echo "deb http://packages.erlang-solutions.com/ubuntu trusty contrib" sudo tee /etc/apt/sources.list.d/erlang.list
echo "deb http://packages.erlang-solutions.com/ubuntu saucy contrib" sudo tee -a /etc/apt/sources.list.d/erlang.list
echo "deb http://packages.erlang-solutions.com/ubuntu precise contrib" sudo tee -a /etc/apt/sources.list.d/erlang.list
sudo apt-get update
sudo apt-get install -y --force-yes erlang
"""
        } else {
            File erlangFile = new File("$toolsDir/erlang-${version}.exe")
            println "${CYAN}* erlang:$NORMAL installing erlang version $version in $erlangHome"

            DownloadAction erlangDownload = new DownloadAction(project)
            erlangDownload.dest(erlangFile)
            erlangDownload.src("http://www.erlang.org/download/otp_win64_${version}.exe")
            erlangDownload.onlyIfNewer(true)
            erlangDownload.execute()

            ant.delete(dir: "$erlangHome", quiet: true)
            erlangHome.mkdirs()

            [
                    "$erlangFile",
                    "/S",
                    "/D=$erlangHome"
            ].execute().waitFor()
        }
    }
}
