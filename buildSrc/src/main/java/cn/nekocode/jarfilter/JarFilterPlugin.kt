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

import com.android.build.api.artifact.ScopedArtifact
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.ScopedArtifacts
import com.android.build.gradle.AppPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.type.ArtifactTypeDefinition

/**
 * Debug: ./gradlew :e:build -Dorg.gradle.daemon=false -Dorg.gradle.debug=true
 * @author nekocode (nekocode.cn@gmail.com)
 */
class JarFilterPlugin : Plugin<Project> {
    companion object {
        const val CONFIG_KEYWORD = "jarFilters"
        const val UPDATE_CONFIG_TASK_NAME = "updateConfig"
    }

    override fun apply(project: Project) {
        // Add config object
        val jarFilters = project.container(JarFilterConfig::class.java) { name ->
            JarFilterConfig(name, emptySet(), emptySet())
        }
        project.extensions.add(CONFIG_KEYWORD, jarFilters)

        var appliedToAndroidApplication = false
        project.plugins.withType(AppPlugin::class.java) {
            appliedToAndroidApplication = true

            // Create a task to save config to json file before build.
            val updateTask = project.tasks.register(UPDATE_CONFIG_TASK_NAME, UpdateConfigTask::class.java)

            val androidComponents = project.extensions.getByType(ApplicationAndroidComponentsExtension::class.java)
            androidComponents.onVariants { variant ->
                variant.lifecycleTasks.registerPreBuild(updateTask)

                val filterTask = project.tasks.register("${variant.name}JarFilter", JarFilterTransform::class.java) { task ->
                    task.dependsOn(updateTask)
                    task.configFile.set(project.layout.buildDirectory.file(UpdateConfigTask.CONFIG_FILE_NAME))
                    val classArtifacts = variant.runtimeConfiguration.incoming.artifactView { view ->
                        view.componentFilter { componentIdentifier ->
                            componentIdentifier is ModuleComponentIdentifier
                        }
                        view.attributes { attributes ->
                            attributes.attribute(
                                    ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE,
                                    "android-classes-jar")
                        }
                    }.artifacts.resolvedArtifacts

                    task.artifactNames.set(classArtifacts.map { artifacts ->
                        artifacts.filter { artifact ->
                            artifact.file.isFile && artifact.file.extension == "jar"
                        }.associate { artifact ->
                            val file = artifact.file
                            val componentIdentifier = artifact.id.componentIdentifier as ModuleComponentIdentifier
                            file.absolutePath to
                                    "${componentIdentifier.group}:${componentIdentifier.module}:${componentIdentifier.version}"
                        }
                    })
                }

                variant.artifacts.forScope(ScopedArtifacts.Scope.ALL)
                        .use(filterTask)
                        .toTransform(
                                ScopedArtifact.CLASSES,
                                JarFilterTransform::allJars,
                                JarFilterTransform::allDirectories,
                                JarFilterTransform::output
                        )
            }
        }

        if (project.plugins.hasPlugin("com.android.library") || project.plugins.hasPlugin("java-library")) {
            throw UnsupportedOperationException(
                    "The JarFilterPlugin can only be used in android application module.")
        }

        project.afterEvaluate {
            if (!appliedToAndroidApplication) {
                throw UnsupportedOperationException(
                        "The JarFilterPlugin can only be used in android application module.")
            }
        }
    }
}
