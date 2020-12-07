package com.sunshine;

import org.apache.commons.text.similarity.CosineDistance;

/**
* @author: flg
* @date：2020年8月19日 下午5:25:41
* @Description:
*/
public class TestCommonstext {
	public static void main(String[] args) {
		CosineDistance r=new CosineDistance();
		System.out.println(r.apply("hamming ", "difference "));
	}
}
