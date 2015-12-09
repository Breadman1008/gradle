/*
 * Copyright 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.integtests.resolve.http
import org.gradle.integtests.fixtures.AbstractHttpDependencyResolutionTest
import org.gradle.integtests.fixtures.TestResources
import org.gradle.test.fixtures.keystore.TestKeyStore
import org.junit.Rule

import static org.gradle.util.Matchers.containsText

abstract class AbstractHttpsRepoResolveIntegrationTest extends AbstractHttpDependencyResolutionTest {
    @Rule TestResources resources = new TestResources(temporaryFolder)
    TestKeyStore keyStore

    abstract protected String setupRepo()

    def "resolve with server certificate"() {
        keyStore = TestKeyStore.init(resources.dir)
        keyStore.enableSslWithServerCert(server)

        def repoType = setupRepo()
        setupBuildFile(repoType)

        when:
        keyStore.configureServerCert(executer)
        succeeds "libs"

        then:
        file('libs').assertHasDescendants('my-module-1.0.jar')
    }

    def "resolve with server and client certificate"() {
        keyStore = TestKeyStore.init(resources.dir)
        keyStore.enableSslWithServerAndClientCerts(server)

        def repoType = setupRepo()
        setupBuildFile(repoType)

        when:
        keyStore.configureServerAndClientCerts(executer)
        succeeds "libs"

        then:
        file('libs').assertHasDescendants('my-module-1.0.jar')
    }

    def "decent error message when client can't authenticate server"() {
        keyStore = TestKeyStore.init(resources.dir)
        keyStore.enableSslWithServerCert(server)

        def repoType = setupRepo()
        setupBuildFile(repoType)

        when:
        executer.withStackTraceChecksDisabled() // Jetty logs stuff to console
        keyStore.configureIncorrectServerCert(executer)
        fails "libs"

        then:
        failure.assertThatCause(containsText("Could not GET '${server.uri}/repo1/my-group/my-module/1.0/"))
        failure.assertOutputContains("javax.net.ssl.SSLHandshakeException")
    }

    def "decent error message when server can't authenticate client"() {
        keyStore = TestKeyStore.init(resources.dir)
        keyStore.enableSslWithServerAndBadClientCert(server)

        def repoType = setupRepo()
        setupBuildFile(repoType)

        when:
        executer.withStackTraceChecksDisabled() // Jetty logs stuff to console
        keyStore.configureServerAndClientCerts(executer)
        fails "libs"

        then:
        failure.assertThatCause(containsText("Could not GET '${server.uri}/repo1/my-group/my-module/1.0/"))
    }

    private void setupBuildFile(String repoType) {
        buildFile << """
repositories {
    $repoType { url '${server.uri}/repo1' }
}
configurations { compile }
dependencies {
    compile 'my-group:my-module:1.0'
}
task libs(type: Copy) {
    into 'libs'
    from configurations.compile
}
        """
    }
}

