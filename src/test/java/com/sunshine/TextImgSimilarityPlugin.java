package com.sunshine;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.index.LeafReaderContext;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.plugins.ScriptPlugin;
import org.elasticsearch.script.ScoreScript;
import org.elasticsearch.script.ScoreScript.LeafFactory;
import org.elasticsearch.script.ScriptContext;
import org.elasticsearch.script.ScriptEngine;

/**
 * 文本和图像相似度计算
 */
public class TextImgSimilarityPlugin extends Plugin implements ScriptPlugin {

    @Override
    public ScriptEngine getScriptEngine(Settings settings, Collection<ScriptContext<?>> contexts) {
        return new MyNameImgScriptEngine();
    }

    // tag::name_img_engine
    private static class MyNameImgScriptEngine implements ScriptEngine {
        @Override
        public String getType() {
            return "name_img_scripts";
        }

        @Override
        public <T> T compile(String scriptName, String scriptSource, ScriptContext<T> context, Map<String, String> param) {
            if (context.equals(ScoreScript.CONTEXT) == false) 
                throw new IllegalArgumentException(getType() + " scripts cannot be used for context [" + context.name + "]");
            
            if (param.containsKey("name") == false) {
                throw new IllegalArgumentException("Missing parameter [name]");
            }
            // we use the script "source" as the script identifier
            if ("name_img_similarity".equals(scriptSource)) {
                ScoreScript.Factory factory = (params, lookup) -> new LeafFactory(){
                	
					@Override
					public boolean needs_score() {
						return false;
					}

					@Override
					public ScoreScript newInstance(LeafReaderContext ctx) throws IOException {
						String name = params.get("name").toString();
						
						return new ScoreScript(params, lookup, ctx) {              	                	
                            @Override
                            public double execute(ExplanationHolder explanation) {
                            	System.out.println("let me look2:"+lookup.source().get("name"));
                            	System.out.println("param I inputed is:"+name);
                                return 1.0;
                            }
                        };
					}};
                return context.factoryClazz.cast(factory);
            }
            throw new IllegalArgumentException("Unknown script name " + scriptSource);
        }

        @Override
        public void close() {}

        @Override
        public Set<ScriptContext<?>> getSupportedContexts() {
            return Set.of(ScoreScript.CONTEXT);
        }
     
    }
}
