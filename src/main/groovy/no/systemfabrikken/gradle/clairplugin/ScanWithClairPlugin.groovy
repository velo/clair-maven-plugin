package no.systemfabrikken.gradle.clairplugin

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction

class ScanWithClairPlugin implements Plugin<Project> {
    void apply(Project project) {
        project.extensions.create("clair", ScanWithClairPluginExtension)

        project.task([type       : ClairScanImageTask,
                      group      : "Clair",
                      description: "Get layers blob adresses from the given image/tag manifest from given repository and submits them to clair. " +
                              "A HTML report is produced that can be written to file etc."],
                "clairScanAndReport")
    }
}

class ScanWithClairPluginExtension {
    String imageName
    String tag

    String registryHost
    int registryPort = 5000

    String clairHost
    int clairPort = 6060

    // If empty report all, otherwise only specific
    List<String> reportSeverities = []

    File htmlReportFile // optional, writes a html report to the given File
}


class ClairScanImageTask extends DefaultTask {

    private ScanWithClairPluginExtension config

    @TaskAction
    def scanImage() {

        this.config = project.clair

        DockerRegistry registry = new DockerRegistry(config.registryHost, config.registryPort)
        Clair clair = new Clair(registry, config.clairHost, config.clairPort, config.reportSeverities)

        def manifest = registry.getManifest(config.imageName, config.tag)
        clair.submitToClair(manifest)

        def vulnerabilties = clair.getVulnerabilities(manifest)

        if (config.htmlReportFile) {
            def report = new HtmlReportGenerator(vulnerabilties).generate()
            config.htmlReportFile.write(report)
            println "html report written to ${config.htmlReportFile.absolutePath}"
        }
    }
}

