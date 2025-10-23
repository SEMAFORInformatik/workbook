package ch.semafor.intens.ws.model;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public enum ApprovalState {
	Experimental("experimental"),
	InPreparation("inPreparation"),
	Shared("shared"),

	Approved("approved"),
	Tested("tested"),
	Tendered("tendered"),

	Obsolete("obsolete"),
	Unknown("unknown");
	
	private final String name;

	ApprovalState(String name) {
		this.name = name;
	}

	private static final Map<String, ApprovalState> lookup = new HashMap<String, ApprovalState>();

	static {
		for (ApprovalState a : EnumSet.allOf(ApprovalState.class))
			lookup.put(a.getName(), a);
	}

	public String getName() {
		return name;
	}

	public static ApprovalState get(String name) {
		ApprovalState a = lookup.get(name);
		if( a==null){
			a=ApprovalState.Experimental;
		}
		return a;
	}

	public Boolean isSnapshot() {
		ApprovalState a = lookup.get(getName());
		switch (a) {
			case Experimental:
			case InPreparation:
			case Shared:
			case Obsolete:
			case Unknown:
				return true;
			default:
				return false;
		}

	}
}
