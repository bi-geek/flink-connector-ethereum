package com.bigeek.flink.streaming.connectors.ethereum;

import com.bigeek.flink.utils.EthereumWrapper;
import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.source.RichSourceFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.methods.response.EthBlock;

import java.io.IOException;
import java.math.BigInteger;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.nonNull;

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
     * Disposable Ethereum
     */
    private volatile Disposable disposable;

    /**
     * Lock object
     */
    private transient Object waitLock = new Object();

    /**
     * Constructor.
     *
     * @param clientAddress
     */
    public EthereumFunctionSource(String clientAddress) {

        this.clientAddress = clientAddress;

    }
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

        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
        }
    }

    @Override
    public void run(SourceContext<EthBlock> sourceContext) throws InterruptedException {
        waitLock = new Object();
        logger.info("Generating subscription with start value {}", start);
        Flowable<EthBlock> ethBlockFlowable = EthereumWrapper.getInstance()
                .replayPastAndFutureBlocksFlowable(DefaultBlockParameter.valueOf(BigInteger.valueOf(start)),
                        true);
        if (nonNull(timeoutSeconds)) {
            ethBlockFlowable = ethBlockFlowable.timeout(timeoutSeconds, TimeUnit.SECONDS);
        }
        disposable = ethBlockFlowable.subscribe(sourceContext::collect);

        while (!disposable.isDisposed()) {
            synchronized (waitLock) {
                waitLock.wait(100L);
            }
        }
    }

    @Override
    public void cancel() {
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
        }
    }
}
