package uni.bonn.eula.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ClustersCreator {

	String identity;
	List<Set<Object>> permissions;
	List<Set<Object>> prohibitions;
	List<Set<Object>> duties;
	
	
	public void setPermissions(List<Set<Object>>  permissions){
		this.permissions = permissions;
	}
	
	public void setProhibitions(List<Set<Object>> prohibitions){
		this.prohibitions = prohibitions;
	}

	public void setDuties(List<Set<Object>>  duties){
		this.duties = duties;
	}
	
	public void setIdentity(String identity){
		this.identity = identity;
	}
		
	public List<Set<Object>> getPermissions(){
		return permissions;
	}
	
	public List<Set<Object>> getProhibitions(){
		return prohibitions;
	}

	public List<Set<Object>> getDuties(){
		return duties;
	}
	
	public String getIdentity(){
		return identity;
	}
}
