package de.dlr.ivf.tapas.runtime;

import de.dlr.ivf.tapas.util.parameters.ParamString;
import de.dlr.ivf.tapas.util.parameters.ParamValue;
import de.dlr.ivf.tapas.util.parameters.TPS_ParameterClass;

import java.util.Optional;

public class TapasLogin {

    public static Optional<TapasLogin> fromParameterClass(TPS_ParameterClass parameters){

        String user = parameters.getString(ParamString.DB_USER);
        String pw = parameters.getString(ParamString.DB_PASSWORD);
        String host = parameters.getString(ParamString.DB_HOST);
        String database = parameters.getString(ParamString.DB_DBNAME);
        String dbtype = parameters.getString(ParamString.DB_TYPE);
        int port = parameters.getIntValue(ParamValue.DB_PORT);

        boolean isValid = user != null &&
                          pw != null &&
                          host != null &&
                          database != null &&
                          dbtype != null;

        if(!isValid){
            return Optional.empty();
        }else{
            return  Optional.of(new TapasLogin(user,pw,host, database, dbtype, port));
        }
    }


    private final String user;
    private final String password;
    private final String host;
    private final String database;
    private final String db_type;
    private final int port;


    private TapasLogin(String user, String password, String host, String database, String db_type, int port){
        this.user= user;
        this.password = password;
        this.host = host;
        this.database = database;
        this.db_type = db_type;
        this.port = port;
    }

    public String getUser(){
        return this.user;
    }

    public String getPassword(){
        return this.password;
    }

    public String getHost(){
        return this.host;
    }

    public String getDbType(){
        return this.db_type;
    }

    public String getDatabase(){
        return this.database;
    }

    public int getPort(){
        return this.port;
    }
}
