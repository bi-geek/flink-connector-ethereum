package com.bigeek.flink.streaming.connectors.ethereum;

import com.bigeek.flink.utils.EthereumWrapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.source.RichSourceFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.methods.response.EthBlock;
import rx.Subscription;

import java.io.IOException;
import java.math.BigInteger;

/**
 * Function source for ethereum .
 */
public class EthereumFunctionSource extends RichSourceFunction<EthBlock> {

    /**
     * Logger.
     */
    private Logger logger = LoggerFactory.getLogger(EthereumFunctionSource.class);


    /**
     * Start block .
     */
    private Integer start;

    /**
     * Client address for ethereum .
     */
    private String clientAddress;

    /**
     * Timeout in seconds.
     */
    private Long timeoutSeconds;

    /**
     * Subscription Ethereum
     */
    private transient Subscription subscription;

    /**
     * Constructor.
     *
     * @param clientAddress
     * @param start
     */
    public EthereumFunctionSource(String clientAddress, Integer start) {

        this.clientAddress = clientAddress;
        this.start = start;

    }

    /**
     * Constructor.
     *
     * @param clientAddress
     * @param start
     * @param timeoutSeconds
     */
    public EthereumFunctionSource(String clientAddress, Integer start, Long timeoutSeconds) {

        this.clientAddress = clientAddress;
        this.start = start;
        this.timeoutSeconds = timeoutSeconds;

    }

    /**
     * Default constructor.
     */
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
        Web3j web3j = EthereumWrapper.configureInstance(this.clientAddress, this.timeoutSeconds);
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
        if (subscription != null && !subscription.isUnsubscribed()) {
            subscription.unsubscribe();
        }

    }

    @Override
    public void run(SourceContext<EthBlock> sourceContext) {
        logger.info("Generating subscription with start value {}", start);
        subscription = EthereumWrapper.getInstance().catchUpToLatestAndSubscribeToNewBlocksObservable(DefaultBlockParameter.valueOf(BigInteger.valueOf(start)),
                true)
                .subscribe(sourceContext::collect);
    }

    @Override
    public void cancel() {
        if (subscription != null && !subscription.isUnsubscribed()) {
            subscription.unsubscribe();
        }
    }
}
