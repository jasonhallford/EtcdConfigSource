**EtcdConfigSource** adds basic [etcd](https://github.com/etcd-io/etcd) support to the Apache DeltaSpike [configuration
mechanism](https://deltaspike.apache.org/documentation/configuration.html). In particular, it has not been tested
against a cluster configuration or with TLS. Both may come at a later time.

# Building
You may build this project using [Maven 3.5+](https://maven.apache.org/) and OpenJDK 8. I use binaries provided by
the [AdoptOpenJDK](https://adoptopenjdk.net/) project.

First, you'll need to clone the repo:
```bash
$ git clone https://github.com/jasonhallford/EtcdConfigSource.git
```

Then build with Maven:
```bash
$ mvn install -DskipITs
```

Here, <code>-DskipITs</code> is used to disable integration tests. You may run them, of course, but you'll need to 
configure a local **etcd** instance first. 

If you prefer an IDE, I recommend [IntelliJ IDEA](https://www.jetbrains.com/idea/)'s Community Edition, although any
IDE with Maven support should suffice.

# Downloading
A pre-built binary is available from this site's [packages](https://github.com/jasonhallford/EtcdConfigSource/packages)
tab. You may download the JAR directly, or integrate Maven with [GitHub packages](https://help.github.com/en/github/managing-packages-with-github-packages/configuring-apache-maven-for-use-with-github-packages)
and then use the provided coordinates (hint: you want the latest version of <pre>io.miscellanea.EtcdConfigSource</pre>).

# Using
To use the **EtcdConfigSource**, simply place the JAR and its dependencies on the classpath. Integration with DeltaSpike is
achieved via the Java Service Loader Framework. With the exception of setting a few parameters (see below),
no additional configuration is required!

## Parameters
The configuration source makes use of parameters to control runtime behavior. They may be set in two ways
1. On the command line using <code>-D</code> (or any other means of invoking <code>System.setProperty()</code>)
2. Via a property file referenced using URL syntax.

 Property  | Type | Description
 --------- | ---- | -----------
 etcd.cs.configUrl | String | The URL for a .properties file containing the other properties listed in this table. For example, to reference a file named <pre>myEtcd.properties</pre> in <pre>/var/lib/etcd/</pre> you'd use the URL <pre>file://var/lib/etcd/myEtcd.properties</code>.
 etcd.cs.ordinal | Integer | The ordinal used to determine the configuration source's priority order. Defaults to 1000 if omitted. Please see the DeltaSpike [configuration mechanism](https://deltaspike.apache.org/documentation/configuration.html) page for more information.
 etcd.cs.watch | Boolean | If <pre>true</code>, then the configuration source will dynamically reload previously read etcd keys should they change. If <pre>false</code> (the default), then each key's value is only read once. 
 etcd.endpoint.host | String | The etcd host's DNS name or IP address.
 etcd.endpoint.password | String | The etcd user's password. Must be omitted if authentication is not required.
 etcd.endpoint.port | Integer | The etcd server's TCP port. Defaults to 2379 if omitted.
 etcd.endpoint.user | String | The name used to authenticate with the etcd server. Must be omitted if authentication in not required.
 
 Parameters may be specified on the command line *and* in a properties file. In that case, command line parameters take precedence, overriding any conflicting
 values in the file.
 