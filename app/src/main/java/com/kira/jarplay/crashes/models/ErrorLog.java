package com.kira.jarplay.crashes.models;

public class ErrorLog extends AbstractLog {
	public final String type = "managedError";
	public final boolean fatal = true;

	public int processId;
	public String processName;
	public String architecture;
	public String appLaunchTimestamp;
	public long errorThreadId;
	public String errorThreadName;
	public ExceptionModel exception;

	public ErrorLog(String id) {
		super(id);
	}
}
