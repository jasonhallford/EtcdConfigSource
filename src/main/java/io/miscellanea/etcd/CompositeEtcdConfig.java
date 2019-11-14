package io.miscellanea.etcd;

import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * {@code EtcdConfig} implementation using the Composite pattern.
 *
 * @author Jason Hallford
 */
class CompositeEtcdConfig implements EtcdConfig {
    // Fields
    private static final Logger LOGGER = LoggerFactory.getLogger(CompositeEtcdConfig.class);

    private List<EtcdConfig> configs = new ArrayList<>();

    // Constructors
    public CompositeEtcdConfig(){
        LOGGER.debug("Adding default environment configuration.");
        configs.add(new EnvironmentEtcdConfig());

        if(!Strings.isNullOrEmpty(System.getProperty(Constants.CONFIG_URL_PROP))){
            try{
                URL url = new URL(System.getProperty(Constants.CONFIG_URL_PROP));
                LOGGER.debug("Adding URL configuration for {}.",url.toString());
                configs.add(new UrlEtcdConfig(url));
            } catch (MalformedURLException e) {
                LOGGER.warn("Unable to initialize URL configuration: {}", e.getMessage());
            }
        }
    }

    public CompositeEtcdConfig(EtcdConfig...configs){
        if(configs != null){
            this.configs.addAll(Arrays.asList(configs));
            LOGGER.debug("Added {} config(s) to composite.",configs.length);
        }
    }

    // EtcdConfig
    @Override
    public String getHost() {
        String host = null;

        for (EtcdConfig config : this.configs) {
            host = config.getHost();
            if(host != null){
                break;
            }
        }

        LOGGER.debug("host = {}",host);

        return host;
    }

    @Override
    public Integer getPort() {
        Integer port = null;

        for (EtcdConfig config : this.configs) {
            port = config.getPort();
            if(port != null){
                break;
            }
        }

        if( port == null ){
            LOGGER.debug("Port is not defined; using default.");
            port = Constants.DEFAULT_PORT;
        }

        LOGGER.debug("port = {}",port);

        return port;
    }

    @Override
    public String getUser() {
        String user = null;

        for (EtcdConfig config : this.configs) {
            user = config.getUser();
            if(user != null){
                break;
            }
        }

        LOGGER.debug("user = {}",user);

        return user;
    }

    @Override
    public String getPassword() {
        String password = null;

        for (EtcdConfig config : this.configs) {
            password = config.getPassword();
            if(password != null){
                break;
            }
        }

        LOGGER.debug("password = {}",password);

        return password;
    }

    @Override
    public Boolean isWatching() {
        Boolean watching = null;

        for (EtcdConfig config : this.configs) {
            watching = config.isWatching();
            if(watching != null){
                break;
            }
        }

        if(watching == null){
            watching = Boolean.FALSE;
        }

        LOGGER.debug("watching = {}",watching);

        return watching;
    }

    @Override
    public Integer getOrdinal() {
        Integer ordinal = null;

        for (EtcdConfig config : this.configs) {
            ordinal = config.getOrdinal();
            if(ordinal != null){
                break;
            }
        }

        if(ordinal == null){
            LOGGER.debug("Using default ordinal");
            ordinal = Constants.DEFAULT_ORDINAL;
        }

        return ordinal;
    }
}
