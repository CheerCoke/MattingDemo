pluginManagement {
    repositories {
        maven {
            url = uri("https://storage.googleapis.com/r8-releases/raw")
        }
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }

        maven {
            isAllowInsecureProtocol = true
            url = uri("http://192.168.10.2:8889/repository/ddmh-android-public/")
        }
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven {
            isAllowInsecureProtocol = true
            url = uri("http://192.168.10.2:8889/repository/ddmh-android-public/")
        }
        google()
        mavenCentral()
    }
}

rootProject.name = "MattingDemo"
include(":app")
include(":lib_matting")
