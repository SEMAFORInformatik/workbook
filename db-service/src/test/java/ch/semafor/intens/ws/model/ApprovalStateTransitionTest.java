package ch.semafor.intens.ws.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.semafor.intens.ws.utils.ApprovalStateTransition;
import ch.semafor.gendas.service.ElementService;
import ch.semafor.gendas.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("jpa")
public class ApprovalStateTransitionTest {

  @Autowired
  ApprovalStateTransition approvalStateTransition;

	@MockBean
	ElementService elementService;
	@MockBean
	UserService userService;

	@Test
	public void test() {
		ApprovalState from = ApprovalState.Approved;
		ApprovalState to = ApprovalState.Approved;
		assertTrue(approvalStateTransition.isValid(from, to));

		from = ApprovalState.Approved;
		to = ApprovalState.Obsolete;
		assertTrue(approvalStateTransition.isValid(from, to));

		from = ApprovalState.Obsolete;
		to = ApprovalState.Approved;
		assertFalse(approvalStateTransition.isValid(from, to));
	}

}
