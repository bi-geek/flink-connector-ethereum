package com.bigeek.flink.batch.connectors.ethereum;

import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.io.TextOutputFormat;
import org.apache.flink.api.java.operators.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.protocol.core.methods.response.EthBlock;

import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

public class JobTest {
	static Logger logger = LoggerFactory.getLogger(JobTest.class);

	public static void main(String[] args) throws Exception {
		final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();
//https://mainnet.infura.io/edaU1hv4fQ7Knj0O3Yd8
		DataSource<List<EthBlock>> list = env.createInput(new EthereumInputSource("https://mainnet.infura.io/edaU1hv4fQ7Knj0O3Yd8", 1000000, 1000500));
		//list.print();

		list.writeAsFormattedText("/home/rafa/text.txt", new TextOutputFormat.TextFormatter<List<EthBlock>>() {
			@Override
			public String format(List<EthBlock> ethBlocks) {
				return ethBlocks.stream()
						.map(ethBlock -> {
							String block =ethBlock.getBlock().getNumber().toString();
							return block+"#"+ethBlock.getBlock().getTransactions().size();
						})
						.collect(Collectors.joining(","));
			}
		});


		env.execute("TEST");


	}
}
