package com.kira.jarplay.crashes.models;

import java.util.List;

public class RequestBody {

	public final List<AbstractLog> logs;

	public RequestBody(List<AbstractLog> logs) {
		this.logs = logs;
	}
}
