package de.dlr.ivf.tapas.environment.dao.exception;


public class DaoReadException extends Exception{
    public DaoReadException(String s, Exception e) {
        super(s, e);
    }
}
