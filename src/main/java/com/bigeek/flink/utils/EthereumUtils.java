package com.bigeek.flink.utils;

import okhttp3.OkHttpClient;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.Web3jService;
import org.web3j.protocol.http.HttpService;
import org.web3j.protocol.ipc.UnixIpcService;
import org.web3j.protocol.ipc.WindowsIpcService;

import java.util.concurrent.TimeUnit;

import static org.apache.commons.lang3.StringUtils.isEmpty;

/**
 * Utils class .
 */
public class EthereumUtils {

	public static OkHttpClient createOkHttpClient(Long timeoutSeconds) {
		OkHttpClient.Builder builder = new OkHttpClient.Builder();
		if (timeoutSeconds != null) {
			builder.connectTimeout(timeoutSeconds, TimeUnit.SECONDS);
			builder.readTimeout(timeoutSeconds, TimeUnit.SECONDS);
			builder.writeTimeout(timeoutSeconds, TimeUnit.SECONDS);
		}
		return builder.build();
	}

	public static Web3j generateClient(String clientAddress, Long timeoutSeconds) {

		if (isEmpty(clientAddress)) {
			throw new IllegalArgumentException("You have to define client address, use constructor or environment variable 'web3j.clientAddress'");
		}

		Web3jService web3jService;
		if (clientAddress.startsWith("http")) {
			web3jService = new HttpService(clientAddress, createOkHttpClient(timeoutSeconds), false);
		} else if (System.getProperty("os.name").toLowerCase().startsWith("win")) {
			web3jService = new WindowsIpcService(clientAddress);
		} else {
			web3jService = new UnixIpcService(clientAddress);
		}
		return Web3j.build(web3jService);
	}

}
