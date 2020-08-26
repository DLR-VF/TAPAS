package de.dlr.ivf.tapas.persistence.db;

import de.dlr.ivf.tapas.persistence.TPS_PersistenceManager;
import de.dlr.ivf.tapas.util.parameters.ParamString;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.sql.SQLException;

public class TPS_PipedDbWriterConsumer implements Runnable{

    private TPS_DB_IOManager pm;
    private PipedInputStream is;
    private CopyManager cm;
    private final String copystring;


    public TPS_PipedDbWriterConsumer(TPS_DB_IOManager  pm){
        this.is = new PipedInputStream();
        this.pm = pm;
        this.copystring = String.format("COPY %s FROM STDIN (FORMAT csv, DELIMITER ;",pm.getParameters().getString(ParamString.DB_TABLE_TRIPS));
        init();
    }
    @Override
    public void run() {

        try {
            this.cm = new CopyManager(pm.getDbConnector().getConnection(this).unwrap(BaseConnection.class));
            cm.copyIn(copystring,is);
        } catch (IOException | SQLException e) {
            e.printStackTrace();
        }


    }

    public void init(){


    }

    public PipedInputStream getInputStream(){
        return this.is;
    }
}
