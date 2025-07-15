package com.ehcache.config;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ehcache")
public class CacheProperties {
    private boolean diskEnabled;
    private String diskPath;

    public boolean isDiskEnabled() { return diskEnabled; }
    public void setDiskEnabled(boolean diskEnabled) { this.diskEnabled = diskEnabled; }

    public String getDiskPath() { return diskPath; }
    public void setDiskPath(String diskPath) { this.diskPath = diskPath; }
}
