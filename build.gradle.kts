plugins {
   kotlin("jvm") version "2.4.20"
   kotlin("plugin.serialization") version "2.4.20"
   application
}

group = "com.timberglund.ledhost"
version = "0.1.0-SNAPSHOP"

repositories {
   mavenCentral()
      maven {
      url = uri("https://jitpack.io")
      content {
         includeGroup("com.github.weliem.blessed-bluez")
         includeGroup("com.github.weliem")
      }
   }
}

// Define platform property (set via gradle.properties or -Pplatform=...)
val targetPlatform = project.findProperty("platform") as String? ?: "raspberrypi"

sourceSets {
   main {
      kotlin {
         setSrcDirs(listOf("src/main/kotlin"))

         // Add platform-specific sources based on target
         when(targetPlatform) {
               "macos" -> srcDir("src/macos/kotlin")
               "raspberrypi" -> srcDir("src/raspberrypi/kotlin")
               else -> throw GradleException("Unknown platform: $targetPlatform. Use 'macos' or 'raspberrypi'")
         }
      }
   }
}

dependencies {
   // Core
   implementation("org.jetbrains.kotlin:kotlin-stdlib:2.4.20")

   // Web Server (Ktor)
   val ktorVersion = "3.6.0"
   implementation("io.ktor:ktor-server-core:$ktorVersion")
   implementation("io.ktor:ktor-server-netty:$ktorVersion")
   implementation("io.ktor:ktor-server-websockets:$ktorVersion")
   implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
   implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

   // Serialization
   implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")

   // Configuration
   implementation("com.charleskorn.kaml:kaml:0.104.0") // YAML

   // CLI (optional, for command-line interface)
   implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.6")

   // Serial Communication
   implementation("com.fazecast:jSerialComm:2.11.4")

   implementation(kotlin("stdlib"))

   // Bluetooth LE library - BLESSED-Bluez for both platforms
   implementation("com.github.weliem.blessed-bluez:blessed:0.65")
   implementation("com.github.hypfvieh:dbus-java-transport-native-unixsocket:4.3.2")

   // Database (Exposed ORM + PostgreSQL JDBC)
   val exposedVersion = "1.5.0"
   implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
   implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
   implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
   implementation("org.postgresql:postgresql:42.7.13")

   // Logging
   implementation("io.github.oshai:kotlin-logging-jvm:8.0.4")
   implementation("ch.qos.logback:logback-classic:1.6.5")

   // Testing
   testImplementation(kotlin("test"))
   testImplementation("org.junit.jupiter:junit-jupiter:6.1.3")
   testImplementation("io.mockk:mockk:1.14.11")
   testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
   testImplementation("io.ktor:ktor-client-core:$ktorVersion")
   testImplementation("io.ktor:ktor-client-cio:$ktorVersion")
   testImplementation("io.ktor:ktor-client-websockets:$ktorVersion")
   testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
}

tasks.register<Exec>("buildFrontend") {
   group = "build"
   description = "Build the React frontend into src/main/resources/web"
   workingDir = file("frontend")
   commandLine("npm", "run", "build")
}

tasks.named("processResources") {
   dependsOn("buildFrontend")
}

tasks.test {
   useJUnitPlatform()
}

kotlin {
   jvmToolchain(21)
}

application {
   mainClass.set("com.timberglund.ledhost.ApplicationKt")
}


// Custom tasks for platform-specific builds
tasks.register("buildMacOS") {
   group = "build"
   description = "Build for macOS platform"
   doFirst {
      project.setProperty("platform", "macos")
   }
   finalizedBy("build")
}

tasks.register("buildRaspberryPi") {
   group = "build"
   description = "Build for Raspberry Pi platform"
   doFirst {
      project.setProperty("platform", "raspberrypi")
   }
   finalizedBy("build")
}

tasks.register("runMacOS") {
   group = "application"
   description = "Run on macOS platform"
   doFirst {
      project.setProperty("platform", "macos")
   }
   finalizedBy("run")
}

tasks.register("runRaspberryPi") {
   group = "application"
   description = "Run on Raspberry Pi platform"
   doFirst {
      project.setProperty("platform", "raspberrypi")
   }
   finalizedBy("run")
}

