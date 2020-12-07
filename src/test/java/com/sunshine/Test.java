package com.sunshine;

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.apache.commons.text.similarity.LevenshteinDetailedDistance;
import org.apache.commons.text.similarity.LevenshteinResults;
import org.apache.commons.text.similarity.LongestCommonSubsequence;

/**
* @author: flg
* @date：2020年8月19日 下午2:09:27
* @Description:
*/
public class Test {
	
	public static String LCS(String s1, String s2) {
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
	
	public static void main(String[] args) {
		
		String fieldValue = "Jan Vosecky";
		String name = "J Vosecky";
		//String name = "Harry Potter";
		//JaroWinkler
    	JaroWinklerSimilarity jw = new JaroWinklerSimilarity();
    	Double jws = jw.apply(fieldValue, name);
    	
    	//编辑距离
    	LevenshteinDetailedDistance ldd = new LevenshteinDetailedDistance();
    	LevenshteinResults lr = null;
    	int max_length = 0;
    	if (fieldValue.length() >= name.length()) {
    		lr = ldd.apply(fieldValue, name);
    		max_length = fieldValue.length();
    	}else {
    		lr = ldd.apply(name, fieldValue);
    		max_length = name.length();
    	}
    	double levenshtein  = lr.getDeleteCount()* 0.1 +lr.getInsertCount()+lr.getSubstituteCount();
        levenshtein = 1 - levenshtein / max_length;
        
        //最长公共子串
    	String lcs = LCS(fieldValue, name);
    	int count = 0;
    	int length = 0;
    	String t1 = fieldValue;
    	String t2 = name;
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
    		lcsd = (length-count+1) * 1.0 /(fieldValue.length()+name.length()+length);
    	
    	//VMN
    	String[] name1 = fieldValue.split("\\s+");
    	String[] name2 = name.split("\\s+");
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
    	
    	System.out.println("VMN:"+vmn);
    	System.out.println(lcsd);
        System.out.println(jws);
        System.out.println(levenshtein);    	
    	System.out.println(jws+levenshtein+lcsd);
	}
}
