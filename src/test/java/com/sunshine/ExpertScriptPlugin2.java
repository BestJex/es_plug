package com.sunshine;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.apache.commons.text.similarity.LevenshteinDetailedDistance;
import org.apache.commons.text.similarity.LevenshteinResults;
import org.apache.commons.text.similarity.LongestCommonSubsequence;
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
public class ExpertScriptPlugin2 extends Plugin implements ScriptPlugin {

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

            private PureDfLeafFactory(Map<String, Object> params, SearchLookup lookup) {
                if (params.containsKey("name") == false) {
                    throw new IllegalArgumentException("Missing parameter [name]");
                }

                this.params = params;
                this.lookup = lookup;
                name = params.get("name").toString();        
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
                    	
                    	return solution1(fieldValue, name);
                    }
                    
                    //竞标书方案
                    public double solution1(String s1, String s2) {
                    	if(StringUtils.isBlank(s2) || StringUtils.isBlank(s1))
                    		return 0.0;
                    	//JaroWinkler
                    	JaroWinklerSimilarity jw = new JaroWinklerSimilarity();
                    	Double jws = jw.apply(s1, s2);
                    	
                    	//编辑距离
                    	LevenshteinDetailedDistance ldd = new LevenshteinDetailedDistance();
                    	LevenshteinResults lr = null;
                    	int max_length = 0;
                    	if (s1.length() >= s2.length()) {
                    		lr = ldd.apply(s1, s2);
                    		max_length = s1.length();
                    	}else {
                    		lr = ldd.apply(s2, s1);
                    		max_length = s2.length();
                    	}
                    	double levenshtein  = lr.getDeleteCount()* 0.1 +lr.getInsertCount()+lr.getSubstituteCount();
                        levenshtein = 1 - levenshtein / max_length;
                    	//最长公共子串
                    	String lcs = LCS(s1, s2);
                    	int count = 0;
                    	int length = 0;
                    	String t1 = s1;
                    	String t2 = s2;
                    	while(lcs.length() > 0) {
                    		length += lcs.length();
                    		count ++;
                    		t1 = t1.replace(lcs, "");
                    		t2 = t2.replace(lcs, "");
                    		lcs = LCS(t1, t2);
                    	}
                    	double lcsd = 0d;
                    	if(count == 0) {
                    		lcsd = 0;
                    	}else
                    		lcsd = (length-count+1) * 1.0 /(s1.length()+s2.length()+length);
                    	
                    	//VMN
                    	String[] name1 = s1.split("\\s+");
                    	String[] name2 = s2.split("\\s+");
                    	double factor =  1.0 / name1.length;
                    	double vmn = Arrays.asList(name1).stream().map(String::toLowerCase).map(t -> {
                    		double sum = 0.0;
             
                    		for(String s: name2) {
                    			s = s.toLowerCase();
                    			double score = 0.0;
                    			if(t.equals(s)) 
                    				score = 1.0;
                    			else if ((s.length() == 1 && t.startsWith(s)) || (t.length() == 1 && s.startsWith(t))) 
									score = 0.5;
								else 
									score = LCS(t, s).length() * 1.0 / (t.length() + s.length());
								sum += score;
                    		}
                    		return sum * factor;
                    	}).reduce(0.0, (a, b) -> a+b);
                    	
                    	return jws + levenshtein + lcsd + vmn;
                    }
                    
                    //最长公共子串
                    public String LCS(String s1, String s2) {
                    	if(StringUtils.isNotBlank(s2) && StringUtils.isNotBlank(s1)) {
                	    	LongestCommonSubsequence lcs = new LongestCommonSubsequence();
                	    	int[][] arr = lcs.longestCommonSubstringLengthArray(s1, s2);
                	    	int len1 = s1.length();
                			int len2 = s2.length();
                			int max = -1;
                	        int indexE = -1;
                	        
                	        for (int i = 1; i < len1+1; i++) {
                	            for (int j = 1; j < len2+1; j++) {
                	                if(arr[i][j] > max){
                	                    max = arr[i][j];
                	                    indexE = j;
                	                }
                	            }
                	        }
                	        
                	        return s2.substring(indexE-max,indexE);
                		}else
                			return "";
					}
                    
                    
                };
            }
        }
    }
    // end
}
