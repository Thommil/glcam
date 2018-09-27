package com.thommil.animalsgo.gl;

import android.content.Context;

import com.thommil.animalsgo.R;
import com.thommil.animalsgo.gl.libgl.GlProgram;

import java.util.HashMap;
import java.util.Map;

public class PluginManager {

    private static final String TAG = "A_GO/PluginManager";

    private static PluginManager sPluginManagerInstance;

    private final Map<String, Plugin> mPluginsMap;
    private final Map<String, GlProgram> mProgramsMap;

    private final Context mContext;

    private PluginManager(final Context context){
        mPluginsMap = new HashMap<>();
        mProgramsMap = new HashMap<>();
        mContext = context;
        loadPlugins();
    }

    public static final PluginManager getInstance(final Context context){
        if(sPluginManagerInstance == null){
            sPluginManagerInstance = new PluginManager(context);
        }
        return sPluginManagerInstance;
    }

    public Plugin getPlugin(final String pluginId){
        return mPluginsMap.get(pluginId);
    }

    private void loadPlugins(){
        //Log.d(TAG, "allocate()");
        try {
            for (final String pluginClassname : mContext.getResources().getStringArray(R.array.plugins_list)) {
                final Class pluginClass = this.getClass().getClassLoader().loadClass(pluginClassname);
                final Plugin plugin = (Plugin) pluginClass.newInstance();
                plugin.setContext(mContext);
                //Log.d(TAG, "Plugin "+ plugin.getId()+" created");
                mPluginsMap.put(plugin.getId(), plugin);
            }
        }catch(ClassNotFoundException cne){
            throw new RuntimeException("Missing plugin class : "+cne);
        }catch(InstantiationException ie){
            throw new RuntimeException("Failed to instanciate plugin : "+ie);
        }catch(IllegalAccessException iae){
            throw new RuntimeException("Failed to instanciate plugin : "+iae);
        }
    }

    public void allocate(final int filter, final float surfaceRatio){
        //Log.d(TAG, "destroy()");
        for(final Plugin plugin : mPluginsMap.values()){
            if((plugin.getType() & filter) > 0){
                if(mProgramsMap.containsKey(plugin.getProgramId())){
                    plugin.setProgram(mProgramsMap.get(plugin.getProgramId()));
                    plugin.allocate(surfaceRatio);
                }
                else{
                    plugin.allocate(surfaceRatio);
                    mProgramsMap.put(plugin.getProgramId(), plugin.getProgram());
                }

            }
        }
    }


    public void free(){
        //Log.d(TAG, "destroy()");
        for(final Plugin plugin : mPluginsMap.values()){
            plugin.free();
        }
        for(final GlProgram program : mProgramsMap.values()){
            program.free();
        }
        mProgramsMap.clear();
    }

}
