package ch.semafor.intens.ws.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import ch.semafor.intens.ws.model.ApprovalState;

@Component
public class ApprovalStateTransition {

	@Autowired
	private Environment env;

	Logger logger = LoggerFactory.getLogger(ApprovalStateTransition.class);

	public boolean isValid(ApprovalState from, ApprovalState to) {
		var validTo = env.getProperty("validStateTransitions." + from);
		return validTo != null && validTo.contains(to.toString());
	}
}
