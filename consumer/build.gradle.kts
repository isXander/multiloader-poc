plugins {
    `java-library`
    id("dev.isxander.mtk.modrepos") version "0.1.0"
}

repositories {
    mavenLocal()
    fabricMC()
    neoForged()
}

dependencies {
    // common
    implementation("dev.isxander:testmod:1.0.0:universal")

}