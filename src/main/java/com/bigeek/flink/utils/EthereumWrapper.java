package com.bigeek.flink.utils;

import org.web3j.protocol.Web3j;

/**
 * Wrapper for web3j client
 */
public class EthereumWrapper {

    private transient static Web3j web3jInstance;

    /**
     * Get instance with parameters.
     *
     * @param address
     * @param timeout
     * @return web3j client
     */
    public static Web3j configureInstance(String address, Long timeout) {
        web3jInstance = EthereumUtils.generateClient(address, timeout);
        return web3jInstance;
    }

    /**
     * Get the initialized instance.
     *
     * @return web3j client
     */
    public static Web3j getInstance() {
        if (web3jInstance == null) {
            throw new IllegalStateException("Need configure first");
        }
        return web3jInstance;
    }

}
