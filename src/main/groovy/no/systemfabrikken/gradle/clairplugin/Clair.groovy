package no.systemfabrikken.gradle.clairplugin

import com.mashape.unirest.http.Unirest
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper

public class Clair {

    private final DockerRegistry registry
    private final String clairHost
    private final int clairPort
    private final List<String> reportSeverities

    Clair(DockerRegistry registry, String clairHost, int clairPort, List<String> reportSeverities) {
        this.reportSeverities = reportSeverities
        this.clairHost = Objects.requireNonNull(clairHost, "Clair hostname must be provided")
        this.clairPort = clairPort
        this.registry = registry
    }

    public void submitToClair(DockerRegistry.ImageManifest manifest) {
        manifest.layers.withIndex().each { layer, int index ->

            def blobPath = registry.blobPath(manifest, layer)
            def json

            if (layer.parent) {
                json = new JsonBuilder([Layer: [Name: layer.name, Format: "Docker", Path: blobPath, ParentName: "${manifest.layers[index - 1].name}"]])
            } else {
                // Name to send to Clair. If layer is parentless assume it is topmost, i.e. using only repo and tag for easy recursive delete from Clair
                json = new JsonBuilder([Layer: [Name: layer.name, Format: "Docker", Path: blobPath]])
            }

            Unirest.setTimeouts(1000, 0) // Layers can take time to scan.


            def response = Unirest.post("http://${clairHost}:${clairPort}/v1/layers")
                    .header("accept", "application/json")
                    .header("Content-Type", "application/json")
                    .body(json.toString())
                    .asString()
        }
    }

    public void removeFromClair(DockerRegistry.ImageManifest manifest) {
        Unirest.delete("http://${clairHost}:${clairPort}/v1/layers/${manifest.parentLayer.name}")
                .header("accept", "application/json")
                .header("Content-Type", "application/json")
                .body()
                .asString()
    }

    public def getVulnerabilities(DockerRegistry.ImageManifest manifest) {
        JsonSlurper slurper = new JsonSlurper()
        Map stats = Vulnerabilities.emptyStats()

        def json = slurper.parseText(Unirest.get("http://${clairHost}:${clairPort}/v1/layers/${manifest.layers.last().name}?vulnerabilities").asString().body)

        if (json.Error) {
            println json
            return
        }

        if (json.Layer.Features && json.Layer.Features.any { it.Vulnerabilities }) {
            def featuresWithVulnerabilities = json.Layer.Features.findAll { it.Vulnerabilities }

            if (!reportSeverities.isEmpty()) {
                featuresWithVulnerabilities = featuresWithVulnerabilities.collect({
                    def vulnsMatchingSeverity = it.Vulnerabilities.findAll { vuln -> reportSeverities.contains(vuln.Severity) }
                    it.Vulnerabilities = vulnsMatchingSeverity
                    return it
                }).findAll({ it.Vulnerabilities })
            }

            featuresWithVulnerabilities.each {
                it.Vulnerabilities.each { v ->
                    stats[v.Severity] += 1
                }
            }

            return new Vulnerabilities(stats: stats, reportedSeverities: reportSeverities, featuresWithVulnerabilities: featuresWithVulnerabilities)
        }

        return new Vulnerabilities()
    }

    public static class Vulnerabilities {

        Map stats = emptyStats()
        List reportedSeverities = []
        List featuresWithVulnerabilities = []

        static Map emptyStats() {
            return new TreeMap(["High": 0, "Medium": 0, "Low": 0, "Unknown": 0, "Negligible": 0])
        }


        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("Vulnerabilities{");
            sb.append("stats=").append(stats);
            sb.append(", reportedSeverities=").append(reportedSeverities);
            sb.append(", featuresWithVulnerabilities=").append(featuresWithVulnerabilities);
            sb.append('}');
            return sb.toString();
        }
    }
}
