package ajk.gradle.rabbit

import org.gradle.api.Project
import org.gradle.util.Configurable

import static org.gradle.util.ConfigureUtil.configure

class StartRabbitExtension implements Configurable<StartRabbitExtension> {
    private Project project

    StartRabbitExtension(Project project) {
        this.project = project
    }

    @Override
    StartRabbitExtension configure(Closure closure) {
        configure(closure, new StartRabbitAction(project)).execute()

        return this
    }
}
