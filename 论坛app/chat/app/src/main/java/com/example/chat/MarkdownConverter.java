package com.example.chat;

import android.text.TextUtils;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class MarkdownConverter {

    // 将API原始响应转换为Markdown格式
    public static String toMarkdown(String rawText) {
        if (TextUtils.isEmpty(rawText)) {
            return "";
        }

        // 1. 处理代码块
        String processed = convertCodeBlocks(rawText);

        // 2. 处理标题
        processed = convertHeadings(processed);

        // 3. 处理列表
        processed = convertLists(processed);

        // 4. 处理加粗和斜体
        processed = convertTextStyles(processed);

        // 5. 处理链接
        processed = convertLinks(processed);

        // 6. 处理图片
        processed = convertImages(processed);

        return processed.trim();
    }

    private static String convertCodeBlocks(String text) {
        // 匹配 ```language\ncontent\n``` 格式的代码块
        Pattern pattern = Pattern.compile("```(\\w*)\\n([\\s\\S]*?)\\n```");
        Matcher matcher = pattern.matcher(text);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String language = matcher.group(1);
            String code = matcher.group(2);
            matcher.appendReplacement(sb, "\n```" + language + "\n" + code + "\n```\n");
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    private static String convertHeadings(String text) {
        // 转换 # 标题 为 Markdown 标题
        return text.replaceAll("(?m)^(#{1,6})\\s*(.*?)$", "$1 $2");
    }

    private static String convertLists(String text) {
        // 处理无序列表
        String processed = text.replaceAll("(?m)^\\s*[-*+]\\s+", "* ");

        // 处理有序列表
        processed = processed.replaceAll("(?m)^\\s*(\\d+)\\.\\s+", "$1. ");

        return processed;
    }

    private static String convertTextStyles(String text) {
        // 处理 **加粗**
        String processed = text.replaceAll("\\*\\*(.*?)\\*\\*", "**$1**");

        // 处理 *斜体*
        processed = processed.replaceAll("\\*(.*?)\\*", "*$1*");

        // 处理 _斜体_
        processed = processed.replaceAll("_(.*?)_", "_$1_");

        return processed;
    }

    private static String convertLinks(String text) {
        // 处理 [text](url) 格式链接
        return text.replaceAll("\\[(.*?)\\]\\((.*?)\\)", "[$1]($2)");
    }

    private static String convertImages(String text) {
        // 处理 ![alt](src) 格式图片
        return text.replaceAll("!\\[(.*?)\\]\\((.*?)\\)", "![$1]($2)");
    }
}
