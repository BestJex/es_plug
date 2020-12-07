package com.sunshine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.sunshine.TextSimilarityTools;

/**
* @author: flg
* @date：2020年8月27日 上午9:46:29
* @Description:
*/
public class TestPipei {

	public double solution(String recordName, String searchName, String[] sims) {
    	//验证
    	if(StringUtils.isBlank(searchName) || StringUtils.isBlank(recordName)) {
    		System.err.println("names are blank!!");
    		return 0.0;
    	}
    	List<String> sim = new ArrayList<String>(Arrays.asList("JK","LVS","VMN","LCSS","LCS"));

    	for(String s: sims) {
    		if (!sim.contains(s)) {
    			System.err.println("input sim is not illegal!");
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
    	System.out.println(recordName + ":"+result);
    	return result;
	}
	
	public static void main(String[] args) {
		TestPipei a = new TestPipei();
		String[] sims = "LVS".split(",");
		
		a.solution("陽光SL", "YG", sims);
	}
}
