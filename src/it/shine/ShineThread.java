package it.shine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

public abstract class ShineThread implements Runnable {
	private StopMode stopMode = StopMode.TERMINATE;
	private volatile boolean stopped = true;
	private volatile boolean terminated = false;
	private volatile Exception terminatingException = null;

	private final ArrayList<ShineThreadListener> threadListeners = new ArrayList<ShineThreadListener>();
	private final Object suspendLock = new Object();

	protected volatile boolean stopRequested = false;
	protected final Thread threadImpl = new ShineThreadImpl();

	private class ShineThreadImpl extends Thread {
		public void run() {
			try {
				try {
					beforeExecute();
					while (!terminated) {
						if (stopped) {
							doStopped();
							// it is possible that either in the doStopped or from another thread, the thread is
							// restarted, in which case we don't want to re-stop it
							if (stopped) {
								if (terminated) {
									break;
								}
								ShineThread.this.suspend();
								if (terminated) {
									break;
								}
							}
						}
						try {
							beforeRun();
							try {
								while (!stopped) {
									ShineThread.this.run();
								}
							} finally {
								afterRun();
							}
						} finally {
							cleanup();
						}
					}
				} finally {
					afterExecute();
				}
			} catch (Exception e) {
				terminatingException = e;
				doException();
				terminate();
			}
		}
	}

	public static class StopMode {
		private final String friendlyName;

		private StopMode(String friendlyName) {
			this.friendlyName = friendlyName;
		}

		public String toString() {
			return friendlyName;
		}

		public static final StopMode TERMINATE = new StopMode("Terminate");
		public static final StopMode SUSPEND = new StopMode("Suspend");
	}

	// TODO: I don't know what could happen if a collection listener will be kill after synchronized part...
	private void doException() {
		Collection<ShineThreadListener> listeners = null;
		synchronized (threadListeners) {
			listeners = Collections.unmodifiableCollection(threadListeners);
		}
		ShineThreadEvent event = new ShineThreadEvent(this);
		Iterator<ShineThreadListener> i = listeners.iterator();
		while (i.hasNext()) {
			((ShineThreadListener) i.next()).onException(event);
		}
	}

	// TODO: I don't know what could happen if a collection listener will be kill after synchronized part...
	private void doStopped() {
		Collection<ShineThreadListener> listeners = null;
		synchronized (threadListeners) {
			listeners = Collections.unmodifiableCollection(threadListeners);
		}
		ShineThreadEvent event = new ShineThreadEvent(this);
		Iterator<ShineThreadListener> i = listeners.iterator();
		while (i.hasNext()) {
			((ShineThreadListener) i.next()).onStopped(event);
		}
	}

	private void resume() {
		synchronized (suspendLock) {
			suspendLock.notifyAll();
		}
	}

	protected void afterExecute() throws InterruptedException {
	}

	protected void afterRun() throws InterruptedException {
	}

	protected void beforeExecute() throws InterruptedException {
	}

	private void suspend() throws InterruptedException {
		synchronized (suspendLock) {
			suspendLock.wait();
		}
	}

	protected void beforeRun() throws InterruptedException {
	}

	protected void cleanup() throws InterruptedException {
	}

	protected Thread getThreadImpl() {
		return threadImpl;
	}

	public ShineThread() {
	}

	public void addThreadListener(ShineThreadListener listener) {
		synchronized (threadListeners) {
			if (!threadListeners.contains(listener)) {
				threadListeners.add(listener);
			}
		}
	}

	public boolean isStopped() {
		return stopped;
	}

	/**
	 * Waits for this thread to die.
	 * 
	 * @exception InterruptedException
	 *                if any thread has interrupted the current thread. The <i>interrupted status</i> of the current
	 *                thread is cleared when this exception is thrown.
	 */
	public void join() throws InterruptedException {
		threadImpl.join();
	}

	public void removeThreadListener(ShineThreadListener listener) {
		synchronized (threadListeners) {
			threadListeners.remove(listener);
		}
	}

	@Override
	public void run() {
	}

	public final void start() {
		if (terminated) {
			throw new IllegalStateException();
		}
		if (!threadImpl.isAlive()) {
			threadImpl.start();
		} else if (stopped) {
			resume();
		}
		stopped = false;
	}

	public final void stop() {
		if (!stopped) {
			if (stopMode == StopMode.TERMINATE) {
				terminate();
			}
			stopped = true;
		}
	}

	/**
	 * Forces the thread of execution to be halted.
	 */
	public void terminate() {
		stopped = true;
		terminated = true;
		resume();
	}

	/**
	 * Signals a thread to terminate and waits for it to finish by exiting the run() method.
	 */
	public void terminateAndWaitFor() throws InterruptedException {
		terminate();
		join();
	}

}
