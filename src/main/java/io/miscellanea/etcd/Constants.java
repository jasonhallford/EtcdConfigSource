package io.miscellanea.etcd;

final class Constants {
    // Property names
    public static final String HOST_PROP = "etcd.endpoint.host";
    public static final String PORT_PROP = "etcd.endpoint.port";
    public static final String USER_PROP = "etcd.endpoint.user";
    public static final String PASSWORD_PROP = "etcd.endpoint.password";
    public static final String ORDINAL_PROP = "etcd.cs.ordinal";
    public static final String WATCHING_PROP = "etcd.cs.watch";
    public static final String CONFIG_URL_PROP = "etc.cs.configUrl";

    // Default values
    public static final Integer DEFAULT_PORT = 2379;
    public static final Integer DEFAULT_ORDINAL = 1000;
}
