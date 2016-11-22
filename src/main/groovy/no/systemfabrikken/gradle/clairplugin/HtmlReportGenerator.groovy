package no.systemfabrikken.gradle.clairplugin

import groovy.xml.MarkupBuilder

public class HtmlReportGenerator {

    private final List features
    private final Map stats
    private final StringWriter writer

    public HtmlReportGenerator(Clair.Vulnerabilities vulnerabilities) {
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
                    stats.collect { key, value -> span "${key}: ${value} " }
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
