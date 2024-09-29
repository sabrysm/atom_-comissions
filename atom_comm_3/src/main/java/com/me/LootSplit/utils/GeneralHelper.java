package com.me.LootSplit.utils;

import org.apache.commons.lang3.StringUtils;

public class GeneralHelper {
    public static String centerTextWide(String text, int wide)
    {
        int nOfSpaces = wide - text.length();

        int lxNOfSpaces = nOfSpaces / 2;
        int rxNOfSpaces = (nOfSpaces % 2) == 0 ? lxNOfSpaces : lxNOfSpaces + 1;

        String lxSpaces = new String(new char[lxNOfSpaces]).replace('\0', ' ');
        String rxSpaces = new String(new char[rxNOfSpaces]).replace('\0', ' ');

        return lxSpaces + text + rxSpaces;
    }

    public static void main(String[] args) {
        System.out.println("'" + centerTextWide("Hello",  10) + "'");
        System.out.println("'" + centerTextWide("Hello", 10) + "'");
        System.out.println("'" + centerTextWide("Hello", 10) + "'");
        System.out.println("'%s'".formatted(StringUtils.center("Hello", 10)));
    }
}
