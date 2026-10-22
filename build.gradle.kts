plugins {
    id("java-library")
    id("com.vanniktech.maven.publish") version "0.30.0"
}

group = "io.github.neoramm"
version = "1.1.0"

val kyoriVersion = "4.26.1"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("org.jetbrains:annotations:26.0.2")

    api("net.kyori:adventure-api:$kyoriVersion")
    testImplementation("net.kyori:adventure-text-minimessage:$kyoriVersion")
    testImplementation("net.kyori:adventure-text-serializer-plain:$kyoriVersion")
    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    testImplementation("org.openjdk.jmh:jmh-core:1.37")
    testAnnotationProcessor("org.openjdk.jmh:jmh-generator-annprocess:1.37")
}

tasks.test {
    useJUnitPlatform()
}

tasks.register<JavaExec>("jmh") {
    group = "benchmark"
    description = "Benchmark JHM executor"
    dependsOn(tasks.named("testClasses"))
    mainClass.set("org.openjdk.jmh.Main")
    classpath = sourceSets["test"].runtimeClasspath
    val results = layout.buildDirectory.file("jmh/results.json")
    doFirst {
        results.get().asFile.parentFile.mkdirs()
    }
    val extra = (project.findProperty("jmhArgs") as String?)?.split(" ")?.filter { it.isNotBlank() } ?: emptyList()
    args = listOf("-prof", "gc", "-rf", "json", "-rff", results.get().asFile.absolutePath) + extra
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 21
    options.encoding = "UTF-8"
}

java {
}

mavenPublishing {
    publishToMavenCentral(com.vanniktech.maven.publish.SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()

    coordinates("io.github.neoramm", "fastminimessage")

    pom {
        name.set("FastMiniMessage")
        description.set("Fast precompiled MiniMessage rendering for Minecraft/Adventure")
        url.set("https://github.com/neoramm/FastMiniMessage")
        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/licenses/MIT")
            }
        }
        developers {
            developer {
                id.set("neoramm")
                name.set("neoramm")
            }
        }
        scm {
            url.set("https://github.com/neoramm/FastMiniMessage")
            connection.set("scm:git:https://github.com/neoramm/FastMiniMessage.git")
            developerConnection.set("scm:git:ssh://git@github.com/neoramm/FastMiniMessage.git")
        }
    }
}

