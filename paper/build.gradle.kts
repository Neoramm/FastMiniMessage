plugins {
    id("java-library")
    id("com.vanniktech.maven.publish")
}

group = "io.github.neoramm"
version = "1.2.0"

base {
    archivesName.set("fastminimessage-paper")
}

val paperVersion = "26.2.build.134-stable"
val placeholderApiVersion = "2.12.3"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.extendedclip.com/releases/")
}

dependencies {
    compileOnly("org.jetbrains:annotations:26.0.2")

    api(project(":core"))
    compileOnly("io.papermc.paper:paper-api:$paperVersion")
    compileOnly("me.clip:placeholderapi:$placeholderApiVersion")

    testImplementation("io.papermc.paper:paper-api:$paperVersion")
    testImplementation("net.kyori:adventure-text-serializer-plain:5.2.0")
    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 25
    options.encoding = "UTF-8"
}

mavenPublishing {
    publishToMavenCentral(com.vanniktech.maven.publish.SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()

    coordinates("io.github.neoramm", "fastminimessage-paper")

    pom {
        name.set("FastMiniMessage Paper")
        description.set("Paper API tags for FastMiniMessage")
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
