package io.miscellanea.etcd;

/**
 * Interface implemented by classes that load {@code EtcdConfigSource}'s internal
 * configuration.
 *
 * @author Jason Hallford
 */
interface EtcdConfiguration {

    /**
     * Gets the etcd server's host name.
     *
     * @return The host name.
     */
    String getHost();

    /**
     * Gets the etcd server's TCP port.
     *
     * @return The TCP port.
     */
    int getPort();

    /**
     * Gets the user name to send to etcd for authentication.
     *
     * @return The user name.
     */
    String getUser();

    /**
     * Gets the password to send with the user name.
     *
     * @return The password.
     */
    String getPassword();

    /**
     * If {@code true}, then the configuration source will watch for
     * changes to any previously read properties.
     *
     * @return The watching status.
     */
    boolean isWatching();
}
