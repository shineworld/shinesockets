import it.shine.ShineThread;

public class ShineSocketsDev {

	public static void main(String[] args) {

		final class ListenerThread extends ShineThread {
			public void run() {
				try {
					while (true) {
						Thread.sleep(100);
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

		ListenerThread thread = new ListenerThread();
		thread.start();

		try {
			while (true) {
				Thread.sleep(100);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

}
