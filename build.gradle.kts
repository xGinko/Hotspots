plugins {
    java
    id("com.gradleup.shadow") version ("9.0.0-beta6")
}

group = "me.xginko"
version = "1.0.0"
description = "Temporary teleport locations that other players can teleport to."

repositories {
    mavenCentral()

    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc-repo"
    }

    maven("https://oss.sonatype.org/content/groups/public/") {
        name = "sonatype"
    }

    maven("https://repo.bsdevelopment.org/releases") {
        name = "configmaster-repo"
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    compileOnly("org.apache.logging.log4j:log4j-core:2.24.3")

    implementation("com.github.thatsmusic99:ConfigurationMaster-API:v2.0.0-rc.3")
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")
    implementation("org.reflections:reflections:0.10.2")
    implementation("com.zaxxer:HikariCP:6.2.1")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks {
    compileJava {
        options.encoding = Charsets.UTF_8.name()
    }

    javadoc {
        options.encoding = Charsets.UTF_8.name()
    }

    build.configure {
        dependsOn("shadowJar")
    }

    shadowJar {
        archiveFileName.set("Hotspots-${version}.jar")

        relocate("com.github.benmanes.caffeine", "me.xginko.hotspots.libs.caffeine")
        relocate("io.github.thatsmusic99.configurationmaster", "me.xginko.hotspots.libs.configmaster")
        relocate("com.zaxxer", "me.xginko.hotspots.libs.zaxxer")
        relocate("org.joml", "me.xginko.hotspots.libs.joml")
        relocate("org.reflections", "me.xginko.hotspots.libs.reflections")
    }

    processResources {
        filesMatching("plugin.yml") {
            expand(
                    mapOf(
                            "name" to project.name,
                            "version" to project.version,
                            "description" to project.description!!.replace('"'.toString(), "\\\""),
                            "url" to "https://github.com/xGinko/Hotspots"
                    )
            )
        }
    }
}