# Ethereum Flink Connector [![Build Status](https://travis-ci.org/bi-geek/flink-connector-ethereum.svg?branch=master)](https://travis-ci.org/bi-geek/flink-connector-ethereum) [![codecov](https://codecov.io/gh/bi-geek/flink-connector-ethereum/branch/master/graph/badge.svg)](https://codecov.io/gh/bi-geek/flink-connector-ethereum) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.bigeek/flink-connector-ethereum/badge.svg?style=plastic)](https://maven-badges.herokuapp.com/maven-central/com.bigeek/flink-connector-ethereum) [![GitHub stars](https://img.shields.io/github/stars/badges/shields.svg?style=social&label=Star)](https://github.com/bi-geek/flink-connector-ethereum)

[Flink](https://flink.apache.org/) connector for [Ethereum](https://www.ethereum.org/).

# Table of Contents
 
- [Overview](#overview)
- [Getting started](#getting-started)
- [License](#license)


### Overview

Ethereum Flink connector provides an InputFormat implementation for reading data from the Ethereum blockchain.
It also provides the streaming version.


### Getting started

#### Add dependency

```xml
<dependency>
    <groupId>com.github.bi-geek</groupId>
    <artifactId>flink-connector-ethereum</artifactId>
    <version>1.0.0</version>
</dependency>

```
#### Code example


```java
public class EthereumJob {

	private static Logger logger = LoggerFactory.getLogger(EthereumJob.class);

	public static void main(String[] args) throws Exception {
		final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();
		DataSource<EthBlock> list = env.createInput(new EthereumInputSource("https://localhost:8545", 4000000, 4005000));
		logger.info("counter: "+ list.count());
		env.execute("Job");


	}
}
```



### License

Ethereum Flink Connector is licensed under the MIT License. See [LICENSE](LICENSE.md) for details.

Copyright (c) 2018 BI-Geek
