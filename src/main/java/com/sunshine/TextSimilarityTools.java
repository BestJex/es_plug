package com.sunshine;

import java.util.Arrays;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.apache.commons.text.similarity.LevenshteinDetailedDistance;
import org.apache.commons.text.similarity.LevenshteinResults;
import org.apache.commons.text.similarity.LongestCommonSubsequence;
import org.apache.commons.text.similarity.LongestCommonSubsequenceDistance;

import net.sourceforge.pinyin4j.PinyinHelper;

/**
* @author: flg
* @date：2020年8月25日 上午10:53:36
* @Description:
*/
public class TextSimilarityTools {
	
	private static Pattern ZHPattern = Pattern.compile("[\\u4E00-\\u9FBF]+");
	private static Pattern pattern = Pattern.compile("[^a-zA-Z\\s\\u4E00-\\u9FBF]");
	//JaroWinkler相似度
	public static double JaroWinkler(String s1, String s2) {
		if (!validata(s1, s2))
			return 0.0;
		
		JaroWinklerSimilarity jw = new JaroWinklerSimilarity();
    	return jw.apply(s1, s2);
	}
	//编辑距离
	public static double Levenshtein(String s1, String s2) {
		if (!validata(s1, s2))
			return 0.0;
		
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
        return (1 - levenshtein / max_length);
	}

	//自定义的最长公共子串
	public static double SelfLCS(String s1, String s2) {
		String lcs = LCS_String(s1, s2);
    	int count = 0;
    	int length = 0;
    	String t1 = s1;
    	String t2 = s2;
    	while(lcs.length() > 0) {
    		length += lcs.length();
    		count ++;
    		t1 = t1.replace(lcs, "");
    		t2 = t2.replace(lcs, "");
    		lcs = LCS_String(t1, t2);
    	}
    	double lcsd = 0d;
    	if(count == 0) {
    		lcsd = 0;
    	}else
    		lcsd = (length-count+1) * 1.0 /(s1.length()+s2.length()+length);
    	
    	return lcsd;
	}
	
	//最长公共子序列
	public static double LCS_Sequence(String s1, String s2) {
		if (!validata(s1, s2))
			return 0.0;
		
		LongestCommonSubsequenceDistance lcsd = new LongestCommonSubsequenceDistance();
		return 1.0 * lcsd.apply(s1, s2);
	}
	
	//最长公共子串
    private static String LCS_String(String s1, String s2) {
    	if (!validata(s1, s2))
			return "";
    	
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
	}
    
    //VMN相似度
    public static double VMN(String s1, String s2) {
    	if (!validata(s1, s2))
			return 0.0;
    	
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
					score = LCS_String(t, s).length() * 1.0 / (t.length() + s.length());
				sum += score;
    		}
    		return sum * factor;
    	}).reduce(0.0, (a, b) -> a+b);
    	
    	return vmn;
    }
    
    /**
          * 是否全是汉字<br>
          * 根据汉字编码范围进行判断<br>
     * CJK统一汉字（不包含中文的，。《》（）“‘'”、！￥等符号）<br>
     *
     * @param str
     * @return
     */
    public static boolean isChineseByReg(String str) {
        if (str == null) {
            return false;
        }      
        return ZHPattern.matcher(str).matches();
    }

    private static Boolean validata(String s1, String s2) {
    	if(StringUtils.isBlank(s2) || StringUtils.isBlank(s1))
    		return false;
    	else
    		return true;
    }
    /**
          * 转拼音
     * @param s
     * @return
     */
	public static String transPinyin(String s) {
		String result = null;
		if(StringUtils.isNotBlank(s)) {
			char[] a = s.toCharArray();
			StringBuffer sb = new StringBuffer();
			for(char ch: a) {
				String[] sa = PinyinHelper.toHanyuPinyinStringArray(ch);
				if(sa == null)
					sb.append(' ');
				else
					sb.append(sa[0].substring(0, sa[0].length()-1));
			}
			result = sb.toString();
		}
		return result;
	}
	/**
	 * 过滤除去中文，英文和空格外的所有字符
	 * @param str
	 * @return
	 */
	public static String filter(String str) {
		if (str == null) {
            return "";
        }      
        return pattern.matcher(str).replaceAll("");
	}
	
	public static void main(String[] args) {
		System.out.println(filter("安|家|斯sdf345fg柯达 胜多负少80dfd sf卡的"));
	}
}
