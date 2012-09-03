package it.shine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

public abstract class ShineThread implements Runnable {
	private Priority priority = Priority.NORMAL;
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

	public static class Priority {
		private final int priority;

		private Priority(int priority) {
			this.priority = priority;
		}

		public static final Priority MIN = new Priority(Thread.MIN_PRIORITY);
		public static final Priority NORMAL = new Priority(Thread.NORM_PRIORITY);
		public static final Priority MAX = new Priority(Thread.MAX_PRIORITY);

		public static Priority parse(int value) {
			switch (value) {
			case 1:
			case 2:
			case 3:
				return MIN;
			case 4:
			case 5:
			case 6:
				return NORMAL;
			case 7:
			case 8:
			case 9:
			case 10:
				return MAX;
			default:
				throw new IllegalArgumentException(
						"Not a valid thread priority value (1..10)");
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

	private void suspend() throws InterruptedException {
		synchronized (suspendLock) {
			suspendLock.wait();
		}
	}

	protected void afterExecute() throws InterruptedException {
	}

	protected void afterRun() throws InterruptedException {
	}

	protected void beforeExecute() throws InterruptedException {
	}

	protected void finalize() throws Throwable {
		// TODO: Could be interesting to ensure that all dispose operations are done (threads closed, monitors released
		// and so on) when the object is destroyed. I know java don't require that but could be something goes wrong
		// with shared monitors and I will keep an open door here.
		super.finalize();
	}

	protected void beforeRun() throws InterruptedException {
	}

	protected void cleanup() throws InterruptedException {
	}

	public Priority getPriority() {
		return Priority.parse(threadImpl.getPriority());
	}

	protected Thread getThreadImpl() {
		return threadImpl;
	}

	public ShineThread() {
		// TODO: I would like to move any member's value initialization in constructor rather than put them in member
		// definition and add the argument "boolean createSuspended" in constructor so you may choose if created thread
		// goes in run just after object creation.
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

	public void setPriority(Priority value) {
		this.priority = value;
		if (!terminated) {
			threadImpl.setPriority(value.priority);
		}
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

	public void terminate() {
		stopped = true;
		terminated = true;
		resume();
	}

	public void terminateAndWaitFor() throws InterruptedException {
		terminate();
		join();
	}

}
