package com.bigeek.flink.batch.connectors.ethereum;

import okhttp3.OkHttpClient;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.common.io.DefaultInputSplitAssigner;
import org.apache.flink.api.common.io.RichInputFormat;
import org.apache.flink.api.common.io.statistics.BaseStatistics;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.core.io.InputSplitAssigner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.Web3jService;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.http.HttpService;
import org.web3j.protocol.ipc.UnixIpcService;
import org.web3j.protocol.ipc.WindowsIpcService;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

public class EthereumInputSource extends RichInputFormat<List<EthBlock>, EthereumInputSplit> {

	Logger logger = LoggerFactory.getLogger(EthereumInputSource.class);

	private transient Web3j web3j;

	private Integer start;

	private Integer end;

	private Integer sizeStages;

	private boolean reachedEnd = false;

	public EthereumInputSource(String clientAddress, Integer start, Integer end) {
		this.start = start;
		this.end = end;
		this.clientAddress = clientAddress;
	}

	public EthereumInputSource(String clientAddress, Integer start, Integer end, Integer sizeStages) {
		this.start = start;
		this.end = end;
		this.clientAddress = clientAddress;
		this.sizeStages = sizeStages;
	}


	private String clientAddress;

	private EthereumInputSplit split;

	public EthereumInputSource() {
	}

	private OkHttpClient createOkHttpClient() {
		OkHttpClient.Builder builder = new OkHttpClient.Builder();
		//configureLogging(builder);
		//configureTimeouts(builder);
		return builder.build();
	}

	@Override
	public void configure(Configuration parameters) {
		if (StringUtils.isEmpty(this.clientAddress)) {
			this.clientAddress = parameters.getString("web3j.clientAddress", "http://localhost:8545");
		}
		Web3jService web3jService;

		if (clientAddress.startsWith("http")) {
			web3jService = new HttpService(clientAddress, createOkHttpClient(), false);
		} else if (System.getProperty("os.name").toLowerCase().startsWith("win")) {
			web3jService = new WindowsIpcService(clientAddress);
		} else {
			web3jService = new UnixIpcService(clientAddress);
		}

		web3j = Web3j.build(web3jService);
		try {

			if (start == null) {
				start = parameters.getInteger("web3j.start", 0);
			}
			if (end == null) {
				int latest = web3j.ethGetBlockByNumber(DefaultBlockParameter.valueOf("latest"), false)
						.send()
						.getBlock()
						.getNumber()
						.intValue();
				end = parameters.getInteger("web3j.end", latest);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	@Override
	public BaseStatistics getStatistics(BaseStatistics cachedStatistics) throws IOException {
		return null;
	}

	@Override
	public EthereumInputSplit[] createInputSplits(int minNumSplits) throws IOException {
		List<EthereumInputSplit> splitList = new ArrayList<>();
		int numberBlocks = (end - start) / minNumSplits;
		int localStart = start;
		while (localStart + numberBlocks < end && !(localStart + numberBlocks >= end)) {
			EthereumInputSplit ethereumInputSplit = new EthereumInputSplit(localStart, localStart + numberBlocks, numberBlocks);
			splitList.add(ethereumInputSplit);
			localStart = localStart + numberBlocks + 1;
		}
		if (localStart < end) {
			EthereumInputSplit ethereumInputSplit = new EthereumInputSplit(localStart, end, end-localStart);
			splitList.add(ethereumInputSplit);
		}

		return splitList.toArray(new EthereumInputSplit[0]);
	}


	@Override
	public InputSplitAssigner getInputSplitAssigner(EthereumInputSplit[] inputSplits) {
		return new DefaultInputSplitAssigner(inputSplits);
	}

	@Override
	public void open(EthereumInputSplit split) throws IOException {

		this.split = split;
	}


	@Override
	public boolean reachedEnd() throws IOException {
		return reachedEnd;
	}

	@Override
	public List<EthBlock> nextRecord(List<EthBlock> reuse) throws IOException {
		int start = this.split.getStart();
		int stop = this.split.getEnd();
		reuse = new ArrayList<>();
		List<CompletableFuture<EthBlock>> completableFutures = new ArrayList<>();
		while (start <= stop) {
			logger.info("Getting block {}", start);
			CompletableFuture<EthBlock> ethBlockCompletableFuture = web3j.ethGetBlockByNumber(DefaultBlockParameter.valueOf(BigInteger.valueOf(start)), true).sendAsync();
			completableFutures.add(ethBlockCompletableFuture);

			if (this.sizeStages != null && completableFutures.size() == sizeStages) {
				reuse.addAll(completeFutureList(completableFutures));
				completableFutures = new ArrayList<>();

			}
			logger.info("Block got {}", start);
			start++;
		}
		reuse.addAll(completeFutureList(completableFutures));

		reachedEnd = true;

		return reuse;

	}

	private List<EthBlock> completeFutureList(List<CompletableFuture<EthBlock>> completableFutures) {
		try {
			logger.info("Processing futures!! size {}", completableFutures.size());

			return CompletableFuture.allOf(completableFutures.toArray(new CompletableFuture<?>[0]))
					.thenApply(v -> completableFutures.stream()
							.map(CompletableFuture::join)
							.collect(toList())
					).get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
		return Collections.emptyList();
	}


	@Override
	public void close() throws IOException {
		web3j.shutdown();
	}

}
