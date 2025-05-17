plugins {
    agent.`common-conventions`
    alias(libs.plugins.runpaper)
}

repositories {
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    compileOnly(libs.paper.api)
    api(project(":common"))
}


tasks {
    runServer {
        minecraftVersion("1.21.1")
    }
}