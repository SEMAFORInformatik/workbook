package ch.semafor.intens.ws.utils;

import ch.semafor.intens.ws.config.ComponentProperties;
import ch.semafor.intens.ws.model.ApprovalState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ApprovalStateConsistency {
    private static final Logger logger = LoggerFactory.getLogger(ApprovalStateConsistency.class);

    public static boolean isValid(ComponentProperties componentProperties, ApprovalState parent, ApprovalState child ) {
        logger.debug("Parent Approval State {} / Child Approval State: {}", parent.getName(), child.getName());
        final String key = "ApprovalStateConsistency." + parent.getName();
        String value = componentProperties.getProperty(key);
        if( value != null ) {
            List<String> inconsistent_approvals = new ArrayList<String>(Arrays.asList(value.split(",")));

            if(logger.isDebugEnabled()) {
                logger.debug("These child approval states are inconsistent for parent approval state {}:", parent.getName());
                for(String s : inconsistent_approvals) {
                    logger.debug(" - {}", s);
                }
            }
            if( inconsistent_approvals.contains(child.getName())) {
                logger.debug("Approval state of child {} was found: -> Update child to approval state of parent {}"
                             , child.getName(), parent.getName());
                return false;
            }
            logger.debug("Approval state of child was not found -> The child approval state is ok");
            return true;
        }
        return ApprovalStateConsistency.isValidWithoutConfig(parent, child);
    }

    public static boolean isSnapshot(ComponentProperties componentProperties, ApprovalState approvalState ) {
        final String key = "ApprovalStateConsistency.snapshots";
        String value = componentProperties.getProperty(key);
        if( value != null ) {
            List<String> snapshots_approvals = new ArrayList<String>(Arrays.asList(value.split(",")));
            return snapshots_approvals.contains(approvalState.getName());
        }
        return approvalState.isSnapshot();
    }

	static private boolean isValidWithoutConfig( ApprovalState parent, ApprovalState child ){
        /*
        Default check if the child has a consistent approval state

         */
        switch (parent) {
            case Approved:
                if( child == ApprovalState.Shared ||
                    child == ApprovalState.InPreparation ||
                    child == ApprovalState.Experimental ) {
                    return false;
                }
                break;

            case Tested:
                if( child == ApprovalState.Shared ||
                    child == ApprovalState.InPreparation ||
                    child == ApprovalState.Experimental  ||
                    child == ApprovalState.Approved ) {
                    return false;
                }
                break;

            case Tendered:
                if( child == ApprovalState.Shared ||
                    child == ApprovalState.InPreparation ||
                    child == ApprovalState.Experimental  ||
                    child == ApprovalState.Approved ||
                    child == ApprovalState.Tested ) {
                    return false;
                }
                break;
            // Other states are consistent by default
            default:
                break;
        }

		return true;
	}
}
