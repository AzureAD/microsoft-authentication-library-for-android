package com.microsoft.identity.client.configuration;

import com.google.gson.annotations.SerializedName;

public class HttpConfiguration {

    @SerializedName("read_timeout")
    private int mReadTimeout;

    @SerializedName("connect_timeout")
    private int mConnectTimeout;

    /**
     * Get the currently configured read timeout for the public client application
     *
     * @return int
     */
    public int getReadTimeout() {
        return this.mReadTimeout;
    }

    /**
     * Sets the read timeout for the public client application
     *
     * @param timeout
     */
    public void setReadTimeout(int timeout) {
        this.mReadTimeout = timeout;
    }

    /**
     * Gets the currently configured connect timeout for the public client application
     *
     * @return
     */
    public int getConnectTimeout() {
        return this.mConnectTimeout;
    }

    /**
     * Sets the connect timeout for the public client application
     *
     * @param timeout
     */
    public void setConnectTimeout(int timeout) {
        this.mConnectTimeout = timeout;
    }

}
