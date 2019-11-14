package io.miscellanea.etcd;

/**
 * Interface implemented by classes that load {@code EtcdConfigSource}'s internal
 * configuration.
 *
 * @author Jason Hallford
 */
interface EtcdConfig {

    /**
     * Gets the etcd server's host name.
     *
     * @return The host name or {@code null} if not defined.
     */
    String getHost();

    /**
     * Gets the etcd server's TCP port.
     *
     * @return The TCP port or {@code null} if not defined.
     */
    Integer getPort();

    /**
     * Gets the user name to send to etcd for authentication.
     *
     * @return The user name or {@code null} if not defined.
     */
    String getUser();

    /**
     * Gets the password to send with the user name.
     *
     * @return The password or {@code null} if not defined.
     */
    String getPassword();

    /**
     * If {@code true}, then the configuration source will watch for
     * changes to any previously read properties.
     *
     * @return The watching status or {@code null} if not defined.
     */
    Boolean isWatching();

    /**
     * Gets the configuration source's ordinal.
     *
     * @return The configuration source's ordinal.
     */
    Integer getOrdinal();
}
