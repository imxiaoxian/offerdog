package com.hanserdev.interview.utils;


import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.ast.TextCollectingVisitor;
import com.vladsch.flexmark.util.data.MutableDataSet;

public class MarkdownUtils {

    public static String toPlainText(String markdown) {

        MutableDataSet options = new MutableDataSet();

        Parser parser = Parser.builder(options).build();
        Node document = parser.parse(markdown);

        TextCollectingVisitor textCollector = new TextCollectingVisitor();
        return textCollector.collectAndGetText(document);
    }
}
