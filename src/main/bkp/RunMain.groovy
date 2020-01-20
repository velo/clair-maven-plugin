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
