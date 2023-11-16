plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
     groovy
}

version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(gradleApi())
    implementation(localGroovy())

    implementation("org.jetbrains.kotlin:kotlin-stdlib")

    // https://mvnrepository.com/artifact/commons-io/commons-io
    implementation("commons-io:commons-io:2.14.0")
    // https://mvnrepository.com/artifact/com.squareup/kotlinpoet
    implementation("com.squareup:kotlinpoet:1.14.2")
}

gradlePlugin {
    plugins {
        create("steamlanguagegen") {
            id = "steamlanguagegen"
            implementationClass = "in.dragonbra.steamlanguagegen.SteamLanguageGenPlugin"
        }
        create("projectversiongen") {
            id = "projectversiongen"
            implementationClass = "in.dragonbra.generators.versions.VersionGenPlugin"
        }
    }
}
