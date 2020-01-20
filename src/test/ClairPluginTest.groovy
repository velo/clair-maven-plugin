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

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Ignore
import spock.lang.Specification

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class ClairPluginTest extends Specification {

    @Rule
    final TemporaryFolder testProjectDir = new TemporaryFolder()

    @Ignore
    def "task clairScanAndReport runs without problem"() {
        when:
        def result = runTask 'clairScanAndReport', """
            plugins {
                id "no.systemfabrikken.clairplugin"
            }
            
            clair {
                imageName = "base/debian-jessie"
                tag = "7.1711160804.bbaeb40"
            
                clairHost = "clair host"
                registryHost = "docker registry host"             
                            
            }
        """

        then:
        result.output.contains('Sending callback')
        result.task(":clairScanAndReport").outcome == SUCCESS
    }

    private BuildResult runTask(String task, String gradlescript) {
        def buildFile = testProjectDir.newFile('build.gradle')
        buildFile << gradlescript
        GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments(task)
                .withDebug(true)
                .withPluginClasspath()
                .build()
    }
}
