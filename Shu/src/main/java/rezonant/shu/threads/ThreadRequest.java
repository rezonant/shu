package rezonant.shu.threads;

import android.util.Log;

import java.util.concurrent.CountDownLatch;

/**
 * Provides a simple thread-to-thread synchronized value transaction, where the requesting
 * thread waits for the messaged thread to return a value. Best used for getting UI values from
 * background threads.
 *
 * Created by liam on 7/4/14.
 */
public class ThreadRequest {
	public ThreadRequest(Thread thread)
	{
		latch = new CountDownLatch(1);
	}

	private CountDownLatch latch;
	private Object result = null;
	private boolean resultReceived = false;

	public Object waitForResult()
	{
		if (resultReceived)
			return result;

		while (true) {
			try {
				latch.await();
			} catch (InterruptedException e) {
				// Latch restarted if necessary as no interrupts
				// are supported.

				if (!resultReceived)
					continue;
			}
			break;
		}
		return result;
	}

	public void respond(Object obj)
	{
		if (this.resultReceived) {
			Log.w("THREADING", "ThreadRequest.respond() called multiple times");
			return;
		}

		Log.i("FYI", "ThreadRequest receiving response: "+obj);
		this.result = obj;
		this.resultReceived = true;
		latch.countDown();
	}
}
