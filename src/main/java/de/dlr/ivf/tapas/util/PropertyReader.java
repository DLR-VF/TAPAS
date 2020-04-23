package de.dlr.ivf.tapas.util;

import java.io.*;
import java.util.Properties;

/**
* 
* @author Holger Siedel(edited)
* original code from Marco
*/

public class PropertyReader
{
    private static Properties properties;
    
    public static Properties getProperties(String propFileName) throws IOException
    {
        BufferedInputStream stream = null;
        
        if(properties == null)
        { 
            properties = new Properties();
            try
            {
                FileInputStream fileInputStream = new FileInputStream(propFileName);
                stream = new BufferedInputStream(fileInputStream);
                if(stream != null)
                    properties.load(stream);
            }
            catch(FileNotFoundException ex)
            {
                // todo write a new Property file
                throw ex;
            }
            finally
            {
                if (stream != null)
                {
                    try
                    {
                        stream.close();
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        }
        
        return properties;
    }
    
    public static String getProperty(String propFileName, String name) throws IOException
    {
        return getProperties(propFileName).getProperty(name);
    }
    
    public static void setProperty(String propFileName, String name, String value) throws IOException
    {
        getProperties(propFileName).setProperty(name, value);
        save(propFileName);
    }
    
    private static void save(String propFileName)throws IOException
    {
        FileOutputStream out = null;
        File file = new File(propFileName);
        try
        {
            if(!file.exists())
                file.createNewFile();
            
            out = new FileOutputStream(file);
            
            getProperties(propFileName).store(out, "Dateipfade");
        }
        finally
        {
            if(out != null)
                    out.close();
        }
    }
}
