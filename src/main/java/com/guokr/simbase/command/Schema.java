package com.guokr.simbase.command;

import org.wahlque.net.action.Command;

public class Schema implements Command {

	public String key;

	@Override
	public String actionName() {
		return "vsch";
	}

}
