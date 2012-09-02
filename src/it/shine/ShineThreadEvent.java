package it.shine;

import java.util.EventObject;

public class ShineThreadEvent extends EventObject {
	private static final long serialVersionUID = -1339271301410423936L;

	public ShineThreadEvent(ShineThread source) {
		super(source);
	}
}
