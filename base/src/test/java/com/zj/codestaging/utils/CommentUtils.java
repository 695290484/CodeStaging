package com.zj.codestaging.utils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class CommentUtils {

    public static final String END = "END###";

    public static final Pattern COMMENT_LINE = Pattern.compile("^\\s*#.*$");
    public static final Pattern BLANK_LINE = Pattern.compile("^\\s*$");

    // 带注释的有效行,  使用非贪婪模式匹配有效内容
    public static final Pattern LINE_WITH_COMMENT = Pattern.compile("^(.*?)\\s+#.*$");


    @Data
    @AllArgsConstructor
    public static class Comment {
        private String lineNoComment;
        private String lineWithComment;
        private Integer indexInDuplicates;    // 存在相同行时的索引 (不同key下相同的行, 如 a:\n name: 1  和  b:\n name: 1 )

        private boolean isEndLine() {
            return END.equals(lineNoComment);
        }
    }


    @SneakyThrows
    public static CommentHolder buildCommentHolder(File file) {
        List<Comment> comments = new ArrayList<>();
        Map<String, Integer> duplicatesLineIndex = new HashMap<>();
        CommentHolder holder = new CommentHolder(comments);


        List<String> lines = FileUtils.readLines(file, StandardCharsets.UTF_8);
        // 末尾加个标志, 防止最后的注释丢失
        lines.add(END);

        StringBuilder lastLinesWithComment = new StringBuilder();
        for (String line : lines) {
            if (StringUtils.isBlank(line) || BLANK_LINE.matcher(line).find()) {
                lastLinesWithComment.append(line).append('\n');
                continue;
            }
            // 注释行/空行 都拼接起来
            if (COMMENT_LINE.matcher(line).find()) {
                lastLinesWithComment.append(line).append('\n');
                continue;
            }
            String lineNoComment = line;

            boolean lineWithComment = false;
            // 如果是带注释的行, 也拼接起来, 但是记录非注释的部分
            Matcher matcher = LINE_WITH_COMMENT.matcher(line);
            if (matcher.find()) {
                lineNoComment = matcher.group(1);
                lineWithComment = true;
            }

            // 去除后面的空格
            lineNoComment = lineNoComment.replace("\\s*$", "");
            // 记录下相同行的索引
            Integer idx = duplicatesLineIndex.merge(lineNoComment, 1, Integer::sum);

            // 存在注释内容, 记录
            if (lastLinesWithComment.length() > 0 || lineWithComment) {
                lastLinesWithComment.append(line);
                comments.add(new Comment(lineNoComment, lastLinesWithComment.toString(), idx));
                // 清空注释内容
                lastLinesWithComment = new StringBuilder();
            }
        }

        return holder;
    }


    @AllArgsConstructor
    public static class CommentHolder {
        private List<Comment> comments;

        /**
         * 通过正则表达式移除匹配的行 (防止被移除的行携带注释信息, 导致填充注释时无法正常匹配)
         */
        public void removeLine(String regex) {
            comments.removeIf(comment -> comment.getLineNoComment().matches(regex));
        }

        @SneakyThrows
        public void fillComments(File file) {
            if (comments == null || comments.isEmpty()) {
                return;
            }
            if (file == null || !file.exists()) {
                throw new IllegalArgumentException("file is not exist");
            }

            List<String> lines = FileUtils.readLines(file, StandardCharsets.UTF_8);

            Map<String, Integer> duplicatesLineIndex = new HashMap<>();


            int comIdx = 0;
            StringBuilder res = new StringBuilder();
            for (String line : lines) {

                Integer idx = duplicatesLineIndex.merge(line, 1, Integer::sum);
                Comment comment = getOrDefault(comments, comIdx, null);
                if (comment != null &&
                        Objects.equals(line, comment.lineNoComment)
                        && Objects.equals(comment.indexInDuplicates, idx)) {

                    res.append(comment.lineWithComment).append('\n');
                    comIdx++;
                } else {
                    res.append(line).append('\n');
                }
            }

            Comment last = comments.get(comments.size() - 1);
            if (last.isEndLine()) {
                res.append(last.lineWithComment.substring(0, last.lineWithComment.indexOf(END)));
            }

            FileUtils.write(file, res.toString(), StandardCharsets.UTF_8);
        }
    }

    public static <T> T getOrDefault(List<T> vals, int index, T defaultVal) {
        if (vals == null || vals.isEmpty()) {
            return defaultVal;
        }
        if (index >= vals.size()) {
            return defaultVal;
        }
        T v = vals.get(index);
        return v == null ? defaultVal : v;
    }

}

