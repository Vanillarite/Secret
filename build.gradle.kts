import java.io.ByteArrayOutputStream

plugins {
    java
    id("net.kyori.blossom") version "1.3.0"
    id("com.github.johnrengelman.shadow") version "7.0.0"
}

val gitBuild: String = run {
    val stdout = ByteArrayOutputStream()
    rootProject.exec {
        commandLine("git", "rev-list", "--count", "HEAD")
        standardOutput = stdout
    }
    stdout.toString().trim()
}

group = "space.rymiel.secret"
version = "0.1.2-b$gitBuild"

java {
    sourceCompatibility = JavaVersion.VERSION_16
    targetCompatibility = JavaVersion.VERSION_16
}

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://papermc.io/repo/repository/maven-public/")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://hub.spigotmc.org/nexus/content/groups/public/")
    maven("https://repo.essentialsx.net/releases/")
    maven("https://repo.essentialsx.net/snapshots/")
    maven("https://nexus.velocitypowered.com/repository/maven-public/")
    maven("https://repo.codemc.org/repository/maven-public")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://jitpack.io")
    maven("https://mvn.exceptionflug.de/repository/exceptionflug-public/")
}

dependencies {
    implementation("cloud.commandframework", "cloud-annotations", "1.6.1")
    compileOnly("com.velocitypowered", "velocity-api", "3.0.1")
    compileOnly("com.velocitypowered", "velocity-proxy", "3.0.1")
    annotationProcessor("com.velocitypowered", "velocity-api", "3.0.1")
    implementation("net.kyori", "adventure-text-minimessage", "4.2.0-SNAPSHOT") {
        isTransitive = false
    }
    implementation("org.spongepowered", "configurate-hocon", "4.1.2")
}

blossom {
    replaceTokenIn("src/main/java/space/rymiel/secret/velocity/SecretVelocity.java")
    replaceToken("@version@", rootProject.version)
}

tasks {
    shadowJar {
        dependencies {
            exclude(dependency("com.google.guava:"))
            exclude(dependency("com.google.errorprone:"))
            exclude(dependency("org.checkerframework:"))
            exclude(dependency("org.jetbrains:"))
            exclude(dependency("org.intellij:"))
        }

        relocate("cloud.commandframework", "${rootProject.group}.shade.cloud")
        relocate("io.leangen.geantyref", "${rootProject.group}.shade.typetoken")
        relocate("net.kyori.adventure.text.minimessage", "${rootProject.group}.shade.minimessage")
        relocate("org.spongepowered.configurate", "${rootProject.group}.shade.configurate")

        archiveClassifier.set(null as String?)
        destinationDirectory.set(rootProject.tasks.shadowJar.get().destinationDirectory.get())
    }
    build {
        dependsOn(shadowJar)
    }
}