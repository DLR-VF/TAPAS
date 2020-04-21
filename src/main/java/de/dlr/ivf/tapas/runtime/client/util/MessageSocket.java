package de.dlr.ivf.tapas.runtime.client.util;

import java.io.Serializable;
import java.util.List;

/**
 * Class for the MEssageSocket-System for the Visum-Connection
 * @author hein_mh
 *
 */
public class MessageSocket implements Serializable {   
	/**
	 * Serial id for this class
	 */
	private static final long serialVersionUID = -5263237504215376478L;
	private String id;
	private String text;   
	private String path;   
	private List<String> systems;
	private String visumInPath;
	private String visumOutPath;
	private String flag;
	
	/*
	public  MessageSocket(String id, String text, String path,List<String> systems, 
			String visumInPath, String visumOutPath, String flag){   
		
		this.id = id;
		this.text = text;   
		this.path = path;   
		this.systems = systems;
		this.visumInPath = visumInPath;
		this.visumOutPath = visumOutPath;
		this.flag = flag;
		
	}
	*/
	
	/**
	 * Constructor with a given id
	 * @param id The id for this MEssageSocket
	 */
	public MessageSocket(String id)
	{
		this.id = id;
	}
	
	/**
	 * Setter for the ID
	 * @param id
	 */
	public void setId(String id) {
		this.id= id;
	}

	/**
	 * Getter for the ID-String
	 * @return
	 */
	public String getId() {
		return id;
	}
	
	/**
	 * Sets the text command to transmit via the socket
	 * @param text
	 */
	public void setText(String text) {
		this.text = text;
	}

	/**
	 * Gets the text command 
	 * @return
	 */
	public String getText() {
		return text;
	}

	/**
	 * Sets the VISUM-Filename to load
	 * @param path should be a file not a path to the visum-version-file 
	 */
	public void setPath(String path) {
		this.path = path;
	}

	/**
	 * Gets the VISUM-Filename to load
	 */
	public String getPath() {
		return path;
	}
	
	/**
	 * Sets a List of traffic systems O/D-Matrices (WALK, BIKE, CAR)
	 * @param systems The Systems
	 */
	
	public void setSystems(List<String> systems) {
		this.systems = systems;
	}

	/**
	 * Gets the List of traffic systems
	 * @return
	 */
	public List<String> getSystems() {
		return systems;
	}   
	
	/**
	 * Sets the path for the input matrices
	 * @param visumInPath
	 */
	public void setVisumInPath(String visumInPath) {
		this.visumInPath = visumInPath;
	}

	/**
	 * Gets the path for the input matrices
	 * @return
	 */
	public String getVisumInPath() {
		return visumInPath;
	}
	
	/**
	 * Sets the path for the output matrices
	 * @param visumOutPath
	 */
	public void setVisumOutPath(String visumOutPath) {
		this.visumOutPath = visumOutPath;
	}

	/**
	 * Gets the path for the output matrices
	 * @return
	 */
	public String getVisumOutPath() {
		return visumOutPath;
	}
	
	/**
	 * Sets the server answer flag
	 * @param flag
	 */
	public void setFlag(String flag) {
		this.flag = flag;
	}

	/**
	 * Gets the server answer flag
	 * @return
	 */
	public String getFlag() {
		return flag;
	}
}   