package org.fenixedu.treasury.services.payments.sibspay.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Object that defines the customer device information.
 */
@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen",
        date = "2023-08-30T20:20:13.375686+01:00[Europe/Lisbon]")

public class SibsPayDeviceinfo {
    @JsonProperty("browserAcceptHeader")
    private String browserAcceptHeader = null;

    @JsonProperty("browserJavaEnabled")
    private String browserJavaEnabled = null;

    @JsonProperty("browserLanguage")
    private String browserLanguage = null;

    @JsonProperty("browserColorDepth")
    private String browserColorDepth = null;

    @JsonProperty("browserScreenHeight")
    private String browserScreenHeight = null;

    @JsonProperty("browserScreenWidth")
    private String browserScreenWidth = null;

    @JsonProperty("browserTZ")
    private String browserTZ = null;

    @JsonProperty("browserUserAgent")
    private String browserUserAgent = null;

    @JsonProperty("systemFamily")
    private String systemFamily = null;

    @JsonProperty("systemVersion")
    private String systemVersion = null;

    @JsonProperty("systemArchitecture")
    private String systemArchitecture = null;

    @JsonProperty("deviceManufacturer")
    private String deviceManufacturer = null;

    @JsonProperty("deviceModel")
    private String deviceModel = null;

    @JsonProperty("deviceID")
    private String deviceID = null;

    @JsonProperty("applicationName")
    private String applicationName = null;

    @JsonProperty("applicationVersion")
    private String applicationVersion = null;

    @JsonProperty("geoLocalization")
    private String geoLocalization = null;

    @JsonProperty("ipAddress")
    private String ipAddress = null;

    public SibsPayDeviceinfo browserAcceptHeader(String browserAcceptHeader) {
        this.browserAcceptHeader = browserAcceptHeader;
        return this;
    }

    /**
     * Get browserAcceptHeader
     * 
     * @return browserAcceptHeader
     **/

    public String getBrowserAcceptHeader() {
        return browserAcceptHeader;
    }

    public void setBrowserAcceptHeader(String browserAcceptHeader) {
        this.browserAcceptHeader = browserAcceptHeader;
    }

    public SibsPayDeviceinfo browserJavaEnabled(String browserJavaEnabled) {
        this.browserJavaEnabled = browserJavaEnabled;
        return this;
    }

    /**
     * Get browserJavaEnabled
     * 
     * @return browserJavaEnabled
     **/

    public String getBrowserJavaEnabled() {
        return browserJavaEnabled;
    }

    public void setBrowserJavaEnabled(String browserJavaEnabled) {
        this.browserJavaEnabled = browserJavaEnabled;
    }

    public SibsPayDeviceinfo browserLanguage(String browserLanguage) {
        this.browserLanguage = browserLanguage;
        return this;
    }

    /**
     * Get browserLanguage
     * 
     * @return browserLanguage
     **/

    public String getBrowserLanguage() {
        return browserLanguage;
    }

    public void setBrowserLanguage(String browserLanguage) {
        this.browserLanguage = browserLanguage;
    }

    public SibsPayDeviceinfo browserColorDepth(String browserColorDepth) {
        this.browserColorDepth = browserColorDepth;
        return this;
    }

    /**
     * Get browserColorDepth
     * 
     * @return browserColorDepth
     **/

    public String getBrowserColorDepth() {
        return browserColorDepth;
    }

    public void setBrowserColorDepth(String browserColorDepth) {
        this.browserColorDepth = browserColorDepth;
    }

    public SibsPayDeviceinfo browserScreenHeight(String browserScreenHeight) {
        this.browserScreenHeight = browserScreenHeight;
        return this;
    }

    /**
     * Get browserScreenHeight
     * 
     * @return browserScreenHeight
     **/

    public String getBrowserScreenHeight() {
        return browserScreenHeight;
    }

    public void setBrowserScreenHeight(String browserScreenHeight) {
        this.browserScreenHeight = browserScreenHeight;
    }

    public SibsPayDeviceinfo browserScreenWidth(String browserScreenWidth) {
        this.browserScreenWidth = browserScreenWidth;
        return this;
    }

    /**
     * Get browserScreenWidth
     * 
     * @return browserScreenWidth
     **/

    public String getBrowserScreenWidth() {
        return browserScreenWidth;
    }

    public void setBrowserScreenWidth(String browserScreenWidth) {
        this.browserScreenWidth = browserScreenWidth;
    }

    public SibsPayDeviceinfo browserTZ(String browserTZ) {
        this.browserTZ = browserTZ;
        return this;
    }

    /**
     * Get browserTZ
     * 
     * @return browserTZ
     **/

    public String getBrowserTZ() {
        return browserTZ;
    }

    public void setBrowserTZ(String browserTZ) {
        this.browserTZ = browserTZ;
    }

    public SibsPayDeviceinfo browserUserAgent(String browserUserAgent) {
        this.browserUserAgent = browserUserAgent;
        return this;
    }

    /**
     * Get browserUserAgent
     * 
     * @return browserUserAgent
     **/

    public String getBrowserUserAgent() {
        return browserUserAgent;
    }

    public void setBrowserUserAgent(String browserUserAgent) {
        this.browserUserAgent = browserUserAgent;
    }

    public SibsPayDeviceinfo systemFamily(String systemFamily) {
        this.systemFamily = systemFamily;
        return this;
    }

    /**
     * Get systemFamily
     * 
     * @return systemFamily
     **/

    public String getSystemFamily() {
        return systemFamily;
    }

    public void setSystemFamily(String systemFamily) {
        this.systemFamily = systemFamily;
    }

    public SibsPayDeviceinfo systemVersion(String systemVersion) {
        this.systemVersion = systemVersion;
        return this;
    }

    /**
     * Get systemVersion
     * 
     * @return systemVersion
     **/

    public String getSystemVersion() {
        return systemVersion;
    }

    public void setSystemVersion(String systemVersion) {
        this.systemVersion = systemVersion;
    }

    public SibsPayDeviceinfo systemArchitecture(String systemArchitecture) {
        this.systemArchitecture = systemArchitecture;
        return this;
    }

    /**
     * Get systemArchitecture
     * 
     * @return systemArchitecture
     **/

    public String getSystemArchitecture() {
        return systemArchitecture;
    }

    public void setSystemArchitecture(String systemArchitecture) {
        this.systemArchitecture = systemArchitecture;
    }

    public SibsPayDeviceinfo deviceManufacturer(String deviceManufacturer) {
        this.deviceManufacturer = deviceManufacturer;
        return this;
    }

    /**
     * Get deviceManufacturer
     * 
     * @return deviceManufacturer
     **/

    public String getDeviceManufacturer() {
        return deviceManufacturer;
    }

    public void setDeviceManufacturer(String deviceManufacturer) {
        this.deviceManufacturer = deviceManufacturer;
    }

    public SibsPayDeviceinfo deviceModel(String deviceModel) {
        this.deviceModel = deviceModel;
        return this;
    }

    /**
     * Get deviceModel
     * 
     * @return deviceModel
     **/

    public String getDeviceModel() {
        return deviceModel;
    }

    public void setDeviceModel(String deviceModel) {
        this.deviceModel = deviceModel;
    }

    public SibsPayDeviceinfo deviceID(String deviceID) {
        this.deviceID = deviceID;
        return this;
    }

    /**
     * Get deviceID
     * 
     * @return deviceID
     **/

    public String getDeviceID() {
        return deviceID;
    }

    public void setDeviceID(String deviceID) {
        this.deviceID = deviceID;
    }

    public SibsPayDeviceinfo applicationName(String applicationName) {
        this.applicationName = applicationName;
        return this;
    }

    /**
     * Get applicationName
     * 
     * @return applicationName
     **/

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public SibsPayDeviceinfo applicationVersion(String applicationVersion) {
        this.applicationVersion = applicationVersion;
        return this;
    }

    /**
     * Get applicationVersion
     * 
     * @return applicationVersion
     **/

    public String getApplicationVersion() {
        return applicationVersion;
    }

    public void setApplicationVersion(String applicationVersion) {
        this.applicationVersion = applicationVersion;
    }

    public SibsPayDeviceinfo geoLocalization(String geoLocalization) {
        this.geoLocalization = geoLocalization;
        return this;
    }

    /**
     * Get geoLocalization
     * 
     * @return geoLocalization
     **/

    public String getGeoLocalization() {
        return geoLocalization;
    }

    public void setGeoLocalization(String geoLocalization) {
        this.geoLocalization = geoLocalization;
    }

    public SibsPayDeviceinfo ipAddress(String ipAddress) {
        this.ipAddress = ipAddress;
        return this;
    }

    /**
     * Get ipAddress
     * 
     * @return ipAddress
     **/

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SibsPayDeviceinfo deviceinfo = (SibsPayDeviceinfo) o;
        return Objects.equals(this.browserAcceptHeader, deviceinfo.browserAcceptHeader)
                && Objects.equals(this.browserJavaEnabled, deviceinfo.browserJavaEnabled)
                && Objects.equals(this.browserLanguage, deviceinfo.browserLanguage)
                && Objects.equals(this.browserColorDepth, deviceinfo.browserColorDepth)
                && Objects.equals(this.browserScreenHeight, deviceinfo.browserScreenHeight)
                && Objects.equals(this.browserScreenWidth, deviceinfo.browserScreenWidth)
                && Objects.equals(this.browserTZ, deviceinfo.browserTZ)
                && Objects.equals(this.browserUserAgent, deviceinfo.browserUserAgent)
                && Objects.equals(this.systemFamily, deviceinfo.systemFamily)
                && Objects.equals(this.systemVersion, deviceinfo.systemVersion)
                && Objects.equals(this.systemArchitecture, deviceinfo.systemArchitecture)
                && Objects.equals(this.deviceManufacturer, deviceinfo.deviceManufacturer)
                && Objects.equals(this.deviceModel, deviceinfo.deviceModel) && Objects.equals(this.deviceID, deviceinfo.deviceID)
                && Objects.equals(this.applicationName, deviceinfo.applicationName)
                && Objects.equals(this.applicationVersion, deviceinfo.applicationVersion)
                && Objects.equals(this.geoLocalization, deviceinfo.geoLocalization)
                && Objects.equals(this.ipAddress, deviceinfo.ipAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(browserAcceptHeader, browserJavaEnabled, browserLanguage, browserColorDepth, browserScreenHeight,
                browserScreenWidth, browserTZ, browserUserAgent, systemFamily, systemVersion, systemArchitecture,
                deviceManufacturer, deviceModel, deviceID, applicationName, applicationVersion, geoLocalization, ipAddress);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Deviceinfo {\n");

        sb.append("    browserAcceptHeader: ").append(toIndentedString(browserAcceptHeader)).append("\n");
        sb.append("    browserJavaEnabled: ").append(toIndentedString(browserJavaEnabled)).append("\n");
        sb.append("    browserLanguage: ").append(toIndentedString(browserLanguage)).append("\n");
        sb.append("    browserColorDepth: ").append(toIndentedString(browserColorDepth)).append("\n");
        sb.append("    browserScreenHeight: ").append(toIndentedString(browserScreenHeight)).append("\n");
        sb.append("    browserScreenWidth: ").append(toIndentedString(browserScreenWidth)).append("\n");
        sb.append("    browserTZ: ").append(toIndentedString(browserTZ)).append("\n");
        sb.append("    browserUserAgent: ").append(toIndentedString(browserUserAgent)).append("\n");
        sb.append("    systemFamily: ").append(toIndentedString(systemFamily)).append("\n");
        sb.append("    systemVersion: ").append(toIndentedString(systemVersion)).append("\n");
        sb.append("    systemArchitecture: ").append(toIndentedString(systemArchitecture)).append("\n");
        sb.append("    deviceManufacturer: ").append(toIndentedString(deviceManufacturer)).append("\n");
        sb.append("    deviceModel: ").append(toIndentedString(deviceModel)).append("\n");
        sb.append("    deviceID: ").append(toIndentedString(deviceID)).append("\n");
        sb.append("    applicationName: ").append(toIndentedString(applicationName)).append("\n");
        sb.append("    applicationVersion: ").append(toIndentedString(applicationVersion)).append("\n");
        sb.append("    geoLocalization: ").append(toIndentedString(geoLocalization)).append("\n");
        sb.append("    ipAddress: ").append(toIndentedString(ipAddress)).append("\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     */
    private String toIndentedString(java.lang.Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }
}
