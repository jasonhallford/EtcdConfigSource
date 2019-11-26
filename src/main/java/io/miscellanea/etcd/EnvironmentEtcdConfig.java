package io.miscellanea.etcd;

import com.google.common.base.Strings;
import org.apache.deltaspike.core.api.config.ConfigResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Manages the configuration source's own, internal configuration.
 *
 * @author Jason Hallford
 */
class EnvironmentEtcdConfig implements EtcdConfig {
    // Fields
    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentEtcdConfig.class);

    private String host;
    private Integer port;
    private String user;
    private String password;
    private Boolean watching;
    private Integer ordinal;
    private List<String> members;

    // Constructors
    public EnvironmentEtcdConfig() {
        this.host = System.getProperty(Constants.HOST_PROP);
        this.port = this.resolvePort();
        this.members = this.resolveClusterMembers();
        this.watching = this.resolveWatching();
        this.user = System.getProperty(Constants.USER_PROP);
        this.password = System.getProperty(Constants.PASSWORD_PROP);
        this.ordinal = this.resolveOrdinal();

        LOGGER.debug("etcd host = {}, etcd port = {}, etcd user = {}, etcd password = {}", this.host, this.port, this.user, password);
    }

    // Properties
    @Override
    public String getHost() {
        return host;
    }

    @Override
    public Integer getPort() {
        return port;
    }

    @Override
    public List<String> getClusterMembers() {
        return members;
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
    public Boolean isWatching() {
        return watching;
    }

    @Override
    public Integer getOrdinal() {
        return ordinal;
    }

    // Private methods
    private Integer resolvePort() {
        Integer port = null;
        String strPort = System.getProperty(Constants.PORT_PROP);

        if (!Strings.isNullOrEmpty(strPort)) {
            try {
                port = Integer.parseInt(strPort);
            } catch (Exception e) {
                LOGGER.warn("Unable to convert configured value to an integer ({}); using default value ({}).", e.getMessage(), port);
            }
        } else {
            LOGGER.info("Property {} is not defined.", Constants.PORT_PROP);
        }

        return port;
    }

    private Integer resolveOrdinal() {
        Integer ordinal = null;
        String strOrdinal = System.getProperty(Constants.ORDINAL_PROP);

        if (!Strings.isNullOrEmpty(strOrdinal)) {
            try {
                ordinal = Integer.parseInt(strOrdinal);
            } catch (Exception e) {
                LOGGER.warn("Unable to convert configured value to an integer ({}); using default value ({}).", e.getMessage(), ordinal);
            }
        } else {
            LOGGER.info("Property {} is not defined.", Constants.ORDINAL_PROP);
        }

        return ordinal;
    }

    private List<String> resolveClusterMembers() {
        String members = System.getProperty(Constants.MEMBERS_PROP);

        List<String> memberList = null;
        if (members != null) {
            memberList = Utils.parseMembers(members);
        }

        return memberList;
    }

    private Boolean resolveWatching() {
        Boolean doWatch = null;

        String strWatch = System.getProperty(Constants.WATCHING_PROP);
        if (!Strings.isNullOrEmpty(strWatch)) {
            doWatch = Boolean.parseBoolean(strWatch);
        } else {
            LOGGER.debug("Property {} is not defined.", Constants.WATCHING_PROP);
        }

        LOGGER.info("doWatch = {}", doWatch);

        return doWatch;
    }
}
