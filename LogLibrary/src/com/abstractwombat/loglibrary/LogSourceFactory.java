package com.abstractwombat.loglibrary;

import android.app.Fragment;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.util.Log;

import com.abstractwombat.library.SharedPreferenceList;

import java.util.Vector;

public class LogSourceFactory {
	private static final String TAG = "LogSourceFactory";
	private static final String PREFERENCE_FILENAME = "Sources";
	private static final String SOURCE_TYPE_LIST = "SourceTypes";
	private static final String CONFIG_TYPE_DELIMITER = "%&^$";
	private static LogSourceMeta[] sourceMeta;
	
	public static LogSourceMeta[] getLogSourceMeta(Context context){
		loadMetaData(context);
		return sourceMeta;
	}
	
	public static int getSourceIcon(Context context, String sourceID){
		ALogSource source = get(context, sourceID);
		loadMetaData(context);
		String className = null;
		if (source != null) {
			className = source.getClass().getName();
		}
		for (LogSourceMeta meta : sourceMeta){
			if (meta.className.equals(className)){
				return meta.iconRes;
			}
		}
		return 0;
	}
	public static String getSourceLabel(Context context, String sourceID){
		ALogSource source = get(context, sourceID);
		loadMetaData(context);
		String className = null;
		if (source != null) {
			className = source.getClass().getName();
		}
		for (LogSourceMeta meta : sourceMeta){
			if (meta.className.equals(className)){
				return meta.label;
			}
		}
		return "";
	}

    public static ALogSourcePreferenceFragment getSourceFragment(Context context, String sourceID){
        ALogSource source = get(context, sourceID);
        loadMetaData(context);
		String className = null;
		if (source != null) {
			className = source.getClass().getName();
		}
		for (LogSourceMeta meta : sourceMeta){
            if (meta.className.equals(className)){
                Log.d(TAG, "Found class " + className + " in meta data");
                return getSourceFragment(context, meta.id, source.config().sourceID);
            }
        }
        return null;
    }

	public static ALogSourcePreferenceFragment getSourceFragment(Context context, int metaID, String sourceID){
		Class<?> c = getSourceFragmentClass(context, metaID);
		try{
			Fragment f = null;
			if (c != null) {
				f = (Fragment)c.newInstance();
			}
			Bundle args = new Bundle();
			args.putString("sourceid", sourceID);
			if (f != null) {
				f.setArguments(args);
			}
			Log.d(TAG, "Instantiated source fragment with ID " + sourceID);
			return (ALogSourcePreferenceFragment)f;
		} catch (InstantiationException x) {
			x.printStackTrace();
		} catch (IllegalAccessException x) {
			x.printStackTrace();
		}
		return null;
	}

    public static int getTotalViewTypes(Context context){
        loadMetaData(context);
        int count = 0;
        for (LogSourceMeta meta : sourceMeta){
            count += meta.viewTypes;
        }
        return count;
    }
	
	private static Class<?> getSourceFragmentClass(Context context, int metaID){
		loadMetaData(context);
		for (LogSourceMeta meta : sourceMeta){
			if (meta.id == metaID){
				Class<?> c;
				try {
					c = Class.forName(meta.fragmentName);
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
					return null;
				}
				return c;
			}
		}
		return null;
	}

	private static void loadMetaData(Context context){
		if (sourceMeta != null && sourceMeta.length > 0) return;
		
		// Get the resource arrays
		Resources res = context.getResources();
		TypedArray idArray = res.obtainTypedArray(R.array.source_id_array);
		String[] labelArray = res.getStringArray(R.array.source_label_array);
		String[] classArray = res.getStringArray(R.array.source_class_array);
		String[] configClassArray = res.getStringArray(R.array.source_config_class_array);
		String[] fragmentArray = res.getStringArray(R.array.source_fragment_array);
		TypedArray iconArray = res.obtainTypedArray(R.array.source_icon_array);
        int[] viewTypeArray = res.getIntArray(R.array.source_view_types);

		// Load into the array of LogSourceMeta objects
		sourceMeta = new LogSourceMeta[idArray.length()];
		for (int i=0; i<idArray.length(); i++){
			LogSourceMeta meta = new LogSourceMeta();
			meta.id = idArray.getResourceId(i, -1);
			meta.label = labelArray[i];
			meta.className = classArray[i];
			meta.configClassName = configClassArray[i];
			meta.fragmentName = fragmentArray[i];
			meta.iconRes = iconArray.getResourceId(i, -1);
            meta.viewTypes = viewTypeArray[i];
			sourceMeta[i] = meta;
		}
	}
	
	/**
	 *	Create a new ALogSource object that will persist on disc. 
	 */
	public static ALogSource newSource(Context context, Class<?> aLogSourceClass, LogSourceConfig config){
		String sourceType = aLogSourceClass.getName();
		Log.d(TAG, "Adding (type=" + sourceType + ", source id=" + config.sourceID + ", group id=" + config.groupID + ")");

		// Add this Class to the source type list
		SharedPreferenceList prefs = new SharedPreferenceList(context, sharedPrefFile());
		prefs.addTo(SOURCE_TYPE_LIST, sourceType);

		// Add the configuration to this type's Config list
		String configType = config.getClass().getName();
		prefs.addTo(sourceType, configType + CONFIG_TYPE_DELIMITER + config.serialize());
		
		// Return a new object
		return instantiateSource(context, sourceType, config);
	}

	/**
	 *	Get the ALogSource object with the given source id.
	 */
	public static ALogSource get(Context context, String sourceID){
		Log.d(TAG, "Getting source ID: " + sourceID);
		SharedPreferenceList prefs = new SharedPreferenceList(context, sharedPrefFile());
		String[] types = prefs.get(SOURCE_TYPE_LIST);
		for (String type : types){
			String[] configs = prefs.get(type);
			if (configs == null){
				Log.d(TAG, "No configs found for type: " + type);
				continue;
			}
			for (String configString : configs){
				LogSourceConfig config = instantiateConfig(configString);
				if (config.sourceID.equals(sourceID)){
					Log.d(TAG, "Found source ID: " + sourceID + " (of type: " + type + ")");
					return instantiateSource(context, type, config);
				}
			}
		}
		return null;
	}
	
	/**
	 *	Get all the ALogSource objects.
	 */
	public static ALogSource[] get(Context context){
		SharedPreferenceList typeList = new SharedPreferenceList(context, sharedPrefFile());
		String[] types = typeList.get(SOURCE_TYPE_LIST);
		Log.d(TAG, "Get (" + types.length + " types found)");
		Vector<ALogSource> sources = new Vector<ALogSource>();
		for (String type : types){
			Class<?> c = null;
			try{
				c = Class.forName(type);
			} catch (ClassNotFoundException x){
				x.printStackTrace();
				continue;
			}
			ALogSource[] typeSources = get(context, c);
			Log.d(TAG, " - " + type + " (" + typeSources.length + " sources found)");
			for (ALogSource source : typeSources){
				if (source != null){
					sources.add(source);
				}
			}
		}
		ALogSource[] sa = new ALogSource[sources.size()];
		return sources.toArray(sa);		
	}

	/**
	 *	Get the ALogSource objects of the given type.
	 */
	public static ALogSource[] get(Context context, Class<?> aLogSourceClass){
		SharedPreferenceList prefs = new SharedPreferenceList(context, sharedPrefFile());
		String typeName = aLogSourceClass.getName();
		Log.d(TAG, "Get(Class) - typename=" + typeName);
		String[] configs = prefs.get(typeName);
		if (configs == null){
			return null;
		}
		Vector<ALogSource> sources = new Vector<ALogSource>();
		for (String configString : configs){
			LogSourceConfig config = instantiateConfig(configString);
			sources.add(instantiateSource(context, typeName, config));
		}
		ALogSource[] sa = new ALogSource[sources.size()];
		return sources.toArray(sa);
	}	
	
	/**
	 *	Get all the ALogSource objects with the given group id.
	 */
	public static ALogSource[] get(Context context, int groupID){
		SharedPreferenceList prefs = new SharedPreferenceList(context, sharedPrefFile());
		Vector<ALogSource> sources = new Vector<ALogSource>();
		String[] types = prefs.get(SOURCE_TYPE_LIST);
		for (String type : types){
			String[] configs = prefs.get(type);
			if (configs == null){
				continue;
			}
			for (String configString : configs){
				LogSourceConfig config = instantiateConfig(configString);
				if (config.groupID == groupID){
					sources.add(instantiateSource(context, type, config));
				}
			}
		}
		ALogSource[] sa = new ALogSource[sources.size()];
		return sources.toArray(sa);
	}

	/**
	 *	Deletes all sources.
	 */
	public static void delete(Context context){
		SharedPreferenceList prefs = new SharedPreferenceList(context, sharedPrefFile());
		String[] types = prefs.get(SOURCE_TYPE_LIST);
		if (types == null) return;
		for (String type : types){
			prefs.remove(type);
		}
		prefs.remove(SOURCE_TYPE_LIST);
	}
	
	/**
	 *	Deletes the source with the given source ID.
	 */
	public static void deleteSource(Context context, String sourceID){
		Log.d(TAG, "Removing (source id=" + sourceID + ")");

		SharedPreferenceList prefs = new SharedPreferenceList(context, sharedPrefFile());
		String[] types = prefs.get(SOURCE_TYPE_LIST);
		for (String type : types){
			String[] configs = prefs.get(type);
			if (configs == null){
				Log.d(TAG, "No configs found for type: " + type);
				continue;
			}
			for (String configString : configs){
				LogSourceConfig config = instantiateConfig(configString);
				if (config.sourceID.equals(sourceID)){
					Log.d(TAG, "Found source ID: " + sourceID + " (of type: " + type + ")");
					// Remove this config string from this type's list
					prefs.removeFrom(type, configString);
					String[] configPost = prefs.get(type);
					if (configPost.length == 0){
						// Remove the type if it's empty
						prefs.removeFrom(SOURCE_TYPE_LIST, type);
					}
				}
			}
		}
	}
	
	/**
	 *	Deletes all the sources of the given type.
	 */
	public static void deleteSource(Context context, Class<?> aLogSourceClass){
		String sourceType = aLogSourceClass.getName();
		Log.d(TAG, "Removing (type=" + sourceType + ")");

		// Remove this Class from the source type list
		SharedPreferenceList prefs = new SharedPreferenceList(context, sharedPrefFile());
		prefs.removeFrom(SOURCE_TYPE_LIST, sourceType);

		// Remove the Config list
		prefs.remove(sourceType);
	}

	/**
	 *	Deletes all the sources with the given group ID.
	 */
	public static void deleteGroup(Context context, int groupID){
		SharedPreferenceList prefs = new SharedPreferenceList(context, sharedPrefFile());

		String[] types = prefs.get(SOURCE_TYPE_LIST);
		if (types == null) return;
		for (String type : types){
			String[] configs = prefs.get(type);
			if (configs == null){
				continue;
			}
			for (String configString : configs){
				LogSourceConfig config = instantiateConfig(configString);
				if (config.groupID == groupID){
					prefs.removeFrom(type, configString);
				}
			}
		}
	}
	
	/**
	 * Instantiates an ALogSource of the given type (represented by a string) using the given configuration.
	 */
	private static ALogSource instantiateSource(Context context, String className, LogSourceConfig config){
		try{
			Class<?> c = Class.forName(className);
			return instantiateSource(context, c, config);
		} catch (ClassNotFoundException x){
			x.printStackTrace();
		}
		return null;
	}

	/**
	 * Instantiates an ALogSource of the given type using the given configuration.
	 */
	private static ALogSource instantiateSource(Context context, Class<?> aLogSourceClass, LogSourceConfig config){
		try{
			ALogSource source = (ALogSource)aLogSourceClass.newInstance();
			source.config(context, config);
			return source;
		} catch (InstantiationException x) {
			x.printStackTrace();
		} catch (IllegalAccessException x) {
			x.printStackTrace();
		}
		return null;
	}	

	/**
	 * Instantiates a LogSourceConfig using the given string.
	 */
	private static LogSourceConfig instantiateConfig(String configString){
		int i = configString.indexOf(CONFIG_TYPE_DELIMITER);
		String configType = configString.substring(0, i);
		String configParams = configString.substring(i+CONFIG_TYPE_DELIMITER.length());
		LogSourceConfig config = new LogSourceConfig();
		try{
			Class<?> c = Class.forName(configType);
			config = (LogSourceConfig)c.newInstance();
		} catch (ClassNotFoundException x){
			x.printStackTrace();
			return config;
		} catch (InstantiationException x) {
			x.printStackTrace();
			return config;
		} catch (IllegalAccessException x) {
			x.printStackTrace();
			return config;
		}
		if (config.unserialize(configParams.trim()) == 0){
			Log.d(TAG, "Failed to unserialize string: " + configString);
		}
		return config;
	}
	
	private static String sharedPrefFile(){
		return PREFERENCE_FILENAME;
	}
}