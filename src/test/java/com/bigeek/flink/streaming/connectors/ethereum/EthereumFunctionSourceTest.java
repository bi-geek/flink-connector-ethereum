package com.bigeek.flink.streaming.connectors.ethereum;

import io.github.ganchix.ganache.GanacheContainer;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.source.SourceFunction;
import org.apache.flink.streaming.api.watermark.Watermark;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.tx.Transfer;
import org.web3j.utils.Convert;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

public class EthereumFunctionSourceTest {

	@Rule
	public GanacheContainer ganacheContainer = new GanacheContainer();

	private EthereumFunctionSource ethereumFunctionSource;

	@Before
	public void setUpClass() throws Exception {
		List<Credentials> credentials = ganacheContainer.getCredentials();
		credentials.forEach(credential -> {
			try {
				Transfer.sendFunds(ganacheContainer.getWeb3j(), credential,
						credential.getAddress(), BigDecimal.ONE, Convert.Unit.ETHER).send();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
	}


	@Test
	public void testOpen() throws IOException {
		ethereumFunctionSource = new EthereumFunctionSource(generateClientAddress(), 1);
		ethereumFunctionSource.open(new Configuration());
		ethereumFunctionSource.cancel();
		ethereumFunctionSource.close();
	}

	@NotNull
	private String generateClientAddress() {
		return "http://" + ganacheContainer.getContainerIpAddress() + ":" + ganacheContainer.getFirstMappedPort() + "/";
	}
}
