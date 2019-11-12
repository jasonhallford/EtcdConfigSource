package io.miscellanea.etcd;

import com.google.common.base.Strings;
import org.apache.deltaspike.core.api.config.ConfigResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the configuration source's own, internal configuration.
 *
 * @author Jason Hallford
 */
class EnvironmentEtcdConfiguration implements EtcdConfiguration {
    // Fields
    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentEtcdConfiguration.class);

    private String host;
    private int port;
    private String user;
    private String password;
    private boolean watching;

    // Constructors
    public EnvironmentEtcdConfiguration() {
        this.host = System.getProperty(Constants.ETCD_HOST_PROP);
        this.port = this.resolvePort();
        this.watching = this.resolveWatching();
        this.user = System.getProperty(Constants.ETCD_USER_PROP);
        this.password = System.getProperty(Constants.ETCD_PASSWORD_PROP);

        LOGGER.debug("etcd host = {}, etcd port = {}, etcd user = {}, etcd password = {}", this.host, this.port, this.user, password);
    }

    // Properties
    @Override
    public String getHost() {
        return host;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public String getUser() {
        return user;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public boolean isWatching() {
        return watching;
    }

    // Private methods
    private int resolvePort() {
        String strPort = System.getProperty(Constants.ETCD_PORT_PROP);
        int port = Constants.DEFAULT_PORT;

        if (!Strings.isNullOrEmpty(strPort)) {
            try {
                port = Integer.parseInt(strPort);
            } catch (Exception e) {
                LOGGER.warn("Unable to convert configured value to an integer ({}); using default value ({}).", e.getMessage(), port);
            }
        } else {
            LOGGER.info("Using default etcd port {}.", port);
        }

        return port;
    }

    private boolean resolveWatching() {
        boolean doWatch = false;

        String strWatch = ConfigResolver.getPropertyValue(Constants.ETCD_WATCHING_PROP);
        if (!Strings.isNullOrEmpty(strWatch)) {
            doWatch = Boolean.parseBoolean(strWatch);
        }

        LOGGER.info("Config source {} watch for property changes.", doWatch ? "will" : "will not");

        return doWatch;
    }
}
