package de.dlr.ivf.tapas.environment.dao.exception;

public class DaoUpdateException extends Exception{
    public DaoUpdateException(String s, Exception e) {
        super(s, e);
    }
}
