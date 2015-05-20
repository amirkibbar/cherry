package ajk.gradle.rabbit

import org.gradle.api.Project
import org.gradle.util.Configurable

import static org.gradle.util.ConfigureUtil.configure

class StopRabbitExtension implements Configurable<StopRabbitExtension> {
    private Project project

    StopRabbitExtension(Project project) {
        this.project = project
    }

    @Override
    StopRabbitExtension configure(Closure closure) {
        configure(closure, new StopRabbitAction(project)).execute()

        return this
    }
}
