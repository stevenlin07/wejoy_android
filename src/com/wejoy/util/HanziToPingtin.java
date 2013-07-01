package com.wejoy.util;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;
 
public class HanziToPingtin {
     //将中文转换为英文
     public static String getEname(String name) throws BadHanyuPinyinOutputFormatCombination {
         HanyuPinyinOutputFormat pyFormat = new HanyuPinyinOutputFormat();
         pyFormat.setCaseType(HanyuPinyinCaseType.LOWERCASE);
         pyFormat.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
         pyFormat.setVCharType(HanyuPinyinVCharType.WITH_V);
 
        return PinyinHelper.toHanyuPinyinString(name, pyFormat, "");
     }
}
