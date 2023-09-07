/*
 * Copyright 2023 the original author or authors.
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

package org.gradle.api.plugins.jvm.internal.testing.toolchains;

import org.gradle.api.plugins.jvm.testing.toolchains.KotlinTestToolchainParameters;

abstract public class KotlinTestToolchain extends AbstractJUnitPlatformTestEngineToolchain<KotlinTestToolchainParameters> {
    public static final String DEFAULT_VERSION = "1.9.0";
    private static final String GROUP_NAME = "org.jetbrains.kotlin:kotlin-test-junit5";

    public KotlinTestToolchain() {
        super(GROUP_NAME);
    }

    @Override
    protected String getVersion() {
        return getParameters().getKotlinTestVersion().get();
    }

}
