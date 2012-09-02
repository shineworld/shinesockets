package it.shine;

import java.util.EventListener;

public interface ShineThreadListener extends EventListener {
	void onStopped(ShineThreadEvent obj);

	void onException(ShineThreadEvent obj);
}
