plugins {
    agent.`common-conventions`
}

dependencies {
    compileOnly(files("../libraries/spark-common-1.10-SNAPSHOT.jar"))
    implementation(libs.async.profiler)
}