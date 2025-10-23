package ch.semafor.gendas.exceptions;

import org.springframework.dao.EmptyResultDataAccessException;

public class UsernameNotFoundException extends EmptyResultDataAccessException {
    /**
     *
     */
    private static final long serialVersionUID = -7580320200978774878L;

    public UsernameNotFoundException(final String msg) {
        super(msg, 1);
    }
}
