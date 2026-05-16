plugins {
    `java-library`
    id("net.fabricmc.fabric-loom") version "1.17.0-alpha.8"
    id("net.neoforged.gradle.userdev") version "7.1.27"
    `maven-publish`
}

group = "dev.isxander"
version = "1.0.4"

val fabric by sourceSets.registering
val neoforge by sourceSets.registering

java {
    withSourcesJar()

    registerFeature("fabric") {
        usingSourceSet(fabric.get())
        withSourcesJar()
    }
    registerFeature("neoforge") {
        usingSourceSet(neoforge.get())
        withSourcesJar()
    }

    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

loom.runConfigs {
    configureEach {
        sourceSet = fabric.name
    }

    register("fabricClient") {
        client()
    }
}

runs {
    configureEach {
        modSources {
            add(neoforge.get())
            primary = neoforge.get()
        }
    }

    register("neoforgeClient") {
        runType("client")
    }
}

jarJar.forFeature("fabric")
jarJar.forFeature("neoforge")

// These configurations are shared between all source sets
// They should not be used for mod-like dependencies where different variants
// would be required across sourcesets
val commonCompileOnly by configurations.registering
val commonRuntimeOnly by configurations.registering
val commonImplementation by configurations.registering
val commonApi by configurations.registering
val commonCompileOnlyApi by configurations.registering
val commonAnnotationProcessor by configurations.registering

// Share the common source set's sources/resources with loader source sets.
// Reference the source set directly — routing through resolvable configurations
// in the same project yields an empty resolution because no incoming dependency exists.
val commonJava = sourceSets.main.map { it.java }
val commonResources = sourceSets.main.map { it.resources }

configurations {
    compileOnly { extendsFrom(commonCompileOnly) }
    runtimeOnly { extendsFrom(commonRuntimeOnly) }
    implementation { extendsFrom(commonImplementation) }
    api { extendsFrom(commonApi) }
    compileOnlyApi { extendsFrom(commonCompileOnlyApi) }
    annotationProcessor { extendsFrom(commonAnnotationProcessor) }

    "fabricCompileOnly" { extendsFrom(commonCompileOnly) }
    "fabricRuntimeOnly" { extendsFrom(commonRuntimeOnly) }
    "fabricImplementation" { extendsFrom(commonImplementation) }
    "fabricApi" { extendsFrom(commonApi) }
    "fabricCompileOnlyApi" { extendsFrom(commonCompileOnlyApi) }
    "fabricAnnotationProcessor" { extendsFrom(commonAnnotationProcessor) }
    afterEvaluate {
        "fabricCompileClasspath" { extendsFrom(named("minecraftNamedCompile")) }
        "fabricRuntimeClasspath" { extendsFrom(named("minecraftNamedRuntime")) }
    }

    "neoforgeCompileOnly" { extendsFrom(commonCompileOnly) }
    "neoforgeRuntimeOnly" { extendsFrom(commonRuntimeOnly) }
    "neoforgeImplementation" { extendsFrom(commonImplementation) }
    "neoforgeApi" { extendsFrom(commonApi) }
    "neoforgeCompileOnlyApi" { extendsFrom(commonCompileOnlyApi) }
    "neoforgeAnnotationProcessor" { extendsFrom(commonAnnotationProcessor) }
}

dependencies {
    minecraft("com.mojang:minecraft:26.1.2")
    // need fabric loader in main sourceset to compile against Mixin.
    compileOnly("net.fabricmc:fabric-loader:0.19.2")

    // example common
    commonImplementation("net.fabricmc:mapping-io:0.8.0")

    "fabricImplementation"("net.fabricmc:fabric-loader:0.19.2")
    "fabricImplementation"("net.fabricmc.fabric-api:fabric-api:0.149.0+26.1.2")
    "fabricCompileOnly"(sourceSets.main.get().output)

    "neoforgeImplementation"("net.neoforged:neoforge:26.1.2.50-beta")
    "neoforgeCompileOnly"(sourceSets.main.get().output)
}

// include common sourceset in loader-specific compilation and resource processing
// this allows for compile-time guarantees when NeoForge patches a vanilla method signature like a bitch
tasks.named<JavaCompile>("compileFabricJava") {
    dependsOn(commonJava)
    source(commonJava)
}
tasks.named<Jar>("fabricSourcesJar") {
    dependsOn(commonJava)
    from(commonJava)
}
tasks.named<JavaCompile>("compileNeoforgeJava") {
    dependsOn(commonJava)
    source(commonJava)
}
tasks.named<Jar>("neoforgeSourcesJar") {
    dependsOn(commonJava)
    from(commonJava)
}
tasks.named<ProcessResources>("processFabricResources") {
    dependsOn(commonResources)
    from(commonResources)
}
tasks.named<ProcessResources>("processNeoforgeResources") {
    dependsOn(commonResources)
    from(commonResources)
}

// Create a universal jar that includes both loaders
val universalJar by tasks.registering(Jar::class) {
    group = "build"

    archiveClassifier = "universal"
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from(sourceSets.main.get().output)
    from(fabric.get().output)
    from(neoforge.get().output)
}
val universalSourcesJar by tasks.registering(Jar::class) {
    group = "build"

    archiveClassifier = "universal-sources"
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from(sourceSets.main.get().allSource)
    from(fabric.get().allSource)
    from(neoforge.get().allSource)
}
// Ensure the jars are generated on `build`
tasks.assemble {
    dependsOn("fabricJar", "neoforgeJar", universalJar, universalSourcesJar)
}

val modLoaderAttribute = Attribute.of("io.github.mcgradleconventions.loader", String::class.java)

fun ConfigurationContainer.configureLoaderVariant(
    sourceSet: SourceSet,
    action: Action<in Configuration>,
) {
    listOf(
        sourceSet.apiElementsConfigurationName,
        sourceSet.runtimeElementsConfigurationName,
        sourceSet.sourcesElementsConfigurationName,
        //sourceSet.javadocElementsConfigurationName,
    ).forEach { configurationName ->
        named(configurationName, action)
    }
}

configurations {
    configureLoaderVariant(sourceSets.main.get()) {
        attributes {
            attribute(modLoaderAttribute, "common")
        }
        outgoing.capability(provider { "${project.group}:${project.name}:${project.version}" })
        outgoing.capability(provider { "${project.group}:${project.name}-common:${project.version}" })
    }
    configureLoaderVariant(fabric.get()) {
        attributes {
            attribute(modLoaderAttribute, "fabric")
        }
        // fabric capability defined by the feature
        outgoing.capability(provider { "${project.group}:${project.name}:${project.version}" })
    }
    configureLoaderVariant(neoforge.get()) {
        attributes {
            attribute(modLoaderAttribute, "neoforge")
        }
        // neoforge capability defined by the feature
        outgoing.capability(provider { "${project.group}:${project.name}:${project.version}" })
    }
}

publishing {
    repositories {
        mavenLocal()
    }
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
}
