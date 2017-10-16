package uni.bonn.eula.model;

import uni.bonn.eula.lib.AnnotEnum;

public class PolicyStructure {
	
	private String mainGrant;
	private AnnotEnum policyType;
	private String condition;
	private String grantType;
	private String actions;
	private String object;
	private String remainder;
	private String sentence;
	private PolicyStructure relatedPermission;
	private PolicyStructure relatedDuty;

	
	public PolicyStructure(String mainGrant, AnnotEnum policyType, String remainder, String condition, String grantType, String actions, String object, String sentence,  PolicyStructure relatedPermission, PolicyStructure relatedDuty){
		this.mainGrant = mainGrant;
		this.policyType = policyType;
		this.remainder = remainder;
		this.condition = condition;
		this.actions = actions;
		this.object = object;
		this.sentence = sentence;
		this.relatedPermission = relatedPermission;
		this.relatedDuty = relatedDuty;
		this.grantType = grantType;
	}
	
	public PolicyStructure(){
		
	}
	
	public String getMainGrant() {
		return mainGrant.replaceAll("\\n", "");
	}
	public void setMainGrant(String mainGrant) {
		this.mainGrant = mainGrant;
	}
	
	
	public String getAnnotation(boolean conditionIncluded, boolean grantTypeIncluded){
		
		String wholeAnnotation = "";
		if (conditionIncluded && grantTypeIncluded) {
			wholeAnnotation = (condition.replaceAll("\\n", "") + " " + grantType.replaceAll("\\n", "") + " " + mainGrant.replaceAll("\\n", "")).trim();
		} else if (!conditionIncluded && grantTypeIncluded) {
			wholeAnnotation = (grantType.replaceAll("\\n", "") + " " + mainGrant.replaceAll("\\n", "")).trim();
		} else if (conditionIncluded && !grantTypeIncluded) {
			wholeAnnotation = (condition.replaceAll("\\n", "") + " " + mainGrant.replaceAll("\\n", "")).trim();
		} else {
			wholeAnnotation = mainGrant.trim();
		}
		
		if(wholeAnnotation.toLowerCase().startsWith("and")){
			wholeAnnotation = wholeAnnotation.substring(3 , wholeAnnotation.length());
		}
		if(wholeAnnotation.toLowerCase().startsWith("or")){
			wholeAnnotation = wholeAnnotation.substring(2 , wholeAnnotation.length());
		}

		//wholeAnnotation = wholeAnnotation.replaceAll("\\n", "");		
		return wholeAnnotation.replaceAll("\\n", "");
	}
	
	
	public String getCondition() {
		return condition.replaceAll("\\n", "");
	}
	public void setCondition(String condition) {
		this.condition = condition;
	}
	
	public String getGrantType() {
		return grantType.replaceAll("\\n", "");		
	}
	public void setGrantType(String grantType) {
		this.grantType = grantType;
	}
	
	
	public String getRemainder() {
		return remainder;
	}
	public void setRemainder(String remainder) {
		this.remainder = remainder;
	}
	
	public String getActions() {
		return actions;
	}
	public void setActions(String actions) {
		this.actions = actions;
	}
		
	public String getSentence() {
		return sentence;
	}
	public void setSentence(String sentence) {
		this.sentence = sentence;
	}
	
	public String getObject() {
		return object;
	}
	public void setObject(String object) {
		this.object = object;
	}

	public AnnotEnum getPolicyType(){
		return policyType;
	}
	public void setPolicyType(AnnotEnum policyType) {
		this.policyType = policyType;
	}
	
	public PolicyStructure getRelatedPermission(){
		return relatedPermission;
	}
	public void setRelatedPermission(PolicyStructure relatedPermission){
		this.relatedPermission = relatedPermission;
	}

	public PolicyStructure getRelatedDuty(){
		return relatedDuty;
	}
	public void setRelatedDuty(PolicyStructure relatedDuty){
		this.relatedDuty = relatedDuty;
	}
	

}
