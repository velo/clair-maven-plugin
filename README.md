# clair-maven-plguin

[![Build Status](https://travis-ci.org/velo/clair-maven-plugin.svg?branch=master)](https://travis-ci.org/velo/clair-maven-plugin?branch=master) 
[![Coverage Status](https://coveralls.io/repos/github/velo/clair-maven-plugin/badge.svg?branch=master)](https://coveralls.io/github/velo/clair-maven-plugin?branch=master) 
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.marvinformatics/clair-maven-plugin/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.marvinformatics/clair-maven-plugin/) 
[![Issues](https://img.shields.io/github/issues/velo/clair-maven-plugin.svg)](https://github.com/velo/clair-maven-plugin/issues) 
[![Forks](https://img.shields.io/github/forks/velo/clair-maven-plugin.svg)](https://github.com/velo/clair-maven-plugin/network) 
[![Stars](https://img.shields.io/github/stars/velo/clair-maven-plugin.svg)](https://github.com/velo/clair-maven-plugin/stargazers)

Submit a Docker image to Clair for scanning
--

*NOTE: only works against a docker repository servering schema 2.1*

This is a simple plugin that retrieves the image manifest for a docker image from a
docker repository, resolves the layers and submit them to [Clair](https://github.com/coreos/clair/) for scanning. 

When all layers have been submitted a html report is produces if the plugin is configured
with a File

Example configuration: 

```
plugins {
  id "no.systemfabrikken.clairplugin" version "1.0.1"
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

[Example output](https://htmlpreview.github.io/?https://github.com/systemfabrikken/gradle-clair-plugin/blob/master/report-example.html)

[Gradle plugin](https://plugins.gradle.org/plugin/no.systemfabrikken.clairplugin)
