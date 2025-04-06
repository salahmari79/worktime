plugins {
    kotlin("jvm")
    application
}

group = "com.example.mywork"
version = "1.0-SNAPSHOT"

dependencies {
    implementation(project(":app"))
    implementation("org.openjfx:javafx-controls:21")
    implementation("org.openjfx:javafx-fxml:21")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
}

application {
    mainClass.set("com.example.mywork.desktop.WorkTrackerApp")
}

tasks.test {
    useJUnit()
}

kotlin {
    jvmToolchain(8)
} 