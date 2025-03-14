import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jmailen.gradle.kotlinter.tasks.FormatTask
import org.jmailen.gradle.kotlinter.tasks.LintTask

plugins {
    `maven-publish`
    alias(libs.plugins.kotlin.dokka)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.kotlinter)
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.protobuf.gradle)
    id("jacoco")
    id("signing")
    projectversiongen
    steamlanguagegen
    rpcinterfacegen
}

allprojects {
    group = "in.dragonbra"
    version = "1.6.1-SNAPSHOT"
}

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.toVersion(libs.versions.java.get())
    targetCompatibility = JavaVersion.toVersion(libs.versions.java.get())
    withSourcesJar()
}

/* Protobufs */
protobuf.protoc {
    artifact = libs.protobuf.protoc.get().toString()
}

/* Testing */
tasks.test {
    useJUnitPlatform()
    testLogging {
        events = setOf(
            TestLogEvent.FAILED,
            TestLogEvent.PASSED,
            TestLogEvent.SKIPPED,
        )
    }
}

/* Test Reporting */
jacoco.toolVersion = libs.versions.jacoco.get()
tasks.jacocoTestReport {
    reports {
        xml.required = false
        html.required = false
    }
}

/* Java-Kotlin Docs */
dokka {
    moduleName.set("JavaSteam")
    dokkaSourceSets.main {
        suppressGeneratedFiles.set(false) // Allow generated files to be documented.
        perPackageOption {
            // Deny most of the generated files.
            matchingRegex.set("in.dragonbra.javasteam.(protobufs|enums|generated).*")
            suppress.set(true)
        }
    }
}

// Make sure Maven Publishing gets javadoc
val javadocJar by tasks.registering(Jar::class) {
    dependsOn(tasks.dokkaGenerate)
    archiveClassifier.set("javadoc")
    from(layout.buildDirectory.dir("dokka/html"))
}
artifacts {
    archives(javadocJar)
}

/* Configuration */
configurations {
    configureEach {
        // Only allow junit 5
        exclude("junit", "junit")
        exclude("org.junit.vintage", "junit-vintage-engine")
    }
}

/* Source Sets */
sourceSets.main {
    java.srcDirs(
        // builtBy() fixes gradle warning "Execution optimizations have been disabled for task"
        files("build/generated/source/steamd/main/java").builtBy("generateSteamLanguage"),
        files("build/generated/source/javasteam/main/java").builtBy("generateProjectVersion", "generateRpcMethods")
    )
}

/* Dependencies */
tasks["lintKotlinMain"].dependsOn("formatKotlin")
tasks["check"].dependsOn("jacocoTestReport")
tasks["compileJava"].dependsOn("generateSteamLanguage", "generateProjectVersion", "generateRpcMethods")
// tasks["build"].finalizedBy("dokkaGenerate")

/* Kotlinter */
tasks.withType<LintTask> {
    this.source = this.source.minus(fileTree("build/generated")).asFileTree
}
tasks.withType<FormatTask> {
    this.source = this.source.minus(fileTree("build/generated")).asFileTree
}

dependencies {
    implementation(libs.commons.io)
    implementation(libs.commons.lang3)
    implementation(libs.commons.validator)
    implementation(libs.gson)
    implementation(libs.kotlin.coroutines)
    implementation(libs.kotlin.stdib)
    implementation(libs.okHttp)
    implementation(libs.xz)
    implementation(libs.protobuf.java)
    implementation(libs.bundles.ktor)

    testImplementation(libs.bundles.testing)
}

/* Artifact publishing */
/*
nexusPublishing {
    repositories {
        sonatype {
            val ossrhUsername: String by project
            val ossrhPassword: String by project
            username = ossrhUsername
            password = ossrhPassword
        }
    }
}
*/

/*
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifact(javadocJar)
            pom {
                name = "JavaSteam"
                packaging = "jar"
                description = "Java library to interact with Valve's Steam network."
                url = "https://github.com/Longi94/JavaSteam"
                inceptionYear = "2018"
                scm {
                    connection = "scm:git:git://github.com/Longi94/JavaSteam.git"
                    developerConnection = "scm:git:ssh://github.com:Longi94/JavaSteam.git"
                    url = "https://github.com/Longi94/JavaSteam/tree/master"
                }
                licenses {
                    license {
                        name = "MIT License"
                        url = "https://www.opensource.org/licenses/mit-license.php"
                    }
                }
                developers {
                    developer {
                        id = "Longi"
                        name = "Long Tran"
                        email = "lngtrn94@gmail.com"
                    }
                }
            }
        }
    }
}

signing {
    sign(publishing.publications["mavenJava"])
}
*/

publishing {
    publications {
        // 保留原有的 mavenJava 发布配置
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifact(javadocJar)
            // 保持原有的 groupId (in.dragonbra)
            groupId = "com.xingchao"
            // artifactId 默认为项目名称
            // 保持原有的版本 (1.6.1-SNAPSHOT)

            pom {
                name = "JavaSteam"
                packaging = "jar"
                description = "Java library to interact with Valve's Steam network."
                url = "https://github.com/Longi94/JavaSteam"
                inceptionYear = "2018"
            }
        }
    }

    repositories {
        // 添加阿里云私有仓库
        maven {
            name = "aliyun"
            val releasesRepoUrl = "https://packages.aliyun.com/60b0a98cb8301d20d58b514c/maven/2106878-release-1hzhlx"
            val snapshotsRepoUrl = "https://packages.aliyun.com/60b0a98cb8301d20d58b514c/maven/2106878-snapshot-ajrzdr"

            url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)

            credentials {
                username = "5fa79bad2d5925c55b9e9dfe"
                password = "lL)2GAz99k_2"
            }
        }
    }
}

