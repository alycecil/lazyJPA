package org.gradle

import javax.persistence.Cacheable;
import javax.persistence.Entity;
import javax.persistence.Table;

def sql = """
 
create table schema.table 
(
	id int identity(1,1),
	name varchar(255) NOT NULL,
	last_started_date DATETIME
)
go

"""
static String toCamelCase(String s){
	String[] parts = s.split("_");
	def camelCaseString = "";
	boolean first = true;
	for (String part : parts){
		camelCaseString = camelCaseString + toProperCase(part, first);
		first = false
	}

	return camelCaseString;
}
static String toProperCase(String s){
	String[] parts = s.split("_");
	def camelCaseString = "";
	for (String part : parts){
		camelCaseString+=toProperCase(part,false);
	}
	return camelCaseString
}
static String toProperCase(String s, boolean first) {
	if(first){
		return s.toLowerCase();
	}
	return s.substring(0, 1).toUpperCase() +
			s.substring(1).toLowerCase();
}

def writeColumn(String name, String type){
	def objectType = 'String';
	def columnDetails = '';
	
	type = type.toLowerCase()
	
	if(name.equals('id')){
		println "	@Id"
		println "	@GeneratedValue(strategy=GenerationType.IDENTITY)"
		objectType = 'Long'
		columnDetails = ', updatable = false'
	}
	
	if(type.equalsIgnoreCase('datetime')){
		println "	@Temporal(TemporalType.TIMESTAMP)"
		objectType = 'Date'
	}else if(type.equalsIgnoreCase('int')){
		objectType = 'Long'
	}
	
	println """
	//raw type : $type
	@Column(name="$name"$columnDetails)
	private $objectType ${toCamelCase(name)};"""
}

boolean inClass = false;
def endOfClass = """
}
""";
int depth=0;
boolean inConstaint = false
sql.eachLine { String it ->

	it = it.trim().toLowerCase()

	if(it.startsWith("create table")){
		if(inClass){
			println endOfClass
		}
		inClass = true;inConstaint = false
		it = it.replace("create table", "")
		def splits = it.split("\\.")
		def name = splits.length > 1 ? splits[1].trim() : splits[0].trim();
		def schema = splits.length > 1 ? splits[0].trim() : null;
		println """
import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(${schema!=null? "schema = \"$schema\",": ""
			} name = "$name")
@Cacheable(true)
create class ${
				toProperCase(name)
			} {
"""
	}else if(it.startsWith('(')){
		depth++;
	}else if(it.startsWith(')')){
		depth--;
		if(depth == 0 ){
			inConstaint = false;
		}
	}else if(it.startsWith('constraint')){
		inConstaint = true;
		if(it.endsWith(',')){
			inConstaint = false;
		}
	}else if(!it.isEmpty()){
		if(depth > 0){
			if(inConstaint){
				if(it.endsWith(',')){
					inConstaint = false;
				}
			}else{
				if(!it.startsWith("--")){
					splits = it.split("\\s+")
					if(splits.length>1){
						def name = splits[0];
						def type = splits[1];
						
						writeColumn(name, type); 
					}
				}
			}
		}
	}
}

if(inClass) println endOfClass
