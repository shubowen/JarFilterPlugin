/*
 * Copyright 2019. nekocode (nekocode.cn@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.nekocode.jarfilter

import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.regex.Pattern

/**
 * Replaces the removed AGP Transform API with the Scoped Artifacts API.
 *
 * AGP provides all classes in the selected scope as jars and directories. This task writes them
 * back into a single jar after applying jarFilters to matched external dependency jars.
 *
 * @author nekocode (nekocode.cn@gmail.com)
 */
abstract class JarFilterTransform : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val allJars: ListProperty<RegularFile>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val allDirectories: ListProperty<Directory>

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val configFile: RegularFileProperty

    @get:Input
    abstract val artifactNames: MapProperty<String, String>

    @get:OutputFile
    abstract val output: RegularFileProperty

    @TaskAction
    fun filter() {
        val configs = Utils.getConfigsFromFile(configFile.get().asFile).orEmpty()
        val filters = configs.map {
            Pattern.compile(it.name) to JarFilter(it)
        }
        val jarsToArtifactNames = artifactNames.get()
        val writtenEntries = LinkedHashSet<String>()
        val outputFile = output.get().asFile
        outputFile.parentFile.mkdirs()

        JarOutputStream(BufferedOutputStream(FileOutputStream(outputFile))).use { jarOutput ->
            allJars.get().forEach { regularFile ->
                val jarFile = regularFile.asFile
                val filter = findFilter(jarFile, jarsToArtifactNames, filters)
                copyJarEntries(jarFile, jarOutput, filter, writtenEntries)
            }

            allDirectories.get().forEach { directory ->
                val rootDir = directory.asFile
                rootDir.walkTopDown()
                        .filter { it.isFile }
                        .forEach { file ->
                            val relativePath = rootDir.toURI()
                                    .relativize(file.toURI())
                                    .path
                                    .replace(File.separatorChar, '/')
                            file.inputStream().use { input ->
                                jarOutput.writeEntry(relativePath, input, writtenEntries)
                            }
                        }
            }
        }
    }

    private fun findFilter(
            jarFile: File,
            jarsToArtifactNames: Map<String, String>,
            jarFilters: List<Pair<Pattern, JarFilter>>): JarFilter? {

        val candidateNames = listOfNotNull(
                jarsToArtifactNames[jarFile.absolutePath],
                jarFile.name,
                jarFile.absolutePath
        )

        return jarFilters.firstOrNull { (pattern, _) ->
            candidateNames.any { pattern.matcher(it).matches() }
        }?.second
    }

    private fun copyJarEntries(
            sourceJar: File,
            jarOutput: JarOutputStream,
            filter: JarFilter?,
            writtenEntries: MutableSet<String>) {

        if (!sourceJar.exists()) {
            return
        }

        JarFile(sourceJar).use { jarFile ->
            jarFile.entries().asSequence().forEach { entry ->
                if (entry.isDirectory || filter?.test(entry.name) == false) {
                    return@forEach
                }

                jarFile.getInputStream(entry).use { input ->
                    jarOutput.writeEntry(entry.name, input, writtenEntries)
                }
            }
        }
    }

    private fun JarOutputStream.writeEntry(
            name: String,
            inputStream: InputStream,
            writtenEntries: MutableSet<String>) {

        if (!writtenEntries.add(name)) {
            return
        }

        putNextEntry(JarEntry(name))
        inputStream.copyTo(this)
        closeEntry()
    }
}
