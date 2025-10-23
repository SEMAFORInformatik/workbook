package ch.semafor.intens.ws;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Before;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kie.api.command.Command;
import org.kie.api.logger.KieRuntimeLogger;
import org.kie.api.runtime.ExecutionResults;
import org.kie.api.runtime.StatelessKieSession;
import org.kie.internal.command.CommandFactory;
import org.kie.internal.logger.KnowledgeRuntimeLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import ch.semafor.gendas.model.Group;
import ch.semafor.gendas.model.Owner;
import ch.semafor.gendas.model.Role;
import ch.semafor.intens.ws.config.ComponentProperties;
import ch.semafor.intens.ws.model.ApprovalState;
import ch.semafor.intens.ws.model.Component;
import ch.semafor.intens.ws.model.User;
import ch.semafor.intens.ws.model.Variant;


@SpringBootTest
// @ExtendWith(SpringRunner.class)
public class RuleTest {
	static Owner fred;
	static User fredUser;
	static Owner bob;
	static Owner mary;
	static Group groupA = new Group("Group A");
	static Group groupB = new Group("Group B");

	@Autowired
	StatelessKieSession kieSession;

	@Autowired
	ComponentProperties componentProperties;

	KieRuntimeLogger logger;

	@BeforeEach
	public void setup(){
		logger=KnowledgeRuntimeLoggerFactory.newConsoleLogger(kieSession);
	}
	@AfterEach
	public void teardown(){
		logger.close();
	}
	@BeforeAll
	public static void setupOnce(){
		fred = new Owner("fred");
		fred.addGroup(groupA);
		fred.setActiveGroup(groupA);
		fred.addRole(new Role("ROLE_ADMIN"));
		fredUser = new User(fred);
		bob = new Owner("bob");
		bob.addGroup(groupA);
		bob.addRole(new Role("ROLE_USER"));
		mary = new Owner("mary");
		mary.addGroup(groupB);
		mary.addRole(new Role("ROLE_USER"));
	}
	private Map<String, Object> getProps( Integer rev, Owner owner, Group group ){
		Map<String, Object> props = new HashMap<String, Object>();
		props.put("approval", "approved");
		props.put("owner", owner.getUsername());
		props.put("group", group.getName());
		props.put("rev", rev);
		props.put("name", "someName");
		props.put("type", "someType");
		return props;
	}
	@Test
	public void differentUserStatusApproved() {
		int rev=0;
		Map<String, Object> props = getProps( rev, bob, groupA );
		Component c = new Component(props, componentProperties);
                //                c.setModified(false);
		Variant v = new Variant(props, componentProperties);

		List<Command> cmds = new ArrayList<Command>();
		
		cmds.add( CommandFactory.newInsert( v, "variant" ) );
		cmds.add( CommandFactory.newInsert( c, "comp" ) );
		cmds.add( CommandFactory.newInsert( fredUser, "user" ) );
		// fred has ADMIN_ROLE
		cmds.add( CommandFactory.newInsert( c.getGroup(), "group" ) );
		ExecutionResults results = kieSession.execute( CommandFactory.newBatchExecution( cmds ) );
		
//		kieSession.execute(CommandFactory.newInsertElements( Arrays.asList(new Object[] {c, "anotheruser"})));
//		kieSession.execute(c);
		
		Assertions.assertTrue(c.isOK());
		Assertions.assertEquals(ApprovalState.Approved, c.getApprovalState());
		Assertions.assertEquals(rev, c.getRevision());
		Assertions.assertTrue(v.isOK());
		Assertions.assertEquals(ApprovalState.Approved, v.getApprovalState());
		Assertions.assertEquals(rev, v.getRevision());

		Map<String, Object> mods = new HashMap<String, Object>();
		mods.put("a", 1);
                c.setModified(mods);
                v.setModified(mods);
		results = kieSession.execute( CommandFactory.newBatchExecution( cmds ) );
		Assertions.assertTrue(c.isOK());
		Assertions.assertEquals(ApprovalState.InPreparation, c.getApprovalState());
		Assertions.assertEquals(rev+1, c.getRevision());
		Assertions.assertEquals(fredUser, c.getOwner());
		Assertions.assertEquals(fred.getActiveGroup(), c.getGroup());

		Assertions.assertTrue(v.isOK());
		Assertions.assertEquals(ApprovalState.InPreparation, v.getApprovalState());
		Assertions.assertEquals(rev+1, v.getRevision());
		Assertions.assertEquals(fredUser, v.getOwner());
		Assertions.assertEquals(fred.getActiveGroup(), v.getGroup());

	}
	
	@Test
	public void differentUserWithAdmin() {
		Integer rev=0;
		Map<String, Object> props = getProps( rev, bob, groupA );
		Component c = new Component(props, componentProperties);

		Owner owner = new Owner("anotheruser");
		owner.addGroup(new Group("ADMIN"));
		User user = new User(owner);
		
		List<Command> cmds = new ArrayList<Command>();
		
		cmds.add( CommandFactory.newInsert( c, "comp" ) );
		cmds.add( CommandFactory.newInsert( c.getGroup(), "group" ) );
		cmds.add( CommandFactory.newInsert( user, "user" ) );
		ExecutionResults results = kieSession.execute( CommandFactory.newBatchExecution( cmds ) );
//		kieSession.execute(c);
		
		Assertions.assertTrue(c.isOK());
	}

	@Test
	public void differentUserWithoutAdmin() {
		Integer rev=0;
		Map<String, Object> props = getProps( rev, bob, groupA );
		Component c = new Component(props, componentProperties);

		User user = new User(mary);
		// bos is in groupA mary in groupB
		List<Command> cmds = new ArrayList<Command>();
		
		cmds.add( CommandFactory.newInsert( c, "comp" ) );
		cmds.add( CommandFactory.newInsert( c.getGroup(), "group" ) );
		cmds.add( CommandFactory.newInsert( user, "user" ) );
		ExecutionResults results = kieSession.execute( CommandFactory.newBatchExecution( cmds ) );
//		kieSession.execute(c);
		
		Assertions.assertFalse(c.isOK());
	}

	@Test
	public void differentUserWithSameGroup() {
		Integer rev=0;
		Map<String, Object> props = getProps( rev, fred, groupA );
		Component c = new Component(props, componentProperties);

		User user = new User(bob);
		// fred and bob are in same groupA
		List<Command> cmds = new ArrayList<Command>();
		
		cmds.add( CommandFactory.newInsert( c, "comp" ) );
		cmds.add( CommandFactory.newInsert( c.getGroup(), "group" ) );
		cmds.add( CommandFactory.newInsert( user, "user" ) );
		ExecutionResults results = kieSession.execute( CommandFactory.newBatchExecution( cmds ) );
//		kieSession.execute(c);
		
		Assertions.assertTrue(c.isOK());
	}

}
