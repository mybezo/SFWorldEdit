plugins {
    java
    alias(libs.plugins.shadow)
}

group = "com.balugaq.sfworldedit"
// The version comes from version= in gradle.properties; release.yml reads it from there too.

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    // The original MetaLib / DisplayModelLib repository is offline; the JARs in lib/ are used instead.
    // maven("https://repo.metamechanists.org/releases/")
}

dependencies {
    // Core
    compileOnly(libs.paper.api)
    compileOnly(libs.slimefun4)

    // Tools
    compileOnly(libs.guizhan.lib.plugin)
    implementation(libs.bstats)
    compileOnly(libs.findbugs.annotations) {
        exclude("net.jcip", "jcip-annotations")
        exclude("com.google.code.findbugs", "jsr305")
    }
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    // Former Maven compile dependencies; their repository returns 404, so local JARs are shaded instead
    implementation(fileTree(mapOf("dir" to "lib", "include" to listOf("*.jar"))))
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        // Local machines may run JDK 25/26 while CI runs 21; release pins Java 21 bytecode and API.
        options.release = 21
        options.compilerArgs.add("-Xlint:-removal")
    }

    processResources {
        filesMatching("plugin.yml") {
            expand("version" to project.version.toString())
        }
    }

    shadowJar {
        archiveBaseName.set("SFWorldEdit")
        archiveVersion.set(project.version.toString())
        archiveClassifier.set("")

        minimize()

        relocate("org.bstats", "com.balugaq.sfworldedit.bstats")
        exclude("META-INF/*")

        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        mergeServiceFiles()
    }

    build {
        dependsOn(shadowJar)
    }
}

defaultTasks("clean", "build")
