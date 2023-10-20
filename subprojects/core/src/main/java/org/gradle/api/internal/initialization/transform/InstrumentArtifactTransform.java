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

package org.gradle.api.internal.initialization.transform;

import com.google.common.io.Files;
import org.apache.commons.io.IOUtils;
import org.gradle.api.artifacts.transform.InputArtifact;
import org.gradle.api.artifacts.transform.TransformAction;
import org.gradle.api.artifacts.transform.TransformOutputs;
import org.gradle.api.artifacts.transform.TransformParameters;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileSystemLocation;
import org.gradle.api.internal.file.temp.TemporaryFileProvider;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.internal.classpath.ClasspathBuilder;
import org.gradle.internal.classpath.ClasspathWalker;
import org.gradle.internal.classpath.TransformedClassPath;
import org.gradle.internal.classpath.transforms.ClasspathElementTransform;
import org.gradle.internal.classpath.transforms.ClasspathElementTransformFactoryForAgent;
import org.gradle.internal.classpath.transforms.InstrumentingClassTransform;
import org.gradle.internal.classpath.types.InstrumentingTypeRegistry;
import org.gradle.internal.file.Stat;
import org.gradle.util.internal.GFileUtils;
import org.gradle.work.DisableCachingByDefault;

import javax.inject.Inject;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.gradle.api.internal.initialization.transform.InstrumentArtifactTransform.InstrumentArtifactTransformParameters;
import static org.gradle.internal.classpath.TransformedClassPath.INSTRUMENTED_DIR_EXTENSION;
import static org.gradle.internal.file.PathTraversalChecker.safePathName;

@DisableCachingByDefault(because = "Not enable yet, since original instrumentation is also not cached in build cache.")
public abstract class InstrumentArtifactTransform implements TransformAction<InstrumentArtifactTransformParameters> {

    public interface InstrumentArtifactTransformParameters extends TransformParameters {
        @InputFiles
        @Classpath
        ConfigurableFileCollection getClassHierarchy();

        @Input
        Property<ClasspathElementTransform.TransformOutput> getOutput();
    }

    @Inject
    public abstract ObjectFactory getObjects();

    @Classpath
    @InputArtifact
    public abstract Provider<FileSystemLocation> getInput();

    private File getInputAsFile() {
        return getInput().get().getAsFile();
    }

    @Override
    public void transform(TransformOutputs outputs) {
        if (!getInputAsFile().exists()) {
            System.out.println("Debug1: " + getInputAsFile() + " does not exist");
            // Don't instrument files that don't exist, these could be files added to classpath via files()
            return;
        }

        // TransformedClassPath.handleInstrumentingArtifactTransform depends on the order and this naming, we should make it more resilient in the future
        String instrumentedJarName = getInput().get().getAsFile().getName().replaceFirst("\\.jar$", TransformedClassPath.INSTRUMENTED_JAR_EXTENSION);
        InstrumentationServices instrumentationServices = getObjects().newInstance(InstrumentationServices.class);

        ClasspathElementTransformFactoryForAgent transformFactory = instrumentationServices.getTransformFactory();
        ClasspathElementTransform transform = transformFactory.createTransformer(getInputAsFile(), new InstrumentingClassTransform(), InstrumentingTypeRegistry.EMPTY);
        if (shouldOutputToDirectory()) {
            instrumentedJarName = getInput().get().getAsFile().getName().replaceFirst("\\.jar$", INSTRUMENTED_DIR_EXTENSION);
            File outputFile = outputs.dir(instrumentedJarName);
            File copyOfOriginalFile = outputs.dir(getInputAsFile().getName().replaceFirst("\\.jar$", ".dir"));
            unzipTo(getInputAsFile(), copyOfOriginalFile);
            transform.transform(outputFile, ClasspathElementTransform.TransformOutput.DIRECTORY);
        } else {
            File outputFile = outputs.file(instrumentedJarName);
            // TODO: Copy in a separate transform
            File copyOfOriginalFile = outputs.file(getInputAsFile().getName());
            GFileUtils.copyFile(getInputAsFile(), copyOfOriginalFile);
            transform.transform(outputFile, ClasspathElementTransform.TransformOutput.JAR);
        }
    }

    private static void unzipTo(File headersZip, File unzipDir) {
        try (ZipInputStream inputStream = new ZipInputStream(new BufferedInputStream(java.nio.file.Files.newInputStream(headersZip.toPath())))) {
            ZipEntry entry;
            while ((entry = inputStream.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                File outFile = new File(unzipDir, safePathName(entry.getName()));
                Files.createParentDirs(outFile);
                try (FileOutputStream outputStream = new FileOutputStream(outFile)) {
                    IOUtils.copyLarge(inputStream, outputStream);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private boolean shouldOutputToDirectory() {
        return getParameters().getOutput().get() == ClasspathElementTransform.TransformOutput.DIRECTORY;
    }

    static class InstrumentationServices {

        private final ClasspathElementTransformFactoryForAgent transformFactory;

        @Inject
        public InstrumentationServices(Stat stat, TemporaryFileProvider temporaryFileProvider) {
            this.transformFactory = new ClasspathElementTransformFactoryForAgent(new ClasspathBuilder(temporaryFileProvider), new ClasspathWalker(stat));
        }

        public ClasspathElementTransformFactoryForAgent getTransformFactory() {
            return transformFactory;
        }
    }
}
