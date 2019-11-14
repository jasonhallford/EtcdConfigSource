**EtcdConfigSource** adds basic [etcd](https://github.com/etcd-io/etcd) support to the Apache DeltaSpike [configuration
mechanism](https://deltaspike.apache.org/documentation/configuration.html). In particular, it has not been tested
against a cluster configuration. That may come at a later time.

# Building
You may build this project using [Maven 3.5+](https://maven.apache.org/) and OpenJDK 8. I use binaries provided by
the [AdoptOpenJDK](https://adoptopenjdk.net/) project.

First, you'll need to clone the repo:
```pwsh
git clone https://github.com/jasonhallford/EtcdConfigSource.git
```

Then build with Maven:
```pwsh
mvn install -DskipITs
```

Here, <code>-DskipITs</code> is used to disable integration tests. You may run them, of course, but you'll need to 
configure a local **etcd** instance first. 

If you prefer an IDE, I recommend [IntelliJ IDEA](https://www.jetbrains.com/idea/)'s Community Edition, although any
idea with Maven support should suffice.