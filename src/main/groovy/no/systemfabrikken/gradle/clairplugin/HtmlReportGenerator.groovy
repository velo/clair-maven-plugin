package no.systemfabrikken.gradle.clairplugin

import groovy.xml.MarkupBuilder

public class HtmlReportGenerator {

    private final List features
    private final StringWriter writer

    public HtmlReportGenerator(List features) {
        this.features = features.sort { it.Name }
        this.writer = new StringWriter()
    }

    public String generate() {
        def html = new MarkupBuilder(this.writer)

        html.html {
            head {
                style "body { font-family: sans-serif; margin: 0 auto; max-width: 1000px;} .feature { margin: 5px 0; padding: 0 10px; border-left: 10px solid #ff5555;} .high {color: #ff5555;}"
            }
            body {
                h1 "Vulnerabilities"
                features.collect { f ->
                    div(class: "feature") {
                        h2(style: "border-bottom: 1px solid black;", "Feature/package ${f.Name}")
                        div "Namespace ${f.NamespaceName} "
                        div "Version: ${f.Version}"
                        div "Added by: ${f.AddedBy}"
                        div {
                            f.Vulnerabilities.collect { vuln ->
                                div {
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
                                    div {
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
