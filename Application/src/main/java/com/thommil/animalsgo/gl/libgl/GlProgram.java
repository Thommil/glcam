package com.thommil.animalsgo.gl.libgl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.opengl.GLES20;
import android.opengl.GLException;

/**
 * Helper class to get shaders source code and link program
 * <br/><br/>
 *
 *  This approach allows automatic linking based on parsing. 
 *  
 * @author Thomas MILLET
 *
 */
public class GlProgram {

	/**
	 * TAG log
	 */
	@SuppressWarnings("unused")
	private static final String TAG = "A_GO/GlProgram";
	
	/**
	 * Handle to use to unbind current program
	 */
	public static final int UNBIND_HANDLE = GLES20.GL_NONE;
	
	/**
	 * Attribute pattern for parsing
	 */
	private static final Pattern ATTRIBUTE_PATTERN = Pattern.compile("attribute\\s*[^\\s]*\\s*([^\\s;]*)\\s*;",Pattern.MULTILINE|Pattern.DOTALL);
	
	/**
	 * Uniform pattern for parsing
	 */
	private static final Pattern UNIFORM_PATTERN = Pattern.compile("uniform\\s*[^\\s]*\\s*([^\\s;]*)\\s*;",Pattern.MULTILINE|Pattern.DOTALL);

	/**
	 * Handle on current program
	 */
	public final int programHandle;
	
	/**
	 * Handle of the vertex mProgram
	 */
	public final int vertexShaderHandle;
	
	/**
	 *  Handle of the fragment mProgram
	 */
	public final int fragmentShaderHandle;
	
	/**
	 *  Handles on GSGL attributes
	 */
	private final Map<String,Integer> mAttributeHandles;
	
	/**
	 *  Store the attributes handles for direct access
	 */
	private int[] mAttributeHandlesArray;
	
	/**
	 *  Handles on GSGL uniforms
	 */
	private final Map<String,Integer> mUniformHandles;
	
	/**
	 * Constructor, creates and link program based on specified shaders
	 * 
	 * @param vertexShaderInputStream The vertex mProgram inputstream
	 * @param fragmentShaderInputStream The fragment mProgram inputstream
	 * @throws GLException
	 */
	public GlProgram(final InputStream vertexShaderInputStream, final InputStream fragmentShaderInputStream){
		this.mAttributeHandles = new HashMap<>();
		this.mUniformHandles = new HashMap<>();
		this.vertexShaderHandle = this.loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderInputStream);
		this.fragmentShaderHandle = this.loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderInputStream);
		this.programHandle = this.createAndLinkProgram(vertexShaderHandle, fragmentShaderHandle);
	}
	
	/**
	 * Enables all attributes of vertex mProgram
	 */
	public GlProgram enableAttributes(){
		for(int handle : this.mAttributeHandlesArray){
			this.enableAttribute(handle);
		}
		return this;
	}
		
	/**
	 * Enables mProgram attribute by its name
	 * 
	 * @param attributeName The name of the attribute to enable
	 */
	public GlProgram enableAttribute(final String attributeName){
		////Log.d(TAG,"enableAttribute("+attributeName+")");
		GLES20.glEnableVertexAttribArray(this.getAttributeHandle(attributeName));
		return this;
	}
	
	/**
	 * Enables mProgram attribute by its handle
	 * 
	 * @param attributeId The handle of attribute to enable
	 */
	public GlProgram enableAttribute(final int attributeId){
		////Log.d(TAG,"enableAttribute("+attributeId+")");
		GLES20.glEnableVertexAttribArray(attributeId);
		return this;
	}
	
	/**
	 * Disables all attributes of vertex mProgram
	 */
	public GlProgram disableAttributes(){
		for(int handle : this.mAttributeHandlesArray){
			this.disableAttribute(handle);
		}
		return this;
	}
	
	/**
	 * Disables mProgram attribute by its name
	 * 
	 * @param attributeName The name of attribute to disable
	 */
	public GlProgram disableAttribute(final String attributeName){
		////Log.d(TAG,"enableAttribute("+attributeName+")");
		GLES20.glDisableVertexAttribArray(this.getAttributeHandle(attributeName));
		return this;
	}
	
	/**
	 * Disables mProgram attribute by its handle
	 * 
	 * @param attributeId The handle of attribute to disable
	 */
	public GlProgram disableAttribute(final int attributeId){
		////Log.d(TAG,"enableAttribute("+attributeId+")");
		GLES20.glDisableVertexAttribArray(attributeId);
		return this;
	}
	
	/**
	 * Enabled the program on pipeline.<br/>
	 * <br/>
	 * <br/>
	 * Caution : only calls to start/stop are taken into account for current state
	 */
	public GlProgram use(){
		////Log.d(TAG,"start()");
        GLES20.glUseProgram(this.programHandle);
        return this;
	}
	
	/*
     * Simple loader used to get shaders from specified location
     * 
     * @param type The vertex mProgram type GLES20.GL_VERTEX_SHADER | GLES20.GL_FRAGMENT_SHADER
     * @param inputStream The mProgram code InputStream
     * @return return a compiled mProgram OpenGL ID
     */
	protected int loadShader(final int type, final InputStream inputStream){
		////Log.d(TAG,"loadShader("+type+")");
		
		final int shader = GLES20.glCreateShader(type);
		
		BufferedReader bufIn = null;
	    
	    try{
	    	bufIn = new BufferedReader(new InputStreamReader(inputStream), 8192);

	    	final StringBuilder shaderCode = new StringBuilder();
	    	String line = null;
	    	while((line = bufIn.readLine()) != null){
	    		shaderCode.append(line).append("\n");
	    	}

	    	// add the source code to the mProgram and compile it
	    	GLES20.glShaderSource(shader, shaderCode.toString());
	    	GLES20.glCompileShader(shader);
	    	
	    	// Get the compilation status.
	        final int[] compileStatus = new int[1];
	        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0);
	     
	        // If the compilation failed, delete the mProgram.
	        if (compileStatus[0] == GLES20.GL_FALSE) {
	            GLES20.glDeleteShader(shader);
	            throw new GLException(GLES20.GL_INVALID_OPERATION, "Failed to compile "+((type == GLES20.GL_VERTEX_SHADER)? "vertex":"fragment")+" mProgram");
	        }
	        
	        //Parse mProgram to store attributes and uniform handles
	    	final Matcher attributeMatcher = ATTRIBUTE_PATTERN.matcher(shaderCode);
	    	while(attributeMatcher.find()){
	    		final String attributeName = attributeMatcher.group(1);
	    		if(!this.mAttributeHandles.containsKey(attributeName)){
	    			this.mAttributeHandles.put(attributeName, UNBIND_HANDLE);
	    		}
	    	}
	    	final Matcher uniformMatcher = UNIFORM_PATTERN.matcher(shaderCode);
	    	while(uniformMatcher.find()){
	    		final String uniformName = uniformMatcher.group(1);
	    		if(!this.mUniformHandles.containsKey(uniformName)){
	    			this.mUniformHandles.put(uniformName, UNBIND_HANDLE);
	    		}
	    	}
	        
	    }catch(IOException ioe){
	    	throw new GLException(GLES20.GL_INVALID_OPERATION, "Failed to compile "+((type == GLES20.GL_VERTEX_SHADER)? "vertex":"fragment")+" mProgram");
	    }	
		
		return shader;
	}
	
    
    /**
	 * Helper function to compile and link a program
	 * 
	 * @param vertexShaderHandle An OpenGL handle to an already-compiled vertex mProgram.
	 * @param fragmentShaderHandle An OpenGL handle to an already-compiled fragment mProgram.
	 * @return An OpenGL handle to the program.
	 */
	protected int createAndLinkProgram(final int vertexShaderHandle, final int fragmentShaderHandle){
		////Log.d(TAG,"createAndLinkProgram("+vertexShaderHandle+", "+fragmentShaderHandle+")");
		final int programHandle = GLES20.glCreateProgram();
		
		if (programHandle != UNBIND_HANDLE) {
			// Bind the vertex mProgram to the program.
			GLES20.glAttachShader(programHandle, vertexShaderHandle);			

			// Bind the fragment mProgram to the program.
			GLES20.glAttachShader(programHandle, fragmentShaderHandle);
			
			// Link the two shaders together into a program.
			GLES20.glLinkProgram(programHandle);

			// Get the link status.
			final int[] linkStatus = new int[1];
			GLES20.glGetProgramiv(programHandle, GLES20.GL_LINK_STATUS, linkStatus, 0);

			// If the link failed, delete the program.
			if (linkStatus[0] == GLES20.GL_FALSE) {				
				final String error = GLES20.glGetProgramInfoLog(programHandle);
				GLES20.glDeleteProgram(programHandle);
				throw new GLException(GLES20.GL_INVALID_OPERATION, "Failed to link program : "+error);
			}
			
			//Update handles and fill the attributes cache
			int index=0;
			this.mAttributeHandlesArray = new int[this.mAttributeHandles.size()];
			for(String attributeName : this.mAttributeHandles.keySet()){
				final int attributeHandle = GLES20.glGetAttribLocation(programHandle, attributeName);
				this.mAttributeHandles.put(attributeName, attributeHandle);
				this.mAttributeHandlesArray[index++] = attributeHandle;
			}
			for(String uniformName : this.mUniformHandles.keySet()){
				final int uniformHandle = GLES20.glGetUniformLocation(programHandle, uniformName);
				this.mUniformHandles.put(uniformName, uniformHandle);
			}
		}
		
		return programHandle;
	}
	
	
	/**
	 * Free resources
	 */
	public GlProgram free(){
		////Log.d(TAG,"free()");
		if(this.programHandle != UNBIND_HANDLE) {
			GLES20.glDeleteProgram(this.programHandle);
		}
		if(this.vertexShaderHandle != UNBIND_HANDLE) {
			GLES20.glDeleteShader(this.vertexShaderHandle);
		}
		if(this.fragmentShaderHandle != UNBIND_HANDLE) {
			GLES20.glDeleteShader(this.fragmentShaderHandle);
		}
        GlOperation.checkGlError(TAG, "glDeleteTextures");
		return this;
	}
	 
	/**
	 * Get the handle of a specified attribute
	 * 
	 * @param name The attribute name
	 * @return The handle ID, 0 if not found
	 */
	public int getAttributeHandle(final String name){
		////Log.d(TAG,"getAttributeHandle("+name+")");
		Integer handle = this.mAttributeHandles.get(name);
		return (handle == null) ? UNBIND_HANDLE : handle;
	}
	
	/**
	 * Get the handle of a specified uniform
	 * 
	 * @param name The uniform name
	 * @return The handle ID, 0 if not found
	 */
	public int getUniformHandle(final String name){
		////Log.d(TAG,"getUniformHandle("+name+")");
		Integer handle = this.mUniformHandles.get(name);
		return (handle == null) ? UNBIND_HANDLE : handle;
	}
	
}
