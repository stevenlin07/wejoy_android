package com.wejoy.module;
/**
 * 
 * @author WeJoy Group
 *
 */
public enum Sex {
	Mail(0),
	Femail(1),
	UNKNOWN(99);
	
	private final int value;
	
	private Sex(int value) {
		this.value = value;
	}
	
	public static Sex valueOf(int value){
		for( Sex type: Sex.values() ){
			if ( value == type.value ) return type;
		}
		return UNKNOWN;
	}
}
