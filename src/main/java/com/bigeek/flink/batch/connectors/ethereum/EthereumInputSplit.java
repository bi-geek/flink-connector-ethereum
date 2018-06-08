package com.bigeek.flink.batch.connectors.ethereum;

import org.apache.flink.core.io.InputSplit;

public class EthereumInputSplit  implements InputSplit {

	private final int start;

	private final int end;

	private final int length;

	public EthereumInputSplit(int start, int end, int length) {
		this.start = start;
		this.end = end;
		this.length = length;
	}

	@Override
	public int getSplitNumber() {
		return (end-start)/length;
	}

	public int getStart() {
		return start;
	}

	public int getEnd() {
		return end;
	}
}
