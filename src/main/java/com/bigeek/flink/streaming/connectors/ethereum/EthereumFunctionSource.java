package com.bigeek.flink.streaming.connectors.ethereum;

import org.apache.commons.lang3.StringUtils;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.source.RichSourceFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.methods.response.EthBlock;

import static com.bigeek.flink.utils.EthereumUtils.generateClient;

public class EthereumFunctionSource extends RichSourceFunction<EthBlock> {

	private Logger logger = LoggerFactory.getLogger(EthereumFunctionSource.class);

	private transient Web3j web3j;

	private String start = "latest";
	private String clientAddress;

	private Long timeoutSeconds;


	public EthereumFunctionSource(String clientAddress, Integer start) {

		this.clientAddress = clientAddress;
		this.start = start.toString();

	}
	public EthereumFunctionSource(String clientAddress, Integer start, Long timeoutSeconds) {

		this.clientAddress = clientAddress;
		this.start = start.toString();
		this.timeoutSeconds = timeoutSeconds;

	}

	public EthereumFunctionSource() {
	}


	@Override
	public void open(Configuration parameters) throws Exception {
		super.open(parameters);
		if (StringUtils.isEmpty(this.clientAddress)) {
			this.clientAddress = parameters.getString("web3j.clientAddress", "http://localhost:8545");
		}

		generateClient(clientAddress, timeoutSeconds);

		start = parameters.getString("web3j.start", start);

	}


	@Override
	public void close() {
		if (web3j != null) {
			web3j.shutdown();
		}
	}

	@Override
	public void run(SourceContext<EthBlock> sourceContext) {

		web3j.catchUpToLatestAndSubscribeToNewBlocksObservable(DefaultBlockParameter.valueOf(start.toString()), true)
				.subscribe(sourceContext::collect);
	}

	@Override
	public void cancel() {
		if (web3j != null) {
			web3j.shutdown();
		}
	}
}
