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

package org.gradle.internal.instrumentation.property.upgrades;


import org.gradle.api.artifacts.component.BuildIdentifier;
import org.gradle.api.artifacts.component.ProjectComponentSelector;
import org.gradle.api.internal.artifacts.ForeignBuildIdentifier;
import org.gradle.internal.instrumentation.api.annotations.CallableKind;
import org.gradle.internal.instrumentation.api.annotations.InterceptGroovyCalls;
import org.gradle.internal.instrumentation.api.annotations.InterceptJvmCalls;
import org.gradle.internal.instrumentation.api.annotations.ParameterKind;
import org.gradle.internal.instrumentation.api.annotations.SpecificGroovyCallInterceptors;
import org.gradle.internal.instrumentation.api.annotations.SpecificJvmCallInterceptors;
import org.gradle.internal.instrumentation.api.declarations.InterceptorDeclaration;

/**
 * Instrument deprecated BuildIdentifier.getName() to return the last part of the build path.
 */
@SuppressWarnings("NewMethodNamingConvention")
@SpecificJvmCallInterceptors(generatedClassName = InterceptorDeclaration.JVM_BYTECODE_GENERATED_CLASS_NAME_FOR_PROPERTY_UPGRADES + "_BuildIdentifier")
@SpecificGroovyCallInterceptors(generatedClassName = InterceptorDeclaration.GROOVY_INTERCEPTORS_GENERATED_CLASS_NAME_FOR_PROPERTY_UPGRADES + "_BuildIdentifier")
public class BuildIdentifierInterceptorsDeclaration {

    @InterceptJvmCalls
    @CallableKind.InstanceMethod
    public static String intercept_getName(@ParameterKind.Receiver BuildIdentifier self) {
        return getName(self.getBuildPath());
    }

    @InterceptGroovyCalls
    @CallableKind.GroovyPropertyGetter
    public static String intercept_name(@ParameterKind.Receiver BuildIdentifier self) {
        return getName(self.getBuildPath());
    }

    @InterceptJvmCalls
    @CallableKind.InstanceMethod
    public static String intercept_getBuildName(@ParameterKind.Receiver ProjectComponentSelector self) {
        return getName(self.getBuildPath());
    }

    @InterceptGroovyCalls
    @CallableKind.GroovyPropertyGetter
    public static String intercept_buildName(@ParameterKind.Receiver ProjectComponentSelector self) {
        return getName(self.getBuildPath());
    }

    private static String getName(String buildPath) {
        String[] buildPathSequence = buildPath.split(":");
        return buildPathSequence.length == 0 ? ":" : buildPathSequence[buildPathSequence.length - 1];
    }

    @InterceptJvmCalls
    @CallableKind.InstanceMethod
    public static boolean intercept_isCurrentBuild(@ParameterKind.Receiver BuildIdentifier self) {
        return isCurrentBuild(self);
    }

    @InterceptGroovyCalls
    @CallableKind.GroovyPropertyGetter
    public static boolean intercept_currentBuild(@ParameterKind.Receiver BuildIdentifier self) {
        return isCurrentBuild(self);
    }

    private static boolean isCurrentBuild(BuildIdentifier self) {
        return !(self instanceof ForeignBuildIdentifier);
    }
}
