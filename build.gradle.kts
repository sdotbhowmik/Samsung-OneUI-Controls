plugins {
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
}

// OneDrive's sync filter holds locks on files inside app/build and makes
// Gradle's incremental tasks fail on Windows. When the project lives inside
// OneDrive, move build outputs to a local folder instead.
val oneDriveBuildRoot = if (rootDir.invariantSeparatorsPath.contains("OneDrive", ignoreCase = true)) {
    File(System.getProperty("user.home"), ".virtualkeys-build")
} else {
    null
}

if (oneDriveBuildRoot != null) {
    allprojects {
        val relativePath = if (path == ":") "root" else path.removePrefix(":").replace(':', '/')
        layout.buildDirectory.set(File(oneDriveBuildRoot, relativePath))
    }
}
