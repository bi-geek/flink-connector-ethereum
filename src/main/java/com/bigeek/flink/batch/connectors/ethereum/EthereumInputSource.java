package com.bigeek.flink.batch.connectors.ethereum;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.api.common.io.DefaultInputSplitAssigner;
import org.apache.flink.api.common.io.RichInputFormat;
import org.apache.flink.api.common.io.statistics.BaseStatistics;
import org.apache.flink.core.io.GenericInputSplit;
import org.apache.flink.core.io.InputSplitAssigner;

import org.apache.commons.lang3.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.methods.response.EthBlock;

import java.io.IOException;
import java.math.BigInteger;

import static com.bigeek.flink.utils.EthereumUtils.generateClient;

/**
 * Input Source for Ethereum .
 */
public class EthereumInputSource extends RichInputFormat<EthBlock, GenericInputSplit> {

	private Logger logger = LoggerFactory.getLogger(EthereumInputSource.class);

	private transient Web3j web3j;

	private Integer start;

	private Integer end;

	private boolean reachedEnd = false;

	private String clientAddress;

	private Integer split;

	private Long timeoutSeconds;

	public EthereumInputSource(String clientAddress, Integer start, Integer end) {
		this.start = start;
		this.end = end;
		this.clientAddress = clientAddress;
	}

	public EthereumInputSource(String clientAddress, Integer start, Integer end, Long timeoutSeconds) {
		this.start = start;
		this.end = end;
		this.clientAddress = clientAddress;
		this.timeoutSeconds = timeoutSeconds;
	}


	public EthereumInputSource() {
	}


	@Override
	public void configure(Configuration parameters) {

		if (StringUtils.isEmpty(this.clientAddress)) {
			this.clientAddress = parameters.getString("web3j.clientAddress", "http://localhost:8545");
		}

		if (this.timeoutSeconds != null) {
			this.timeoutSeconds = parameters.getLong("web3j.timeout", this.timeoutSeconds);
		}

		this.web3j = generateClient(this.clientAddress, this.timeoutSeconds);


		if (this.start == null) {
			this.start = parameters.getInteger("web3j.start", 0);
		}
		if (this.end == null) {
			int latest;
			try {
				latest = web3j.ethGetBlockByNumber(DefaultBlockParameter.valueOf("latest"), false)
						.send()
						.getBlock()
						.getNumber()
						.intValue();
			} catch (IOException e) {
				throw new RuntimeException(e);

			}
			this.end = parameters.getInteger("web3j.end", latest);
		}

		if (this.start >= this.end) {
			throw new IllegalArgumentException("Start block must have less value than end block");

		}

	}

	@Override
	public BaseStatistics getStatistics(BaseStatistics cachedStatistics)  {
		return cachedStatistics;
	}

	@Override
	public GenericInputSplit[] createInputSplits(int minNumSplits)  {

		GenericInputSplit[] ret = new GenericInputSplit[(this.end - this.start) + 1];

		int startLocal = this.start;
		for (int i = 0; i <= ret.length && startLocal <= this.end; i++) {
			ret[i] = new GenericInputSplit(startLocal, ret.length);
			startLocal++;
		}
		return ret;
	}

	@Override
	public InputSplitAssigner getInputSplitAssigner(GenericInputSplit[] inputSplits) {
		return new DefaultInputSplitAssigner(inputSplits);
	}

	@Override
	public void open(GenericInputSplit split) {
		this.split = split.getSplitNumber();
		this.reachedEnd = false;


	}

	@Override
	public boolean reachedEnd() {
		return reachedEnd;
	}

	@Override
	public EthBlock nextRecord(EthBlock reuse) throws IOException {


		logger.info("Getting block {}", this.split);
		reuse = web3j
				.ethGetBlockByNumber(DefaultBlockParameter.valueOf(BigInteger.valueOf(this.split)), true)
				.send();

		logger.info("Block got {}", this.split);
		reachedEnd = true;

		return reuse;

	}

	@Override
	public void close() {
		web3j.shutdown();
	}

}
