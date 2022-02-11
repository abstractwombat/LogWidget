package com.abstractwombat.loglibrary;

public class EmailSourceConfig extends LogSourceConfig {
	public String server;
    public int port;
    public String folder;
	public String username;
	public String password;
    private final int DEFAULT_IMAP_PORT = 143;

	public EmailSourceConfig(){
		super();
		this.server = "";
        this.port = DEFAULT_IMAP_PORT;
        this.username = "";
        this.password = "";
	}
	public EmailSourceConfig(LogSourceConfig config){
		this.sourceID = config.sourceID;
		this.groupID = config.groupID;
		this.count = config.count;
		this.lookupKeyFilter = config.lookupKeyFilter;
        this.server = "";
        this.port = DEFAULT_IMAP_PORT;
        this.folder = "";
        this.username = "";
        this.password = "";
	}
	public EmailSourceConfig(String sourceID, int groupID, int count){
		super(sourceID, groupID, count);
        this.server = "";
        this.port = DEFAULT_IMAP_PORT;
        this.folder = "";
        this.username = "";
        this.password = "";
	}
	
	@Override
	public String serialize(){
		String base = super.serialize();

		base += this.delimiter + this.server;
        base += this.delimiter + this.port;
        base += this.delimiter + this.folder;
        base += this.delimiter + this.username;
        //String encryptedPass = null;
        //try {
        //    encryptedPass = SimpleCrypto.encrypt("comabstractwombatlibrary", this.password);
        //} catch (Exception e) {
        //    encryptedPass = "";
        //    Log.d("EmailSourceConfig", "Encrypting password exception");
        //}
        //Log.d("EmailSourceConfig", "Encrypting password: " + this.password + " to " + encryptedPass);
        //base += this.delimiter + encryptedPass;
        base += this.delimiter + this.password;

		return base;
	}
	@Override
	public int unserialize(String s){
		int baseCount = super.unserialize(s);
		if (baseCount == 0){
			return 0;
		}
		
		String[] a = s.split(this.delimiter);
		if (a.length < baseCount+1) {
			return baseCount;
		}
		int i=baseCount;

        this.server = a[i++];
        this.port = Integer.parseInt(a[i++]);
        this.folder = a[i++];
        this.username = a[i++];
        this.password = a[i++];
        //String encryptedPass = a[i++];
        //try {
        //    this.password = SimpleCrypto.decrypt("comabstractwombatlibrary", encryptedPass);
        //} catch (Exception e) {
        //    this.password = "";
        //    Log.d("EmailSourceConfig", "Decrypting password exception");
        //}
        //Log.d("EmailSourceConfig", "Decrypting password: " + encryptedPass + " to " + this.password);

        return i;
	}

    @Override
    public String getSummary(){
        String o = "" + this.count + " items - " + this.username + " (" + this.server + ")";
        return o;
    }
}
