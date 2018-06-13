package com.bigeek.flink.streaming.connectors.ethereum;

import org.apache.commons.lang3.StringUtils;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.source.RichSourceFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthBlock;

import java.io.IOException;
import java.math.BigInteger;

import static com.bigeek.flink.utils.EthereumUtils.generateClient;

public class EthereumFunctionSource extends RichSourceFunction<EthBlock> {

	private Logger logger = LoggerFactory.getLogger(EthereumFunctionSource.class);

	private transient Web3j web3j;

	private Integer start;
	private String clientAddress;

	private Long timeoutSeconds;


	public EthereumFunctionSource(String clientAddress, Integer start) {

		this.clientAddress = clientAddress;
		this.start = start;

	}

	public EthereumFunctionSource(String clientAddress, Integer start, Long timeoutSeconds) {

		this.clientAddress = clientAddress;
		this.start = start;
		this.timeoutSeconds = timeoutSeconds;

	}

	public EthereumFunctionSource() {
	}


	@Override
	public void open(Configuration parameters) throws IOException {
		if (StringUtils.isEmpty(this.clientAddress)) {
			this.clientAddress = parameters.getString("web3j.clientAddress", "http://localhost:8545");
		}
		if (this.timeoutSeconds != null) {
			this.timeoutSeconds = parameters.getLong("web3j.timeout", this.timeoutSeconds);
		}
		web3j = generateClient(clientAddress, timeoutSeconds);

		if (start == null) {
			start = web3j.ethGetBlockByNumber(DefaultBlockParameter.valueOf("latest"), false)
					.send()
					.getBlock()
					.getNumber()
					.intValue();

		}
		start = parameters.getInteger("web3j.start", start);
	}


	@Override
	public void close() {
		if (web3j != null) {
			web3j.shutdown();
		}
	}

	@Override
	public void run(SourceContext<EthBlock> sourceContext) {
		web3j.catchUpToLatestAndSubscribeToNewBlocksObservable(DefaultBlockParameter.valueOf(BigInteger.valueOf(start)),
				true)
				.subscribe(sourceContext::collect);
	}

	@Override
	public void cancel() {
		if (web3j != null) {
			web3j.shutdown();
		}
	}
}
