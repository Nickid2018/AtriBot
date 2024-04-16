group = "io.github.nickid2018"
version = "1.0-SNAPSHOT"

repositories {
   maven("https://maven.nova-committee.cn/releases/")
}

dependencies {
   api(project(":atribot-backend-bridge"))
   implementation("cn.evole.onebot:OneBot-Client:0.4.0")
}