package com.abstractwombat.loglibrary;

public class CombinedLogSourceConfig extends LogSourceConfig {
	public String[] sources;
	
	public CombinedLogSourceConfig(){
		super();
		this.sources = null;
	}
	public CombinedLogSourceConfig(String sourceID, int widgetID, int count){
		super(sourceID, widgetID, count);
		this.sources = null;
	}
	public CombinedLogSourceConfig(String sourceID, int widgetID, int count, String[] sources){
		super(sourceID, widgetID, count);
		this.sources = sources;
	}
	
	@Override
	public String serialize(){
		String base = super.serialize();
		if (this.sources == null){
			base += this.delimiter + 0;
		}else{
			base += this.delimiter + this.sources.length;
			for (String s : this.sources){
				base += this.delimiter + s;
			}
		}
		return base;
	}
	@Override
	public int unserialize(String s){
		int baseCount = super.unserialize(s);
		if (baseCount == 0){
			return 0;
		}
		String[] a = split(s, this.delimiter, 0);
		if (a.length < baseCount+1) {
			return baseCount;
		}
		
		int i=baseCount;
		int sourceCount = Integer.parseInt(a[i++]);
		if (sourceCount > 0){
			this.sources = new String[sourceCount];
			for (int source=0; source<sourceCount; source++){
				this.sources[source] = a[i++];
			}
		}		
		return i;
	}
}
