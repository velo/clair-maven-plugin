Submit a Docker image to Clair for scanning
--

This is a simple plugin that retrieves the image manifest for a docker image from a
docker repository, resolves the layers and submit them to [Clair](https://github.com/coreos/clair/) for scanning. 

When all layers have been submitted a html report is produces if the plugin is configured
with a File

Example configuration: 

```
plugins {
  id "no.systemfabrikken.clairplugin" version "1.0"
}

clair {
    imageName = "your/image"
    tag = "1.0"
    reportSeverities = ["High", "Medium", "Unknown"]

    clairHost = "your-clair.host.org"
    registryHost = "your-docker-repository.org"

    htmlReportFile = new File(project.projectDir, "docker-scan.html")
}
```

[Gradle plugin](https://plugins.gradle.org/plugin/no.systemfabrikken.clairplugin)
