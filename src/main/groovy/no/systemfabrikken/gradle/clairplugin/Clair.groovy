/**
 * Copyright (C) 2020 Marvin Herman Froeder (marvin@marvinformatics.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

        def lastLayer = manifest.lastLayer().name
        def clairUrl = "http://${clairHost}:${clairPort}/v1/layers/${lastLayer}?vulnerabilities"
        def json = slurper.parseText(Unirest.get(clairUrl).asString().body)

        def references = new Vulnerabilities.References(clairLayerReference: lastLayer, clairLayerUrl: clairUrl, imageName: manifest.repo, imageTag: manifest.tag)

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


            return new Vulnerabilities(stats: stats, reportedSeverities: reportSeverities, featuresWithVulnerabilities: featuresWithVulnerabilities, references: references)
        }

        return new Vulnerabilities(references: references)
    }

    public static class Vulnerabilities {

        References references
        Map stats = emptyStats()
        List reportedSeverities = []
        List featuresWithVulnerabilities = []

        static Map emptyStats() {
            return ["High": 0, "Medium": 0, "Low": 0, "Unknown": 0, "Negligible": 0]
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("Vulnerabilities{");
            sb.append("references=").append(references);
            sb.append(", stats=").append(stats);
            sb.append(", reportedSeverities=").append(reportedSeverities);
            sb.append(", featuresWithVulnerabilities=").append(featuresWithVulnerabilities);
            sb.append('}');
            return sb.toString();
        }


        public static class References {
            String clairLayerReference
            String clairLayerUrl
            String imageName
            String imageTag


            @Override
            public String toString() {
                final StringBuilder sb = new StringBuilder("References{");
                sb.append("clairReference='").append(clairLayerReference).append('\'');
                sb.append(", clairUrl='").append(clairLayerUrl).append('\'');
                sb.append(", imageName='").append(imageName).append('\'');
                sb.append(", imageTag='").append(imageTag).append('\'');
                sb.append('}');
                return sb.toString();
            }
        }
    }
}
