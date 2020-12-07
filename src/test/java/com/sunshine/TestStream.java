package com.sunshine;
/**
* @author: flg
* @date：2020年8月20日 下午3:23:43
* @Description:
*/
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
 
public class TestStream 
{
    public static void main(String[] args) 
    {
        List<String> listOfStrings = Arrays.asList("1", "2", "3", "4", "5");
         
        List<Integer> listOfIntegers = listOfStrings.stream()
                                        .map(x -> {return Integer.valueOf(x);}).collect(Collectors.toList());
         
        System.out.println(listOfIntegers);
    }
}
