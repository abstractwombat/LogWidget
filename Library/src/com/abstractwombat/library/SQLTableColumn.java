package com.abstractwombat.library;

/**
 *	Data column for use in the convenience wrapper for an SQL database; SQLDatabase.
 */
public class SQLTableColumn{
	public String name;	///< Column Name
	public String type;	///< Column Type (TEXT, INTEGER, REAL, BLOB)
	public SQLTableColumn(String name, String type){
		this.name = name;
		this.type = type;
	}
}


