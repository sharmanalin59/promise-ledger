package com.promiseledger.extraction;

import java.time.Duration;

interface ClaudeSleeper {

	void sleep(Duration duration) throws InterruptedException;

}
