package org.wahlque.cmd;

import java.util.Map;

import org.wahlque.net.transport.Payload;

public interface Command {

	public void from(Map<String, Object> context, Payload<?> data);

	public Payload<?> to(Map<String, Object> context);

	public Payload<?> apply(Map<String, Object> context, Payload<?> data);

	public void validate(Payload<?> data) throws CommandException;

}
