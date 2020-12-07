package com.sunshine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.index.LeafReaderContext;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.plugins.ScriptPlugin;
import org.elasticsearch.script.ScoreScript;
import org.elasticsearch.script.ScoreScript.LeafFactory;
import org.elasticsearch.script.ScriptContext;
import org.elasticsearch.script.ScriptEngine;
import org.elasticsearch.script.ScriptFactory;
import org.elasticsearch.search.lookup.SearchLookup;

/**
 *文本和图像相似度计算
 */
public class ExpertScriptPlugin extends Plugin implements ScriptPlugin {

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
            if (context.equals(ScoreScript.CONTEXT) == false) {
                throw new IllegalArgumentException(getType() + " scripts cannot be used for context [" + context.name + "]");
            }
            // we use the script "source" as the script identifier
            if ("name_img_similarity".equals(scriptSource)) {
                ScoreScript.Factory factory = new PureDfFactory();
                return context.factoryClazz.cast(factory);
            }
            throw new IllegalArgumentException("Unknown script name " + scriptSource);
        }

        @Override
        public void close() {
            // optionally close resources
        }

        @Override
        public Set<ScriptContext<?>> getSupportedContexts() {
            return Set.of(ScoreScript.CONTEXT);
        }

        private static class PureDfFactory implements ScoreScript.Factory, ScriptFactory {
            @Override
            public boolean isResultDeterministic() {
                // PureDfLeafFactory only uses deterministic APIs, this implies the results are cacheable.
                return true;
            }

            @Override
            public LeafFactory newFactory(Map<String, Object> params, SearchLookup lookup) {
                return new PureDfLeafFactory(params, lookup);
            }
        }

        private static class PureDfLeafFactory implements LeafFactory {
            private final Map<String, Object> params;
            private final SearchLookup lookup;
            private final String name;
            private final String[] sims;

            private PureDfLeafFactory(Map<String, Object> params, SearchLookup lookup) {
                if (params.containsKey("name") == false) {
                    throw new IllegalArgumentException("Missing parameter [name]");
                }

                this.params = params;
                this.lookup = lookup;
                name = params.get("name").toString();  
                sims = params.get("sims").toString().split(",");
                //System.out.println("input name:"+name);
                //System.out.println("sims: "+"".join(",",Arrays.asList(sims)));
            }

            @Override
            public boolean needs_score() {
                return false;  // Return true if the script needs the score
            }

            @Override
            public ScoreScript newInstance(LeafReaderContext context) throws IOException {
            	
                return new ScoreScript(params, lookup, context) {              	                	
                    @Override
                    public double execute(ExplanationHolder explanation) {
                    	
                    	String fieldValue = lookup.source().get("name").toString();
                    	//System.out.println("record name:"+fieldValue);
                    	return solution(fieldValue, name, sims);
                    }
                    
                    /**
                     * 	按照指定方式比较字符串相似度，sims元素的取值包括：JK， LVS，VMN，LCS(最长公共子序列)，LCSS(最长公共子串)
                     * @param recordName
                     * @param searchName
                     * @param sims
                     * @return
                     */
                    public double solution(String recordName, String searchName, String[] sims) {
                    	//验证
                    	if(StringUtils.isBlank(searchName) || StringUtils.isBlank(recordName)) {
                    		//System.err.println("names are blank!!");
                    		return 0.0;
                    	}
                    	List<String> sim = new ArrayList<String>(Arrays.asList("JK","LVS","VMN","LCSS","LCS"));
     
                    	for(String s: sims) {
                    		if (!sim.contains(s)) {
                    			//System.err.println("input sim is not illegal!");
                    			return 0.0;
                    		}
                    	}
                    	
                    	//过滤，保留中文字符，英文和空格
                    	recordName = TextSimilarityTools.filter(recordName);
                    	searchName = TextSimilarityTools.filter(searchName);
                    	if(StringUtils.isBlank(searchName) || StringUtils.isBlank(recordName)) {
                    		//System.err.println("过滤后的名字是blank");
                    		return 0.0;               	                 		
                    	}
                    	//判断中文；如果不都是中文，就把中文转拼音
                    	if (!(TextSimilarityTools.isChineseByReg(recordName.replaceAll("\\s+", "")) 
                    			&& TextSimilarityTools.isChineseByReg(searchName.replaceAll("\\s+", "")))){
                    		if (TextSimilarityTools.isChineseByReg(recordName.replaceAll("\\s+", "")))
                    			recordName = TextSimilarityTools.transPinyin(recordName);
                    		if (TextSimilarityTools.isChineseByReg(searchName.replaceAll("\\s+", "")))
                    			searchName = TextSimilarityTools.transPinyin(searchName);
                    	}
                    	
                    	//计算
                    	double result = 0.0;
                    	if (sims.length == 1) {
                    		String s = sims[0];
                    		switch(s) {
                    		case "JK": result = TextSimilarityTools.JaroWinkler(recordName, searchName); break;
                    		case "LVS": result = TextSimilarityTools.Levenshtein(recordName, searchName); break;
                    		case "VMN": result = TextSimilarityTools.VMN(recordName, searchName); break;
                    		case "LCSS": result = TextSimilarityTools.SelfLCS(recordName, searchName); break;
                    		case "LCS": result = TextSimilarityTools.LCS_Sequence(recordName, searchName); break;
                    	
                    		}                 		
                    	}else {
                    		for(String s: sims) {
                    			double t = 0.0;
                    			switch(s) {
                        		case "JK":  t= TextSimilarityTools.JaroWinkler(recordName, searchName); break;
                        		case "LVS": t = TextSimilarityTools.Levenshtein(recordName, searchName); break;
                        		case "VMN": t = TextSimilarityTools.VMN(recordName, searchName); break;
                        		case "LCSS": t = TextSimilarityTools.SelfLCS(recordName, searchName); break;
                        		case "LCS": t = TextSimilarityTools.LCS_Sequence(recordName, searchName); break;               	
                        		}  
                    			result += t;
                    		}
                    	}
                    	//System.out.println(recordName + ":"+result);
                    	return result;
                    }                    
                    
                };
            }
        }
    }
    // end
}
