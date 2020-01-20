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

import groovy.xml.MarkupBuilder

public class HtmlReportGenerator {

    private final List features
    private final Map stats
    private final StringWriter writer
    private final Clair.Vulnerabilities vulnerabilities

    public HtmlReportGenerator(Clair.Vulnerabilities vulnerabilities) {
        this.vulnerabilities = vulnerabilities
        this.stats = vulnerabilities.stats
        this.features = vulnerabilities.featuresWithVulnerabilities.sort { it.Name }
        this.writer = new StringWriter()
    }

    public String generate() {
        def html = new MarkupBuilder(this.writer)

        html.html {
            head {
                style "body { font-family: sans-serif; margin: 0 auto; max-width: 1000px; } " +
                        ".feature { margin: 5px 0; padding: 0 20px; border-left: 10px solid #ff5555; } " +
                        ".feature .details { margin-bottom: 10px;} " +
                        ".high {font-weight: bold; color: #ff5555; }" +
                        ".vulnerability { padding: 5px; }" +
                        ".vulnerability:last-child { padding: 0; }" +
                        ".even { background: #eee; }"
            }
            body {
                h1 "${stats.values().sum(0)} Vulnerabilities"
                div {
                    stats.collect { key, value -> span(class: key.toLowerCase(), "${key}: ${value} ")}
                }

                div {
                    p {
                        b "Image name: "
                        mkp.yield("${vulnerabilities.references.imageName}")
                    }

                    p {
                        b "Image tag: "
                        mkp.yield("${vulnerabilities.references.imageTag}")
                    }

                    p {
                        b "Clair (last layer) name: "
                        mkp.yield("${vulnerabilities.references.clairLayerReference}")
                    }

                    p {
                        b "Clair (last layer) URL: "
                        a(href: "${vulnerabilities.references.clairLayerUrl}", vulnerabilities.references.clairLayerUrl)
                    }
                }

                features.collect { f ->
                    div(class: "feature") {
                        h2(style: "border-bottom: 1px solid black;", "Feature/package ${f.Name}")

                        div(class: "details") {
                            div {
                                b "Namespace: "
                                mkp.yield(f.NamespaceName)
                            }
                            div {
                                b "Version: "
                                mkp.yield(f.Version)
                            }

                            div {
                                b "Added by: "
                                mkp.yield(f.AddedBy)
                            }
                        }

                        div {
                            f.Vulnerabilities.withIndex(). collect { vuln, index ->
                                div(class: "vulnerability ${index % 2 == 0 ? "even": "odd"}") {
                                    h3 "Name: ${vuln.Name}"
                                    div(class: vuln.Severity.toLowerCase(), "Severity: ${vuln.Severity}")
                                    if (vuln.Description) {
                                        div(style: "max-width: 500px;") {
                                            h4 "Description"
                                            p "${vuln.Description}"
                                        }
                                    }
                                    if (vuln.FixedBy) {
                                        div(style: "background: #a1ffa1;") {
                                            span {
                                                b "Fix available in ${vuln.FixedBy}"
                                            }
                                        }
                                    } else {
                                        div {
                                            span {
                                                b "No fix available"
                                            }
                                        }
                                    }
                                    p {
                                        mkp.yield("More information: " )
                                        a(href: vuln.Link, vuln.Link)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        String report = writer.toString()
        writer.close()
        return report
    }
}
