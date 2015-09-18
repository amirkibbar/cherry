package ajk.gradle.rabbit

import de.undercouch.gradle.tasks.download.DownloadAction
import org.gradle.api.Project

import static ajk.gradle.rabbit.RabbitPlugin.CYAN
import static ajk.gradle.rabbit.RabbitPlugin.NORMAL
import static org.apache.tools.ant.taskdefs.condition.Os.FAMILY_WINDOWS
import static org.apache.tools.ant.taskdefs.condition.Os.isFamily

class RabbitActions {
    String version
    File toolsDir
    Project project
    AntBuilder ant
    File home

    RabbitActions(Project project, File toolsDir, String version) {
        home = new File("$toolsDir/rabbit")
        this.project = project
        this.toolsDir = toolsDir
        this.version = version
        ant = project.ant
    }

    boolean isInstalled() {
        File marker = new File("$toolsDir/rabbit/rabbit.$version")
        return marker.exists()
    }

    void install() {
        File marker = new File("$home/rabbit.$version")
        println "${CYAN}* rabbit:$NORMAL installing RabbitMQ version $version in $home"

        String linuxUrl = "https://www.rabbitmq.com/releases/rabbitmq-server/v$version/rabbitmq-server-generic-unix-${version}.tar.gz"
        String winUrl = "https://www.rabbitmq.com/releases/rabbitmq-server/v$version/rabbitmq-server-windows-${version}.zip"
        String rabbitPackage = isFamily(FAMILY_WINDOWS) ? winUrl : linuxUrl
        File rabbitFile = new File("$toolsDir/rabbit-${version}${isFamily(FAMILY_WINDOWS) ? '.zip' : '.tar.gz'}")

        DownloadAction rabbitDownload = new DownloadAction(project)
        rabbitDownload.dest(rabbitFile)
        rabbitDownload.src(rabbitPackage)
        rabbitDownload.onlyIfNewer(true)
        rabbitDownload.execute()

        ant.delete(dir: "$home", quiet: true)
        ant.touch(file: marker, mkdirs: true)

        if (isFamily(FAMILY_WINDOWS)) {
            ant.unzip(src: rabbitFile, dest: "$home") {
                cutdirsmapper(dirs: 1)
            }
        } else {
            ant.untar(src: rabbitFile, dest: "$home", compression: "gzip") {
                cutdirsmapper(dirs: 1)
            }
        }
    }
}
