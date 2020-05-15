package io.miscellanea.etcd;

import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Properties;

/**
 * An {@code EtcdConfig} implementation that reads configuration, stored as a .properties file, from
 * a URL.
 *
 * @author Jason Hallford
 */
class UrlEtcdConfig implements EtcdConfig {
  @FunctionalInterface
  interface InputStreamSupplier {
    InputStream get() throws IOException;
  }

  // Fields
  private static final Logger LOGGER = LoggerFactory.getLogger(UrlEtcdConfig.class);

  private final Properties props = new Properties();

  // Constructors
  public UrlEtcdConfig(final URL configUrl) {
    if (configUrl == null) {
      throw new IllegalArgumentException("configUrl must not be null.");
    }
    this.initializeProperties(() -> configUrl.openStream());
  }

  public UrlEtcdConfig(final InputStream inputStream) {
    if (inputStream == null) {
      throw new IllegalArgumentException("inputStream must not be null.");
    }
    this.initializeProperties(
        () -> {
          return inputStream;
        });
  }

  // Private methods
  private void initializeProperties(InputStreamSupplier stream) {
    try (InputStream in = stream.get()) {
      this.props.load(in);
      LOGGER.debug("Successfully loaded properties.");
    } catch (IOException e) {
      LOGGER.warn(
          "Unable to open input stream to URL: {}. URL configuration is disabled.", e.getMessage());
    }
  }

  // EtcdConfig
  @Override
  public String getHost() {
    return this.props.getProperty(Constants.HOST_PROP);
  }

  @Override
  public Integer getPort() {
    Integer port = null;

    if (this.props.containsKey(Constants.PORT_PROP)) {
      String strPort = this.props.getProperty(Constants.PORT_PROP);
      try {
        port = Integer.parseInt(strPort);
      } catch (Exception e) {
        LOGGER.warn("Unable to convert ''{}'' to an integer; port == null.", strPort);
      }
    }

    return port;
  }

  @Override
  public List<String> getClusterMembers() {
    return null;
  }

  @Override
  public String getUser() {
    return this.props.getProperty(Constants.USER_PROP);
  }

  @Override
  public String getPassword() {
    return this.props.getProperty(Constants.PASSWORD_PROP);
  }

  @Override
  public Boolean isWatching() {
    Boolean watching = null;

    if (this.props.containsKey(Constants.WATCHING_PROP)
        && !Strings.isNullOrEmpty(this.props.getProperty(Constants.WATCHING_PROP))) {
      watching = Boolean.parseBoolean(this.props.getProperty(Constants.WATCHING_PROP));

      LOGGER.debug(
          "Converted ''{}'' to ''{}''.", this.props.getProperty(Constants.WATCHING_PROP), watching);
    }

    return watching;
  }

  @Override
  public Integer getOrdinal() {
    Integer ordinal = null;

    if (this.props.containsKey(Constants.ORDINAL_PROP)) {
      String strOrdinal = this.props.getProperty(Constants.ORDINAL_PROP);
      try {
        ordinal = Integer.parseInt(strOrdinal);
      } catch (Exception e) {
        LOGGER.warn("Unable to convert ''{}'' to an integer; ordinal == null.", strOrdinal);
      }
    }

    return ordinal;
  }

  @Override
  public String getKeyPrefix(){
    String prefix = Constants.DEFAULT_KEY_PREFIX;

    if(this.props.containsKey(Constants.KEY_PREFIX)){
      prefix = this.props.getProperty(Constants.KEY_PREFIX);
    }

    return prefix;
  }
}
