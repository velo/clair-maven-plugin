package no.systemfabrikken.gradle.clairplugin

class RunMain {
    public static void main(String[] args) {
        DockerRegistry registry = new DockerRegistry("repo", 5000)
        Clair clair = new Clair(registry, "localhost", 6060, [])

        def manifest = registry.getManifest("repo/image", "tagname")
        clair.submitToClair(manifest)

        def vulnerabilties = clair.getVulnerabilities(manifest)

        new File("./test-report.html").write(new HtmlReportGenerator(vulnerabilties).generate())
    }
}
