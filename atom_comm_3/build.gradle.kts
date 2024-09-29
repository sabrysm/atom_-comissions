plugins {
    id("java")
    application
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "com.me"
version = "1.0-SNAPSHOT"

val jdaVersion = "5.0.2"
application.mainClass = "com.me.LootSplit.Main"

repositories {
    mavenCentral()
}

dependencies {
    implementation("net.dv8tion:JDA:$jdaVersion")
    implementation("ch.qos.logback:logback-classic:1.5.6")
    implementation("io.github.cdimascio:dotenv-java:3.0.0")
    implementation("org.xerial:sqlite-jdbc:3.46.0.1")
    implementation("net.sourceforge.tess4j:tess4j:5.12.0")
    implementation("org.bytedeco:javacv-platform:1.5.10")
    implementation("org.apache.commons:commons-lang3:3.0")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.isIncremental = true

    // Set this to the version of java you want to use,
    // the minimum required for JDA is 1.8
    sourceCompatibility = "1.8"
}