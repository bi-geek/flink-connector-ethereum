package com.bigeek.flink.batch.connectors.ethereum;

import io.github.ganchix.ganache.GanacheContainer;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.core.io.GenericInputSplit;
import org.apache.flink.core.io.InputSplit;
import org.apache.flink.core.io.InputSplitAssigner;
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
import java.math.BigInteger;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Tests class .
 */
public class EthereumInputSourceTest {

	@Rule
	public GanacheContainer ganacheContainer = new GanacheContainer();

	private EthereumInputSource ethereumInputSource;

	@Before
	public void setUpClass() {
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

	@Test(expected = IllegalArgumentException.class)
	public void testStartBlockIsHigherThanEndBlock() {
		ethereumInputSource = new EthereumInputSource(generateClientAddress(), 100, 5);
		ethereumInputSource.configure(new Configuration());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testStartBlockIsEqualEndBlock() {
		ethereumInputSource = new EthereumInputSource(generateClientAddress(), 100, 100);
		ethereumInputSource.configure(new Configuration());
	}

	@Test
	public void testInputSplitWithClientAddressNull() {
		ethereumInputSource = new EthereumInputSource(generateClientAddress(), 0, 5);
		ethereumInputSource.configure(new Configuration());
		GenericInputSplit[] inputSplits = ethereumInputSource.createInputSplits(0);
		assertEquals(inputSplits.length, 6);
		ethereumInputSource.close();

	}

	@Test
	public void testInputSplitWithAssigner() {
		ethereumInputSource = new EthereumInputSource(generateClientAddress(), 0, 5);
		ethereumInputSource.configure(new Configuration());
		GenericInputSplit[] inputSplits = ethereumInputSource.createInputSplits(0);
		InputSplitAssigner inputSplitAssigner = ethereumInputSource.getInputSplitAssigner(inputSplits);
		InputSplit nextInputSplit = inputSplitAssigner.getNextInputSplit(null, 0);
		assertEquals(nextInputSplit, inputSplits[inputSplits.length - 1]);
		ethereumInputSource.close();

	}

	@Test
	public void testNextRecord() throws IOException {
		ethereumInputSource = new EthereumInputSource(generateClientAddress(), 0, 5);
		ethereumInputSource.configure(new Configuration());
		GenericInputSplit[] inputSplits = ethereumInputSource.createInputSplits(0);
		ethereumInputSource.open(inputSplits[0]);
		EthBlock ethBlock = new EthBlock();
		ethBlock = ethereumInputSource.nextRecord(ethBlock);
		assertEquals(ethBlock.getBlock().getNumber(), BigInteger.ZERO);
		ethereumInputSource.close();
	}


	@NotNull
	private String generateClientAddress() {
		return "http://" + ganacheContainer.getContainerIpAddress() + ":" + ganacheContainer.getFirstMappedPort() + "/";
	}
}
